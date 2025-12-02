// For the Display Window
import javax.swing.JPanel;
import javax.sound.midi.SysexMessage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.JOptionPane;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

// For Graphics
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Image;
import java.awt.Toolkit;

// For Mouse and Keyboard Input
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Point;

// Utilities
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class UIController extends JPanel implements ActionListener, MouseListener, KeyListener
{
    /*  ~~~~~ WINDOW MANAGEMENT ~~~~~ */
    private JFrame window;
    private Timer clock;
    // constant pixel values
    private final int spacing = 12; // spacing between buttons
    private final int buttonHeight = 35; // height (and width) of buttons
    private final int driveHeight = 25; // height of drives shown in drive area
    private final int fileHeight = 20; // height of files shown in file area
    private final int fileSpacing = 7; // spacing between files and drives in their respective areas
    // Colors
    private final Color topPanelColor = new Color(255, 238, 176);
    private final Color mainPanelColor = new Color(255, 246, 212);
    private final Color fileClickedColor = new Color(206, 231, 240);
    private final Color fieldBackgroundColor = new Color(242, 242, 242);
    // flags
    private boolean clickingBack, clickingSearch, clickingRefresh, clickingUpdate, clickingDrive, clickingFile;
    private boolean screenUpdated;
    private boolean searching;
    // position markers
    private int clickRelativePositionInDriveList, clickRelativePositionInFileList;

    /* ~~~~~ APPLICATION SPECIFIC OBJECTS ~~~~~ */
    private DBManager database;
    private DriveScanner driveScanner;

    /* ~~~~~ FILE NAVIGATION ~~~~~ */
    private Stack<FileItem> path;
    private Drive currentDrive;
    private List<Drive> listDrives;
    private List<FileItem> listFiles;

    /* ~~~~~ BUTTONS ~~~~~ */
    // button positions and sizes
    private Rectangle backButton, pathBar, searchBar, refreshButton, updateButton, fileArea, driveArea;
    // text values
    private String searchBarText;
    private String pathText;
    // button images
    private final Image backButtonImage = Toolkit.getDefaultToolkit().getImage("back_button.png");
    private final Image backButtonClickedImage = Toolkit.getDefaultToolkit().getImage("back_button_clicked.png");
    private final Image refreshButtonImage = Toolkit.getDefaultToolkit().getImage("refresh_button.png");
    private final Image refreshButtonClickedImage = Toolkit.getDefaultToolkit().getImage("refresh_button_clicked.png");
    private final Image updateButtonImage = Toolkit.getDefaultToolkit().getImage("update_button.png");
    private final Image updateButtonClickedImage = Toolkit.getDefaultToolkit().getImage("update_button_clicked.png");

    public UIController(DBManager dbm, DriveScanner ds)
    {
        this.database = dbm;
        this.driveScanner = ds;

        // creates window
        window = new JFrame();
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                // TODO: anything else need to be done on shutdown?
                database.closeConnection();
                window.dispose();
                System.exit(0);
            }
        }
        );
        window.setVisible(true);
        window.setSize(842,500);
        window.add(this);
        this.setBackground(mainPanelColor);

        // listens to mouse and keyboard
        window.addKeyListener(this);
        window.addMouseListener(this);

        // create window sections - buttons, fields, file and drive areas
        backButton = new Rectangle();
        pathBar = new Rectangle();
        searchBar = new Rectangle();
        refreshButton = new Rectangle();
        updateButton = new Rectangle();
        fileArea = new Rectangle();
        driveArea = new Rectangle();
        resizeWindow();

        // initialize specific variables
        listDrives = dbm.getDrives();
        path = new Stack<FileItem>();
        resetToHome();

        // checks where we are clicking
        resetClick();

        // constant update to window
        clock = new Timer(10,this);
        clock.start();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // TODO: maybe not repaint every time?
        window.repaint();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        // TODO: maybe we don't have to resize every single time?
        super.paintComponent(g);
        render(g);
        resizeWindow();
    }

    private void render(Graphics g)
    {
        int fontSize = 16;
        g.setFont(new Font("Arial", Font.PLAIN, fontSize));

        // draw top panel
        g.setColor(topPanelColor);
        g.fillRect(0, 0, window.getWidth(), spacing*2 + buttonHeight);
        
        // draw text fields
        g.setColor(fieldBackgroundColor);
        g.fillRect((int)pathBar.getX(), (int)pathBar.getY(), (int)pathBar.getWidth(), (int)pathBar.getHeight());
        g.fillRect((int)searchBar.getX(), (int)searchBar.getY(), (int)searchBar.getWidth(), (int)searchBar.getHeight());
        
        // outlines and lines
        g.setColor(Color.black);
        g.drawRect((int)pathBar.getX(), (int)pathBar.getY(), (int)pathBar.getWidth(), (int)pathBar.getHeight());
        g.drawRect((int)searchBar.getX(), (int)searchBar.getY(), (int)searchBar.getWidth(), (int)searchBar.getHeight());
        g.drawLine(0, spacing*2 + buttonHeight, window.getWidth(), spacing*2 + buttonHeight);
        g.drawLine(spacing*2 + (int)driveArea.getWidth(), spacing*2 + buttonHeight, spacing*2 + (int)driveArea.getWidth(), window.getHeight());

        // back button
        if(clickingBack)
        {
            g.drawImage(backButtonClickedImage, (int)backButton.getX(), (int)backButton.getY(), (int)backButton.getWidth(), (int)backButton.getHeight(), null);
        }
        else
        {
            g.drawImage(backButtonImage, (int)backButton.getX(), (int)backButton.getY(), (int)backButton.getWidth(), (int)backButton.getHeight(), null);
        }

        // refresh button
        if(clickingRefresh)
        {
            g.drawImage(refreshButtonClickedImage, (int)refreshButton.getX(), (int)refreshButton.getY(), (int)refreshButton.getWidth(), (int)refreshButton.getHeight(), null);
        }
        else
        {
            g.drawImage(refreshButtonImage, (int)refreshButton.getX(), (int)refreshButton.getY(), (int)refreshButton.getWidth(), (int)refreshButton.getHeight(), null);
        }
        
        // update button
        if(clickingUpdate)
        {
            g.drawImage(updateButtonClickedImage, (int)updateButton.getX(), (int)updateButton.getY(), (int)updateButton.getWidth(), (int)updateButton.getHeight(), null);
        }
        else
        {
            g.drawImage(updateButtonImage, (int)updateButton.getX(), (int)updateButton.getY(), (int)updateButton.getWidth(), (int)updateButton.getHeight(), null);
        }

        // display the path and search bar
        if(searching)
        {
            g.drawString("[searching for files]", (int)pathBar.getX() + fileSpacing, (int)pathBar.getY() + ((int)pathBar.getHeight() - fontSize) / 3 + fontSize);
            g.drawString(searchBarText, (int)searchBar.getX() + fileSpacing, (int)searchBar.getY() + ((int)searchBar.getHeight() - fontSize) / 3 + fontSize);
        }
        else
        {
            if(pathText.isEmpty())
            {
                g.drawString("[click on a drive to begin]", (int)pathBar.getX() + fileSpacing, (int)pathBar.getY() + ((int)pathBar.getHeight() - fontSize) / 3 + fontSize);
            }
            else
            {
                g.drawString(pathText, (int)pathBar.getX() + fileSpacing, (int)pathBar.getY() + ((int)pathBar.getHeight() - fontSize) / 3 + fontSize);
            }
            g.drawString("[click to search]", (int)searchBar.getX() + fileSpacing, (int)searchBar.getY() + ((int)searchBar.getHeight() - fontSize) / 3 + fontSize);
        }

        // display the drives
        if(!(listDrives == null))
        {
            for(int step = 0; step < listDrives.size(); step++)
            {
                if(clickRelativePositionInDriveList == step)
                {
                    g.setColor(fileClickedColor);
                    g.fillRect((int)driveArea.getX(), (int)driveArea.getY() + step*(driveHeight + fileSpacing), (int)driveArea.getWidth(), driveHeight);
                    g.setColor(Color.black);
                }
                g.drawString(listDrives.get(step).getDisplayName(), (int)driveArea.getX(), (int)driveArea.getY() + fontSize + step*(driveHeight + fileSpacing));
            }
        }
        

        fontSize = 14;
        g.setFont(new Font("Arial", Font.PLAIN, fontSize));
        // display the files
        if(!(listFiles == null))
        {
            for(int step = 0; step < listFiles.size(); step++)
            {
                if(clickRelativePositionInFileList == step)
                {
                    g.setColor(fileClickedColor);
                    g.fillRect((int)fileArea.getX(), (int)fileArea.getY() + step*(fileHeight + fileSpacing), (int)fileArea.getWidth(), fileHeight);
                    g.setColor(Color.black);
                }
                g.drawString(listFiles.get(step).getName() + "          Path -> " + listFiles.get(step).getPath(), (int)fileArea.getX(), (int)fileArea.getY() + fontSize + step*(fileHeight + fileSpacing));
            }
        }
        g.setColor(Color.blue);
        g.fillRect(50, 50, 3, 3);
    }

    private void resizeWindow()
    {
        // constant value adjust due to window thinking it bigger than it actually is
        int windowHeight = window.getHeight() - 35;
        int windowWidth = window.getWidth() - 12;

        // cubit defined to divide width by a percentage
        double cubit = (windowWidth - (buttonHeight*3 + spacing*6)) / 128.0;

        // set rectangles for bounding boxes of buttons
        backButton.setBounds(spacing, spacing, buttonHeight, buttonHeight);
        pathBar.setBounds(spacing + (int)backButton.getX() + (int)backButton.getWidth(), spacing, (int)(cubit*80.0), buttonHeight);
        searchBar.setBounds(spacing + (int)pathBar.getX() + (int)pathBar.getWidth(), spacing, (int)(cubit*48.0), buttonHeight);
        refreshButton.setBounds(spacing + (int)searchBar.getX() + (int)searchBar.getWidth(), spacing, buttonHeight, buttonHeight);
        updateButton.setBounds(spacing + (int)refreshButton.getX() + (int)refreshButton.getWidth(), spacing, buttonHeight, buttonHeight);

        // set more rectangles for the bounding boxes of file and drive spaces
        driveArea.setBounds(spacing, spacing*3 + buttonHeight, 250, windowHeight - (spacing*4 + buttonHeight));
        fileArea.setBounds(spacing*3 + (int)driveArea.getWidth(), spacing*3 + buttonHeight, windowWidth - (spacing*4 + (int)driveArea.getWidth()), windowHeight - (spacing*4 + buttonHeight));
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        /* marks where the user clicked, but does not perform the action -> action taken when the mouse is released */
        System.out.println(e.getPoint());

        // adjusts for the fact that the mouse at (0,0) on screen is (7,30) when printed
        Point actualMousePosition = new Point(e.getX() - 7, e.getY() - 30);

        if(backButton.contains(actualMousePosition))
        {
            // clicking back button
            clickingBack = true;
        }
        else if(searchBar.contains(actualMousePosition))
        {
            // user wants to search
            clickingSearch = true;
        }
        else if(refreshButton.contains(actualMousePosition))
        {
            // user wants to refresh connection to database
            clickingRefresh = true;
        }
        else if(updateButton.contains(actualMousePosition))
        {
            // clicking the update button
            clickingUpdate = true;
        }
        else if(driveArea.contains(actualMousePosition))
        {
            // user clicking on a drive
            clickingDrive = true;

            // calculates which drive (relative position)
            clickRelativePositionInDriveList = (int)((actualMousePosition.getY() - (int)driveArea.getY()) / (driveHeight + fileSpacing));
            // accounts for space under the drive option
            if((int)actualMousePosition.getY() > (int)driveArea.getY() + (clickRelativePositionInDriveList)*(driveHeight + fileSpacing) + driveHeight)
            {
                // clicked too low, no drive
                clickRelativePositionInDriveList = -1;
            }
        }
        else if(!searching && fileArea.contains(actualMousePosition))
        {
            // user clicking on a file
            clickingFile = true;

            // calculates which file (relative position)
            clickRelativePositionInFileList = (int)((actualMousePosition.getY() - (int)fileArea.getY()) / (fileHeight + fileSpacing));

            // accounts for space under the file option
            if((int)actualMousePosition.getY() > (int)fileArea.getY() + (clickRelativePositionInFileList)*(fileHeight + fileSpacing) + fileHeight)
            {
                // clicked too low, no file
                clickRelativePositionInFileList = -1;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        /* upon release of the mouse, given that it is over the same section of screen that it began its click on, take action */

        // adjusts for the fact that the mouse at (0,0) on screen is (7,30) when printed
        Point actualMousePosition = new Point(e.getX() - 7, e.getY() - 30);

        if(clickingBack && backButton.contains(actualMousePosition))
        {
            // back button clicked
            if(searching)
            {
                resetToHome();
            }
            else
            {
                if(!path.isEmpty())
                {
                    path.pop();

                    // pull the last folder off the path
                    if(path.isEmpty())
                    {
                        pathText = currentDrive.getDisplayName() + " : ";
                    }
                    else
                    {
                        pathText = pathText.substring(0, pathText.length()-2);
                        pathText = pathText.substring(0, pathText.lastIndexOf("/") + 1);
                    }
                }
            }
            
        }
        else if(clickingSearch && searchBar.contains(actualMousePosition))
        {
            // user wants to search
            searching = true;
        }
        else if(clickingRefresh && refreshButton.contains(actualMousePosition))
        {
            // refreshing connection to database
            int refresh = JOptionPane.showConfirmDialog(null, "Refresh Connection to Database?\n(this will take you back to [Select Drive])", "Refresh Database Connection", JOptionPane.OK_CANCEL_OPTION);
            if(refresh == JOptionPane.OK_OPTION)
            {
                listDrives = database.getDrives();
                resetToHome();
            }
        }
        else if(clickingUpdate && updateButton.contains(actualMousePosition))
        {
            // handles clicking the update button
            if(driveScanner.detectDrives())
            {
                // TODO: 
                // need to know what drive we're dealing with
                // then confirm update with that drive
                int updateDB = JOptionPane.showConfirmDialog(null, "Confirm Update to Database with Drive ()?", "Update Database", JOptionPane.YES_NO_OPTION);
                if (updateDB == JOptionPane.YES_OPTION)
                {
                    System.out.println("User chose Yes.");
                }
            }
            else
            {
                // no drive detected
                JOptionPane.showMessageDialog(null, "Error: Could not detect any hard drives connected");
            }
            
        }
        else if(clickingDrive && driveArea.contains(actualMousePosition))
        {
            // user has clicked on a drive
            
            // calculate the area where the user ought to have clicked for a successful drive selection
            // this includes checking which relative drive we unclicked on and verifying it is the same as the one we started to click on
            // use a checking method rather than recalculating the position
            int holdMouseYPos = (int)actualMousePosition.getY();
            int holdTopPosDrive = (int)(driveArea.getY() + clickRelativePositionInDriveList*(driveHeight + fileSpacing));
            if(holdMouseYPos >= holdTopPosDrive && holdMouseYPos <= (holdTopPosDrive + driveHeight))
            {
                // indeed have clicked on a drive
                if(clickRelativePositionInDriveList >= 0 && !(listDrives == null) && clickRelativePositionInDriveList < listDrives.size())
                {
                    // actually clicked on a drive that exists
                    currentDrive = listDrives.get(clickRelativePositionInDriveList);
                    listFiles = database.getFiles(currentDrive);
                    path.clear();
                    pathText = currentDrive.getDisplayName() + " : ";
                    searching = false;
                    searchBarText = "";
                }
            }
        }
        else if(clickingFile && fileArea.contains(actualMousePosition))
        {
            // user has clicked on a file

            // the calculations are done the exact same way as done for checking the drive area (see above)
            int holdMouseYPos = (int)actualMousePosition.getY();
            int holdTopPosFile = (int)(fileArea.getY() + clickRelativePositionInFileList*(fileHeight + fileSpacing));
            if(holdMouseYPos >= holdTopPosFile && holdMouseYPos <= (holdTopPosFile + fileHeight))
            {
                // indeed have clicked on a file
                if(clickRelativePositionInFileList >= 0 && !(listFiles == null) && clickRelativePositionInFileList < listFiles.size())
                {
                    // actually clicked a file that exists
                    FileItem holdFile = listFiles.get(clickRelativePositionInFileList);
                    if(holdFile.isFolder())
                    {
                        // only folders can truly be entered
                        path.add(holdFile);
                        listFiles = database.getFiles(currentDrive, holdFile);
                        pathText += holdFile.getName() + "/";
                    }
                }
            }
        }
        resetClick();
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        // handle keyboard input
        int key = e.getKeyCode();
        if(searching)
        {
            if(key == 10)
            {
                // searching now (enter)
                listFiles = database.getSearchResults(searchBarText);
            }
            else if(key >= 32 && key <= 126)
            {
                // normal input
                // TODO: ok that single and double quote, along with tilde thing and backwards quote not being able to be pressed is ok?
                searchBarText += e.getKeyChar();
            }
            else if(key == 8 && searchBarText.length() > 0)
            {
                // backspace
                searchBarText = searchBarText.substring(0, searchBarText.length()-1);
            }
            else if(key == 27)
            {
                // escape
                resetToHome();
            }
        }
    }

    public void resetClick()
    {
        clickingBack = false;
        clickingSearch = false;
        clickingRefresh = false;
        clickingUpdate = false;
        clickingDrive = false;
        clickingFile = false;

        clickRelativePositionInDriveList = -1;
        clickRelativePositionInFileList = -1;
    }

    public void resetToHome()
    {
        currentDrive = null;
        path.clear();
        pathText = "";
        searchBarText = "";
        listFiles = null;
        searching = false;
    }
    
    public void mouseClicked(MouseEvent e){}
    public void keyReleased(KeyEvent e){}
    public void mouseExited(MouseEvent e){}
    public void mouseEntered(MouseEvent e){}
	public void keyTyped(KeyEvent e){}
}