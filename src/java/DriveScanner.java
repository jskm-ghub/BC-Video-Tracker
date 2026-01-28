import java.io.BufferedReader;
import java.io.File;
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

    /**
     * Detect all drives connected to the system.
     * @return true if at least one drive is detected.
     */
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
    }

    /**
     * Get a serial number for a drive (Checks the OS first, then calls the serial fetching for that specific OS)
     */
    public String getDriveSerial(File drive) {
    String os = System.getProperty("os.name").toLowerCase();

    if (os.contains("win")) {
        return getSerialWindows(drive);
    } else if (os.contains("mac")) {
        return getSerialMac(drive);
    } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
        return getSerialLinux(drive);
    }

    return "UNKNOWN"; //In case of unsopported OS
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
            if (l.contains("ID_SERIAL=")) {   // finds the serial property
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
        Process mountProc = new ProcessBuilder("df", drive.getAbsolutePath()).start();

        BufferedReader mread = new BufferedReader(
            new InputStreamReader(mountProc.getInputStream())
        );

        mread.readLine(); // skip header
        String line = mread.readLine();
        if (line == null) return "UNKNOWN_MAC";

        String device = line.split("\\s+")[0]; // ex: /dev/disk2s1

        // Remove partition suffix (disk2s1 → disk2)
        String disk = device.replaceAll("s\\d+$", "").replace("/dev/", "");

        // Query IORegistry for serial number
        Process serProc = new ProcessBuilder(
            "sh", "-c",
            "ioreg -r -c IOBlockStorageDevice | grep -A20 '" + disk + "' | grep 'Serial Number' | sed 's/.*= //'"
        ).start();

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(serProc.getInputStream())
        );

        String serial = reader.readLine();
        if (serial != null && !serial.trim().isEmpty()) {
            return serial.trim().replaceAll("[\" ]", "");
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
