import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Stack;
import java.util.ArrayList;
import javax.swing.filechooser.FileSystemView;




/**
 * The DriveScanner class is responsible for detecting connected storage drives
 * and scanning their directory structures into FileItem representations.
 * It supports Windows, macOS, and Linux by using OS-specific methods
 * to retrieve drive serial numbers.
 */

public class DriveScanner {

    private List<Drive> detectedDrives;

    
    /**
    * Constructs a new DriveScanner and initializes the drive list.
    */
    public DriveScanner() {
        detectedDrives = new ArrayList<>();
    }

    /**
     * Detect all drives connected to the system, and add them to internal list.
     * This checks the OS in order to correctly find all the drives, and then
     * calls the serial fetching for that specific OS.
     * @return true if at least one drive is detected.
     */
    public boolean detectDrives() {
        detectedDrives.clear();
        String osString = System.getProperty("os.name").toLowerCase();
        //Windows runs as before (I think); 

        if (osString.contains("win")) {
            File[] roots = File.listRoots(); // lists root directories (drives) - ONLY WORKS FOR WINDOWS
            //both Mac and Linux are Unix-based and have only one filesystem root, i.e. "/". 
    
            if (roots == null || roots.length == 0) {
                return false; // no drives detected
            }
    
            FileSystemView fsv = FileSystemView.getFileSystemView();

            for (File root : File.listRoots()) {
                // Must be a drive
                if (!fsv.isDrive(root)) continue;

                // Skip the main system drive (usually C:)
                String systemDriveLetter = System.getenv("SystemDrive"); // usually "C:"
                if (systemDriveLetter != null && root.getAbsolutePath().startsWith(systemDriveLetter)) {
                    continue;
                }

                // Check if the drive is removable, which is want we want to show
                if (fsv.isDrive(root) && fsv.getSystemTypeDescription(root) != null &&
                    (fsv.getSystemTypeDescription(root).toLowerCase().contains("removable") ||
                    fsv.getSystemTypeDescription(root).toLowerCase().contains("usb"))) {

                    String serial = getSerialWindows(root);
                    String displayName = fsv.getSystemDisplayName(root);


                    if(displayName != null && displayName.contains(" (")) { 
                        displayName = displayName.substring(0, displayName.indexOf("(")).trim(); 
                    }
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = root.getAbsolutePath();
                    }
                    Drive drive = new Drive(serial, displayName, root);
                    detectedDrives.add(drive);
                }
            }
        } else if (osString.contains("mac")) {
            //This just lists things in /Volumes. This may have issues if there are other things in there (e.g. MobileBackups) other than the main drive and portable drives/USBs etc.
            File[] insideVolumes = (new File("/Volumes")).listFiles();
            //Inside volumes, the "Macintosh HD" or some such directory is actually a link back to "/" which we obviously don't want. The getCanonicalPath() resolves links and is used to screen it out. This structure seems to be consistent for all Mac since OS X?
            ArrayList<File> realVolumes = new ArrayList<>();

            try
            {
                if(insideVolumes != null)
                {
                    for (File volume : insideVolumes)
                    {
                        if (!(volume.getCanonicalPath().equals("/")))
                        {
                            realVolumes.add(volume);
                        }
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace(System.err);
            }
            if (realVolumes.isEmpty()) {
                return false; //Nothing except for the main drive
            }

            for (File realVolume : realVolumes) {
                String serial = getSerialMac(realVolume);
                String displayName = realVolume.getName(); // display name - should have e.g. USB20FD without the /Volumes in the path
                Drive drive = new Drive(serial, displayName, realVolume);
                detectedDrives.add(drive);
            }

        } else if (osString.contains("nix") || osString.contains("nux") || osString.contains("aix")) {
            //For Linux systems (needs to handle different media paths in different distributions)

            ArrayList<File> mediaDevices = new ArrayList<>();
            //Searches system file listing current filesystems/things mounted for anything with "/media/" in it (seems only temporarily mounted things have this)
            try (BufferedReader mountListReader = new BufferedReader(new FileReader("/proc/mounts")) ) {

                String mountsLine;
                while ((mountsLine = mountListReader.readLine()) != null) {
                    if (mountsLine.contains("/media/")) {
                        //lines are formatted as e.g. "/dev/sdd1 /run/media/USERNAME/USB20FD vfat ..." so get the second thing, assuming whitespace separator.
                        String mountPt = mountsLine.split("\\s+")[1];
                        // whitespaces in device name are "\040", these must be resolved back to whitespaces before making a File out of them
                        mountPt = mountPt.replace("\\040", " ");
                        
                        File mediaDevice = new File(mountPt);
                        if (mediaDevice.exists()) { //don't add nonexistent thing (shouldn't happen)
                            mediaDevices.add(mediaDevice);
                        }
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace(System.err);
            }
            //outside the catch so that still checking list (though probably not strictly necessary)
            if (mediaDevices.isEmpty()) {
                return false; //Nothing found in file
            }
            for (File mediaDevice : mediaDevices) {
                String serial = getSerialLinux(mediaDevice);
                String displayName = mediaDevice.getName(); // display name - should return e.g. USB20FD (without /run/media etc.).
                Drive drive = new Drive(serial, displayName, mediaDevice);
                detectedDrives.add(drive);
            }

        } else
        {
            //If none of the OS's above matched it, then who knows how to do it now.
            UIController.displayDataMessage("Unknown operating system detected (not supported)");
            return false;
        } 

        return !detectedDrives.isEmpty();
    }

    /**
    * Retrieves the serial number of a drive on Windows systems
    * using PowerShell commands.
    *
    * @param drive the root drive to query
    * @return the drive serial number
    */
    // WINDOWS
    private String getSerialWindows(File drive)
{
    try
    {
        String driveLetter = drive.getAbsolutePath().substring(0, 1);

        String psScript =
            "$dl = '" + driveLetter + "';" +
            "$part = Get-Partition -DriveLetter $dl;" +
            "if ($part -eq $null) { 'NO_DISK' } else {" +
                "$disk = Get-Disk -Number $part.DiskNumber;" +
                "$pnp = (Get-CimInstance Win32_DiskDrive | " +
                "Where-Object { $_.Index -eq $disk.Number }).PNPDeviceID;" +
                "if ($pnp) {" +
                    "$pnp.Split('\\')[-1]" +  
                "} else { 'NO_SERIAL' }" +
            "}";

        ProcessBuilder builder = new ProcessBuilder("powershell", "-Command", psScript);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );

        String output = reader.readLine();
        process.waitFor();

        if (output != null) {
            output = output.trim();
            // Method works and gets a correct serial number, however, Windows PNPDeviceID adds a tail segemnt, this if statement removes it.
            if (output.contains("&")) {
                output = output.substring(0, output.indexOf("&"));
        }
            if (!output.isEmpty() && !output.equalsIgnoreCase("NO_DISK") && !output.equalsIgnoreCase("NO_SERIAL")) {
                return output;
            }
        }

    } catch (Exception ex) {
        ex.printStackTrace();
    }

    return "UNKNOWN_WIN";
}

    // LINUX
    /**
     * Works by using "df" to get the device path for the mount point.
     * Then Use "udevadm info" to read hardware properties.
     * Then Extract ID_SERIAL=xxxx from output.
     */
    private String getSerialLinux(File drive)
    {
        try
        {
            // find the actual device for the mount point
            Process mounts = new ProcessBuilder("df", drive.getAbsolutePath()).start();

            BufferedReader mountReader = new BufferedReader(new InputStreamReader(mounts.getInputStream()));

            mountReader.readLine(); // skip header
            String line = mountReader.readLine();
            if (line == null) return "UNKNOWN_LINUX";

            String device = line.split("\\s+")[0];

            // query udev for serial
            Process process = new ProcessBuilder(
                "udevadm", "info", "--query=all", "--name=" + device
            ).start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );

            String l;
            while ((l = reader.readLine()) != null) {
                //if (l.contains("ID_SERIAL=")) {   // finds the serial property
                if (l.contains("ID_SERIAL_SHORT=")) {   // finds the serial property - appears necessary, to get only the number matching the Mac version; otherwise get e.g. PNY_USB_2.0_FD_0719... etc instead of just the "0719..." part.
                    return l.split("=")[1].trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "UNKNOWN_LINUX";
    }

    // MAC
    /**
     *  Uses "df" to determine the physical disk (ex: disk2s1 → disk2).
     *  Query IORegistry: ioreg -r -c IOBlockStorageDevice
     *  Filter output with grep + sed to extract the serial number.
     */
    private String getSerialMac(File drive)
    {
        try
        {
            //find disk device from mount point
            Process mountProc = new ProcessBuilder("df", drive.getAbsolutePath()).start(); //seems to work: e.g. df /Volumes/USB20FD

            BufferedReader mread = new BufferedReader(new InputStreamReader(mountProc.getInputStream()));
            mread.readLine(); // skip header
            String line = mread.readLine();
            if (line == null) return "UNKNOWN_MAC";

            String device = line.split("\\s+")[0]; // ex: /dev/disk2s1

            // Remove partition suffix (disk2s1 → disk2)
            String disk = device.replaceAll("s\\d+$", "").replace("/dev/", "");

            //The system_profiler SPUSBDataType command worked when tested on older Mac. The "Serial Number: ..." shows up after the full name (not mounted name) and BEFORE the "BSD Name" (e.g. disk2) that extracted, so keep track of the lines with "Serial Number" until finding the line that has the "BSD Name" since last one before that will be right. (Could maybe use mark and reset?)
            Process serialProc = new ProcessBuilder("system_profiler", "SPUSBDataType").start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(serialProc.getInputStream()));

            String serialLine = null;
            String currentLine;
            while ((currentLine = reader.readLine()) != null)
            {
                if (currentLine.contains("Serial Number"))
                {
                    serialLine = currentLine;
                } else if (currentLine.contains(disk))
                {
                    break;//The last thing assigned to serialLine should be right
                }
            }
            if (serialLine != null && !serialLine.trim().isEmpty()) {
                return serialLine.trim().replaceFirst("Serial Number: ", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "UNKNOWN_MAC";
    }

    /**
     * Explicityly name checks for System Volume Information files to skip (not the most ideal, however, due to nature of the app and code is the msot reliable solution for now.)
     * Also skipps hidden and unreadable files, since these are not accessible to the user and would just cause errors if tried to be scanned.
     * @param file the file to check
     * @return true if the file should be skipped, false otherwise
     */

    private boolean shouldSkip(File file) {
        if (file == null) return true;

        String name = file.getName();

        // Explicit Windows system folder
        if (name.equalsIgnoreCase("System Volume Information")) {
            return true;
        }
        // Skip hidden files
        if (file.isHidden()) {
            return true;
        }
        // Skip unreadable files
        if (!file.canRead()) {
            return true;
        }

        return false;
        }
    /**
    * Scans the given drive and returns a list of FileItem objects representing
    * all files and directories found on the drive.
    *
    * @param drive the drive to scan
    * @return a list of FileItem objects found on the drive
    */
    public List<FileItem> scan(Drive drive) {
        List<FileItem> items = new ArrayList<>();
        Stack<File> fileStack = new Stack<>();
        Stack<Integer> parentStack = new Stack<>();
        int fileIDCounter = 1; // unique file ID counter


        //File root = new File(drive.getDisplayName()); //BAD! Means that display name has to be the full path!!!
        File root = drive.getDriveRootFolder(); //safe b/c File is immutable

        // Theoretically, root could be null. But scan() is only called in this way: UIController -> DriveScanner.getDetectedDrives called to get list; then -> DBManager.insertDrive() called with each of these drives, which in turn -> DriveScanner.scan() called with drive. So it shouldn't ever be null.
        if (root.exists() && root.isDirectory()) {
            File[] children = root.listFiles(file -> ! shouldSkip(file));
            if (children != null) {
                for (File child : children) {
                    fileStack.push(child);
                    parentStack.push(-1); // root has no parent
                }
            }
        }

        while (!fileStack.isEmpty()) {
            File current = fileStack.pop();
            int parentId = parentStack.pop();

            boolean isFolder = current.isDirectory();
            String rootPath = drive.getDriveRootFolder().getAbsolutePath();
            String fullPath = current.getAbsolutePath();
            String name = current.getName(); 
            String relativePath;
            if (fullPath.equals(rootPath)) {
                relativePath = ""; // root
            } else {
                relativePath = fullPath.substring(rootPath.length());
            }
            relativePath = relativePath.replaceFirst("^[/\\\\]", ""); // remove leading slash

            FileItem item = new FileItem(
                    fileIDCounter++,
                    name,
                    relativePath,
                    isFolder,
                    drive.getDriveID(),
                    0L,
                    parentId
            );
            items.add(item);

            if (isFolder) {
                File[] children = current.listFiles(file -> ! file.isHidden()); // this will ignore hidden files
                if (children != null) {
                    for (File child : children) {
                        fileStack.push(child);
                        parentStack.push(item.getFileID());
                    }
                }
            }

        }

        return items;
    }

    /**
     * Get the list of drives detected.
     * 
     * @return a list of detected Drive objects
     */
    public List<Drive> getDetectedDrives() {
        return detectedDrives;
    }

}

