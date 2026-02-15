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
}

