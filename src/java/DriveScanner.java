import java.io.File;
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
            String serial = getDriveSerial(root.getAbsolutePath());
            String displayName = root.getAbsolutePath(); // default display name
            Drive drive = new Drive(serial, displayName);
            detectedDrives.add(drive);
        }

        return !detectedDrives.isEmpty();
    }

    /**
     * Get a serial number for a drive (placeholder for now)
     */
    public String getDriveSerial(String drivePath) {
        // TODO: replace with OS-specific serial fetching
        return drivePath.replace("\\", "") + "_SERIAL";
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
