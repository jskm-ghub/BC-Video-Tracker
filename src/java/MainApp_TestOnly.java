import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class MainApp_TestOnly {

    //we don't really need a constructor for testing stuff; each test method can initialize what it needs to
    private MainApp_TestOnly() { }

    /** Call whatever test methods are desired from this method (i.e. edit
     * this as desired to call a different method).
     * Generally, the test methods are intended to be called one at a time
     * (not sequentially)
     */
    public static void main(String[] args) {
        testDriveScanner();
    }

    // this was in drive scanner
    /** Tests the drive detection and scanning methods of DriveScanner, without connecting to any DB. This should work anywhere, anytime. */
    public static void testDriveScanner() {
        DriveScanner scanner = new DriveScanner();

        System.out.println("Detecting drives...");
        boolean found = scanner.detectDrives();

        if (!found) {
            System.out.println("No drives detected.");
            return;
        }

        List<Drive> driveList = scanner.getDetectedDrives();
        System.out.println("Number of drives detected: " + driveList.size());

        //for (Drive drive : scanner.getDetectedDrives()) {
        for (Drive drive : driveList) {
            System.out.println("\n===== DRIVE =====");
            System.out.println("Display Name: " + drive.getDisplayName());
            System.out.println("Serial: " + drive.getSerialName());

            System.out.println("Scanning files...");
            List<FileItem> items = scanner.scan(drive);

            System.out.println("Files found: " + items.size());

            // Print first 10 items using toString()
            int numItems = Math.min(10, items.size());
            for (int i = 0; i < numItems; i++) {
                System.out.println("  " + items.get(i).toString());
            }
        }
    }

    /** Tests the ability to connect to the SQL DB (including decrypting information) and to close the connections.
     * Creates a DriveScanner in order to create a DBManager, but does not use it.
     */
    public static void testDBConnection() {
        //opens connection (Initializes database manager and connects to DB)
        DBManager dbm = new DBManager(new DriveScanner());
        //closes connection
        dbm.closeConnection();
    }

    /** Assuming DB can connect, tests ability to select all the drives in 
     * the DB and return them.
     */
    public static void testDBSelectDrives() {
        DBManager dbm = new DBManager(new DriveScanner());
        Map<Integer, Drive> drives = dbm.getDrives();
        System.out.println("Drives found in the DB:");
        for (Drive drive : drives.values()) {
            System.out.println(drive);
        }
        dbm.closeConnection();
    }

    /** Assuming DB can connect and there is at least 1 drive in the DB, 
     * tests the getFiles(Drive d) and (if at least one of these is a folder)
     * the getFiles(Drive d, FileItem f) methods. */
    public static void testDBSelDriveAndSomeFiles() {
        DBManager dbm = new DBManager(new DriveScanner());
        Collection<Drive> drives = dbm.getDrives().values();
        if (drives.isEmpty()) {
            System.out.println("No drives found in the DB.");
            return;
        }

        Drive driveToTest = null;
        System.out.println("Drives found in the DB:");
        for (Drive drive : drives) {
            System.out.println(drive);
            //grabbing a drive to use like this due to limitations in Collection interface
            driveToTest = drive;
        }

        System.out.println("Testing DBManager.getFiles(Drive d) with the drive: " + driveToTest);
        List<FileItem> rootFiles = dbm.getFiles(driveToTest);
        if (rootFiles.isEmpty()) {
            System.out.println("The drive being tested has no files associated with it in the DB");
            return;
        }
        System.out.println("Files/directories in the \"root\" directory of the drive:");
        FileItem trySubDirectory = null; //rootFiles.getFirst();
        for (FileItem fileItem : rootFiles) {
            //try to find something that is a folder
            if (fileItem.isFolder()) {
                trySubDirectory = fileItem;
            }
            System.out.println(fileItem);
        }
        
        if (trySubDirectory == null) {
            System.out.println("None of the files in the \"root\" directory of the drive are folders, so can't try the second getFiles method.");
            return;
        }

        System.out.println("Testing DBManager.getFiles(Drive d, FileItem f) with the FileItem: " + trySubDirectory);
        rootFiles = dbm.getFiles(driveToTest, trySubDirectory);
        if (rootFiles.isEmpty()) {
            System.out.println("The directory being tested has no files associated with it in the DB");
            return;
        }
        System.out.println("Files/directories in the \"" + trySubDirectory.getName() + "\" directory of the drive:");
        for (FileItem fileItem : rootFiles) {
            //try to find something that is a folder
            System.out.println(fileItem);
        }
        dbm.closeConnection();
    }

    /*There doesn't seem to be a good way to test inserting or deleting an arbitrary Drive object (or files) into the DB: The insertDrive(Drive drive) method also calls DriveScanner.scan(Drive drive), which relies on the drive having been created with a File object attribute by DriveScanner. The method to insert files is private.
    DELETING an arbitrary drive could be done relatively easily, but this would have permanent effects on the DB, so seems undesirable.
    */

    /** Tests the whole drive insertion process (detecting what drives, if any,
     * are plugged in; and adding the selected drive and its files to DB)
     * without using the UI.
     * This of course relies on the DB connection (tested in the 
     * <code>testDBConnection()</code> method of this class) and on the methods
     * tested in the <code>testDriveScanner()</code> method of this class.
     */
    public static void testDetectAndInsertDrive() {
        DriveScanner scanner = new DriveScanner();
        DBManager dbm = new DBManager(scanner);

        System.out.println("Detecting drives...");
        
        if (!scanner.detectDrives()) {
            System.out.println("No drives detected.");
            return;
        }

        List<Drive> driveList = scanner.getDetectedDrives();
        System.out.println("Number of drives detected: " + driveList.size());

        //for (Drive drive : scanner.getDetectedDrives()) {
        for (Drive drive : driveList) {
            System.out.println("\n===== DRIVE =====");
            System.out.println("Display Name: " + drive.getDisplayName());
            System.out.println("Serial: " + drive.getSerialName());

            //don't need to test the DriveScanner.scan() method separately here b/c the DBManager.insertDrive(Drive drive) method calls it...
            System.out.println("Scanning files...");
            List<FileItem> items = scanner.scan(drive);

            System.out.println("Files found: " + items.size());

            // Print first 10 items using toString()
            for (int i = 0; i < Math.min(10, items.size()); i++) {
                System.out.println("  " + items.get(i).toString());
            }
        }
        //allow to choose drive to scan and add to the DB
        BufferedReader keyboardIn = new BufferedReader(new InputStreamReader(System.in));
        String chosenDriveName = null;
        Drive chosenDrive = null;
        inputLoop: while(true) {
            System.out.print("Enter the display name of one of the drives as shown above to try scanning its contents and adding them to the DB: ");
            try {
                chosenDriveName = keyboardIn.readLine();
            } catch (IOException ioe) {
                System.err.println("Reading input line failed for some stupid reason. Exiting method.");
                return; //thread will "finish" and exit
            }
            chosenDriveName = chosenDriveName.trim();
            for (Drive drive : driveList) {
                if (drive.getDisplayName().equals(chosenDriveName)) {
                    chosenDrive = drive;
                    break inputLoop;
                }
            }
            System.out.println("The drive display name given did not match any possible drive; try again.");
        }
        //only get out of above loop if found the drive. Now scan and add.
        System.out.println("Attempting to scan drive and add contents to the DB.");
        dbm.insertDrive(chosenDrive);

        dbm.closeConnection();
    }
}

