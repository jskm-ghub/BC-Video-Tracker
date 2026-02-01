import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Stack;
import java.util.ArrayList;

public class DriveScanner {

    private List<Drive> detectedDrives;
    private int fileIDCounter = 1; // unique file ID counter

    public DriveScanner() {
        detectedDrives = new ArrayList<>();
    }

    /* //old version
    public boolean detectDrives() {
        detectedDrives.clear();
        File[] roots = File.listRoots(); // lists root directories (drives)

        if (roots == null || roots.length == 0) {
            return false; // no drives detected
        }

        for (File root : roots) {
            String serial = getDriveSerial(root);
            String displayName = root.getAbsolutePath(); // default display name
            Drive drive = new Drive(serial, displayName);
            detectedDrives.add(drive);
        }

        return !detectedDrives.isEmpty();
    } */
   /**
     * Detect all drives connected to the system, and add them to internal list.
     * This has to check the OS in order to correctly find all the drives.
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
    
            for (File root : roots) {
                String serial = getDriveSerial(root);
                String displayName = root.getAbsolutePath(); // default display name
                Drive drive = new Drive(serial, displayName);
                detectedDrives.add(drive);
            }
        } else if (osString.contains("mac")) {
            //This just lists things in /Volumes. This may have issues if there are other things in there (e.g. MobileBackups) other than the main drive and portable drives/USBs etc.
            File[] insideVolumes = (new File("/Volumes")).listFiles();
            //Inside volumes, the "Macintosh HD" or some such directory is actually a link back to "/" which we obviously don't want. The getCanonicalPath() resolves links and is used to screen it out.
            ArrayList<File> realVolumes = new ArrayList<>();

            try { //I hate this!
                for (File volume : insideVolumes) {
                    if (!(volume.getCanonicalPath().equals("/"))) {
                        realVolumes.add(volume);
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
                String displayName = realVolume.getAbsolutePath(); // default display name - I don't know about this, but might as well try
                Drive drive = new Drive(serial, displayName);
                detectedDrives.add(drive);
            }

        } else if (osString.contains("nix") || osString.contains("nux") || osString.contains("aix")) {
            //Tested briefly; put together quickly based on the Mac and what I know about Linux

            //We need USERNAME to get the right directory to start with...
            String userName = System.getProperty("user.name");
            
            //This just tries getting things in /run/media/USERNAME. This may have issues if there are other things in there (e.g. CD drives) other than portable drives/USBs etc, or if this isn't the directory to use.
            File runMediaUName = (new File("/run/media/" + userName));
            //There may not be any such directory (if nothing plugged in/has been?), so check if the folder actually exists

            if (runMediaUName.exists()) {
                //Shouldn't be any looping directories in here, unlike Mac!
                File[] mediaDevices = runMediaUName.listFiles();

                if ((mediaDevices == null) || (mediaDevices.length ==0)) {
                    return false; //Nothing in run/media/USERNAME
                }
    
                for (File mediaDevice : mediaDevices) {
                    String serial = getSerialLinux(mediaDevice);
                    String displayName = mediaDevice.getAbsolutePath(); // default display name - I don't know about this, but might as well try - returns e.g. /Volumes/USB20FD. Could remove that if desired.
                    Drive drive = new Drive(serial, displayName);
                    detectedDrives.add(drive);
                }
            } else {//directory doesn't even exist, so can't be anything?
                return false;
            }
        } else { //If none of the OS's above matched it, then who knows how to do it now. 
            System.err.println("Unknown operating system detected (not supported)");
            return false;
        } 


        return !detectedDrives.isEmpty();
    }

    /**
     * Get a serial number for a drive (Checks the OS first, then calls the serial fetching for that specific OS)
     */
    public String getDriveSerial(File drive) {
    String os = System.getProperty("os.name").toLowerCase();

    if (os.contains("win")) {
        return getSerialWindows(drive);
    //Mac and Linux checks below are now probably redundant, since detectDrives() now checks that
    } else if (os.contains("mac")) {
        return getSerialMac(drive);
    } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
        return getSerialLinux(drive);
    }

