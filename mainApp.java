public class MainApp {

    private DBManager dbManager;
    private UIController uiController;
    private DriveScanner driveScanner;

    public MainApp() {              
        dbManager = new DBManager(); //Initializes database manager and connect to DB
        dbManager.connect(); 

        driveScanner = new DriveScanner();  //Initializes drive scanner

        uiController = new UIController(dbManager, driveScanner); //Initializes UI controller with DB manager and drive scanner
    }

    public static void main(String[] args) {
        new MainApp(); 
    }
}

