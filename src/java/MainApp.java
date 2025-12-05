public class MainApp {

    private DBManager dbManager;
    private UIController uiController;
    private DriveScanner driveScanner;

    public MainApp() {              
        dbManager = new DBManager(); //Initializes database manager and connect to DB
         try {
            dbManager.connect();
        } catch (Exception e) {
            System.out.println("Database connection failed: " + e.getMessage());
            return;
        }

        driveScanner = new DriveScanner();  //Initializes drive scanner

        driveScanner.detectDrives();  // Detect drives (this updates internal drive list)

        uiController = new UIController(dbManager, driveScanner); //Initializes UI controller with DB manager and drive scanner
    }

    public static void main(String[] args) {
        new MainApp(); 
    }
}