    return "UNKNOWN_OS_AND_SERIAL"; //In case of unsopported OS
}

    // WINDOWS
    private String getSerialWindows(File drive) {
        try {
        // Extracts drive letter (ex: "C")
        String driveLetter = drive.getAbsolutePath().substring(0, 1);

        // Powershell script to get the disk serial no.
        String psScript =
            "$dl = '" + driveLetter + "';" +
            "$part = Get-Partition -DriveLetter $dl;" +
            "if ($part -eq $null) { 'NO_DISK_NUMBER' } else {" +
                "$diskNum = $part.DiskNumber;" +
                "Get-PhysicalDisk | Where-Object { $_.DeviceId -eq $diskNum } | " +
                "Select -ExpandProperty SerialNumber" +
            "}";

        // Prepares the PowerShell process
        ProcessBuilder builder = new ProcessBuilder(
            "powershell",
            "-Command",
            psScript
        );

        // Combines the error and out message so both come through as just one text line
        builder.redirectErrorStream(true);

        Process process = builder.start();

        BufferedReader reader =
            new BufferedReader(new InputStreamReader(process.getInputStream()));

        String output = reader.readLine();
        process.waitFor();    // this is for Java to wait until Powershell is completely done.

        if (output != null) {
            output = output.trim();
            if (!output.isEmpty() && !output.equalsIgnoreCase("NO_DISK_NUMBER")) {
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
    private String getSerialLinux(File drive) {
    try {
        // find the actual device for the mount point
        Process mounts = new ProcessBuilder("df", drive.getAbsolutePath()).start();

        BufferedReader mountReader = new BufferedReader(
            new InputStreamReader(mounts.getInputStream())
        );

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
   private String getSerialMac(File drive) {
    try {
        //find disk device from mount point
        Process mountProc = new ProcessBuilder("df", drive.getAbsolutePath()).start(); //seems to work: e.g. df /Volumes/USB20FD

        BufferedReader mread = new BufferedReader(
            new InputStreamReader(mountProc.getInputStream())
        );

        //Test prints - this part works...
        //System.out.println("Reading first and second lines from process: " + mread.readLine());
        mread.readLine(); // skip header
        String line = mread.readLine();
        //System.out.println(line);
        if (line == null) return "UNKNOWN_MAC";

        String device = line.split("\\s+")[0]; // ex: /dev/disk2s1
        //System.out.println(device); //works

        // Remove partition suffix (disk2s1 → disk2)
        String disk = device.replaceAll("s\\d+$", "").replace("/dev/", "");
        //System.out.println("disk: " + disk);

        /* // Query IORegistry for serial number
        //This doesn't seem to work. Doing without grep gets hundreds of nasty lines. And the lines don't seem to have any "Serial Number" or even disk name (e.g. disk2), or even the file display name (e.g. the "USB20FD" example is shown as "USB 2.0 FD@...").
        //Also the -A20 prints 20 "lines of trailing context" after the right line, which seems useless anyway. ALSO I don't know why sh -c is necessary at all.
        Process serProc = new ProcessBuilder(
            "sh", "-c",
            "ioreg -r -c IOBlockStorageDevice | grep -A20 '" + disk + "' | grep 'Serial Number' | sed 's/.*= //'"
        ).start(); */

        //The system_profiler SPUSBDataType command worked when tested on older Mac. The "Serial Number: ..." shows up after the full name (not mounted name) and BEFORE the "BSD Name" (e.g. disk2) that extracted, so keep track of the lines with "Serial Number" until finding the line that has the "BSD Name" since last one before that will be right. (Could maybe use mark and reset?)
        Process serialProc = new ProcessBuilder(
            "system_profiler", "SPUSBDataType"
        ).start();

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(serialProc.getInputStream())
        );

        //Old:
        /* String serial = reader.readLine();
        if (serial != null && !serial.trim().isEmpty()) {
            return serial.trim().replaceAll("[\" ]", "");
        } */
        //New:
        String serialLine = null;
        String currentLine = null;
        while ( ((currentLine = reader.readLine()) != null)) {
            if (currentLine.contains("Serial Number")) {
                serialLine = currentLine;
            } else if (currentLine.contains(disk)) {
                break;//The last thing assigned to serialLine should be right
            }
        }
        if (serialLine != null && !serialLine.trim().isEmpty()) {
            //System.out.println("Test output serial: " + serialLine.trim().replaceFirst("Serial Number: ", ""));
            return serialLine.trim().replaceFirst("Serial Number: ", "");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }

    return "UNKNOWN_MAC";
}

    /**
     * Scan a drive and return all files/folders as FileItem objects.
     */
    public List<FileItem> scan(Drive drive) {
        List<FileItem> items = new ArrayList<>();
        Stack<File> fileStack = new Stack<>();
        Stack<Integer> parentStack = new Stack<>();

        File root = new File(drive.getDisplayName());
        if (root.exists() && root.isDirectory()) {
            fileStack.push(root);
            parentStack.push(-1); // root has no parent
        }

        while (!fileStack.isEmpty()) {
            File current = fileStack.pop();
            int parentId = parentStack.pop();

            boolean isFolder = current.isDirectory();

            FileItem item = new FileItem(
                    fileIDCounter++,
                    current.getName(),
                    current.getAbsolutePath(),
                    isFolder,
                    drive.getSerialName().hashCode(),
                    parentId
            );
            items.add(item);

            if (isFolder) {
                File[] children = current.listFiles();
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
     */
    public List<Drive> getDetectedDrives() {
        return detectedDrives;
    }
}
