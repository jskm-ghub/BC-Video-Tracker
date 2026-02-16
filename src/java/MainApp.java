public class MainApp
{
    public MainApp()
    {
        DriveScanner driveScanner = new DriveScanner();
        DBManager dbManager = new DBManager(driveScanner);
        UIController uiController = new UIController(dbManager, driveScanner);
    }

    public static void main(String[] args)
    {
        new MainApp(); 
    }
}
