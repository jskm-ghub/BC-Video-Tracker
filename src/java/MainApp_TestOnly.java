import java.util.*;

public class MainApp_TestOnly {

    private DBManager dbManager;
    private UIController uiController;
    private DriveScanner driveScanner;

    public MainApp_TestOnly() {              
        //Don't need this for testing...
        //dbManager = new DBManager(); //Initializes database manager and connect to DB
        //dbManager.connect();

        driveScanner = new DriveScanner();  //Initializes drive scanner

        //For testing of various methods, no UI (temporary!):
        //uiController = new UIController(dbManager, driveScanner); //Initializes UI controller with DB manager and drive scanner

        if (driveScanner.detectDrives()) {
            List<Drive> driveList = driveScanner.getDetectedDrives();
            System.out.println("size of driveList: " + driveList.size());
            for (Drive drive : driveList) {
                //print drive serial and display name to test the getDriveSerial function and the display name...
                System.out.println("Serial name: " + drive.getSerialName() + "; and display name: " + drive.getDisplayName());
            }
        } else {
            System.out.println("No drives connected");
        }
    }

    public static void main(String[] args) {
        new MainApp_TestOnly(); 
    }

    // this was in drive scanner
    /*
    * public static void main(String[] args) {
    DriveScanner scanner = new DriveScanner();

    System.out.println("Detecting drives...");
    boolean found = scanner.detectDrives();

    if (!found) {
        System.out.println("No drives detected.");
        return;
    }

    System.out.println("Drives detected: " + scanner.getDetectedDrives().size());

    for (Drive drive : scanner.getDetectedDrives()) {
        System.out.println("\n===== DRIVE =====");
        System.out.println("Display Name: " + drive.getDisplayName());
        System.out.println("Serial: " + drive.getSerialName());

        System.out.println("Scanning files...");
        List<FileItem> items = scanner.scan(drive);

        System.out.println("Files found: " + items.size());

        // Print first 10 items using toString()
        for (int i = 0; i < Math.min(10, items.size()); i++) {
            System.out.println("  " + items.get(i).toString());
        }
    }
}*/
}

