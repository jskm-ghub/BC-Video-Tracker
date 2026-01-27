// For the Display Window
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.JOptionPane;
import java.awt.event.*;

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

public class UIController extends JPanel implements ActionListener, MouseListener, KeyListener, MouseWheelListener
{
    /*  ~~~~~ WINDOW MANAGEMENT ~~~~~ */
    private final JFrame window;
    private final Timer clock;
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
    // Fonts
    private final int mainTextFontSize = 16;
    private final int fileTextFontSize = 14;
    private final Font mainTextFont = new Font("Arial", Font.PLAIN, mainTextFontSize);
    private final Font fileTextFont = new Font("Arial", Font.PLAIN, fileTextFontSize);
    // flags
    private boolean clickingBack, clickingSearch, clickingRefresh, clickingUpdate, clickingDrive, clickingFile;
    private boolean screenUpdated;
    private boolean searching;
    // position markers
    private int clickRelativePositionInDriveList, clickRelativePositionInFileList;
    // scrolling
    private int scrollDrivePos; // (scrolling Drive area Position) how much scroll to apply to drive section
    private int scrollFilePos; // (scrolling File area Position) how much scroll to apply to file section
    private int maxDriveLines; // maximum number of drives possible to display
    private int maxFileLines; // max number of files possible to display

    /* ~~~~~ APPLICATION SPECIFIC OBJECTS ~~~~~ */
    private final DBManager database;
    private final DriveScanner driveScanner;

    /* ~~~~~ FILE NAVIGATION ~~~~~ */
    private final Stack<FileItem> path; // holds the file path in the order that we have descended into the drive
    private Drive currentDrive;
    private List<Drive> listDrives; // all the drives in the system
    private List<FileItem> listFiles; // all the files in whatever drive we are looking at

    /* ~~~~~ BUTTONS ~~~~~ */
    // button positions and sizes
    private final Rectangle backButton, pathBar, searchBar, refreshButton, updateButton, fileArea, driveArea;
    // text values
    private String searchBarText;
    private String pathText;
    // button images
    private final Image backButtonImage = Toolkit.getDefaultToolkit().createImage(UIController.class.getResource("/images/back_button.png"));
    private final Image backButtonClickedImage = Toolkit.getDefaultToolkit().createImage(UIController.class.getResource("/images/back_button_clicked.png"));
    private final Image refreshButtonImage = Toolkit.getDefaultToolkit().createImage(UIController.class.getResource("/images/refresh_button.png"));
    private final Image refreshButtonClickedImage = Toolkit.getDefaultToolkit().createImage(UIController.class.getResource("/images/refresh_button_clicked.png"));
    private final Image updateButtonImage = Toolkit.getDefaultToolkit().createImage(UIController.class.getResource("/images/update_button.png"));
    private final Image updateButtonClickedImage = Toolkit.getDefaultToolkit().createImage(UIController.class.getResource("/images/update_button_clicked.png"));

    /**
     * Constructor for the object that controls the entire UI, creates a window, handles user input.
     * @param dbm handles connection and data transfer for the database
     * @param ds handles connection and data transfer for the plugged in hard drive
     */
    public UIController(DBManager dbm, DriveScanner ds)
    {
        this.database = dbm;
        this.driveScanner = ds;

        // create window sections - buttons, fields, file and drive areas
        backButton = new Rectangle();
        pathBar = new Rectangle();
        searchBar = new Rectangle();
        refreshButton = new Rectangle();
        updateButton = new Rectangle();
        fileArea = new Rectangle();
        driveArea = new Rectangle();

        // initialize specific variables
        listDrives = dbm.getDrives();
        path = new Stack<FileItem>();
        resetToHome();

        // checks where we are clicking
        resetClick();

        // creates window
        window = new JFrame();
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                // everything that needs to happen on shutdown
                // TODO: anything else need to be done on shutdown?
                database.closeConnection();
                window.dispose();
                System.exit(0);
            }
        }
        );
        window.setVisible(true);
        window.setSize(842,500);
        window.add(this); // that is, the JPanel
        this.setBackground(mainPanelColor);
        resizeWindow();

        // listens to mouse and keyboard
        addKeyListener(this);
        addMouseListener(this);
        addMouseWheelListener(this);

        // important for key listener
        setFocusable(true);
        requestFocusInWindow();

        // constant update to window
        clock = new Timer(10,this);
        clock.start();
    }

    /**
     * The method called by the clock, calls the repaint method
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        // TODO: maybe not repaint every time?
        this.repaint();
    }

    /**
     * Handles painting the screen.
     * @param g the <code>Graphics</code> object to protect
     */
    @Override
    public void paintComponent(Graphics g)
    {
        // TODO: maybe we don't have to resize every single time?
        super.paintComponent(g);
        render(g);
        resizeWindow();
    }

    /**
     * Handles all objects that should be painted to the screen.
     * @param g Graphics object
     */
    private void render(Graphics g)
    {
        // draw top panel
        g.setColor(topPanelColor);
        g.fillRect(0, 0, this.getWidth(), spacing*2 + buttonHeight);
        
        // draw text fields
        g.setColor(fieldBackgroundColor);
        g.fillRect((int)pathBar.getX(), (int)pathBar.getY(), (int)pathBar.getWidth(), (int)pathBar.getHeight());
        g.fillRect((int)searchBar.getX(), (int)searchBar.getY(), (int)searchBar.getWidth(), (int)searchBar.getHeight());
        
        // draw outlines and lines
        g.setColor(Color.black);
        g.drawRect((int)pathBar.getX(), (int)pathBar.getY(), (int)pathBar.getWidth(), (int)pathBar.getHeight());
        g.drawRect((int)searchBar.getX(), (int)searchBar.getY(), (int)searchBar.getWidth(), (int)searchBar.getHeight());
        g.drawLine(0, spacing*2 + buttonHeight, this.getWidth(), spacing*2 + buttonHeight);
        g.drawLine(spacing*2 + (int)driveArea.getWidth(), spacing*2 + buttonHeight, spacing*2 + (int)driveArea.getWidth(), this.getHeight());

        // back, refresh, and update buttons - clicked or unclicked versions depending on reality
        g.drawImage((clickingBack) ? backButtonClickedImage : backButtonImage, (int)backButton.getX(), (int)backButton.getY(), (int)backButton.getWidth(), (int)backButton.getHeight(), null);
        g.drawImage((clickingRefresh) ? refreshButtonClickedImage:refreshButtonImage, (int)refreshButton.getX(), (int)refreshButton.getY(), (int)refreshButton.getWidth(), (int)refreshButton.getHeight(), null);
        g.drawImage((clickingUpdate) ? updateButtonClickedImage : updateButtonImage, (int)updateButton.getX(), (int)updateButton.getY(), (int)updateButton.getWidth(), (int)updateButton.getHeight(), null);

        // set font
        g.setFont(mainTextFont);

        // display the path and search bar
        if(searching)
        {
            g.drawString("[searching for files]", (int)pathBar.getX() + fileSpacing, (int)pathBar.getY() + ((int)pathBar.getHeight() - mainTextFontSize) / 3 + mainTextFontSize);
            // display what we are searching
            g.drawString(searchBarText, (int)searchBar.getX() + fileSpacing, (int)searchBar.getY() + ((int)searchBar.getHeight() - mainTextFontSize) / 3 + mainTextFontSize);
        }
        else
        {
            if(pathText.isEmpty())
            {
                g.drawString("[click on a drive to begin]", (int)pathBar.getX() + fileSpacing, (int)pathBar.getY() + ((int)pathBar.getHeight() - mainTextFontSize) / 3 + mainTextFontSize);
            }
            else
            {
                // display actual path
                g.drawString(getTruncatedString(pathText, (int)pathBar.getWidth() - spacing*2, g), (int)pathBar.getX() + fileSpacing, (int)pathBar.getY() + ((int)pathBar.getHeight() - mainTextFontSize) / 3 + mainTextFontSize);
            }
            g.drawString("[click to search]", (int)searchBar.getX() + fileSpacing, (int)searchBar.getY() + ((int)searchBar.getHeight() - mainTextFontSize) / 3 + mainTextFontSize);
        }

        // display the drives
        if(!(listDrives == null))
        {
            // need to store the value of the scroll in a local variable so it doesn't change mid-calculation
            int localScrollDrivePos = adjustScrollInBounds(scrollDrivePos, listDrives.size(), maxDriveLines);
            scrollDrivePos = localScrollDrivePos;

            for(int step = 0; step < maxDriveLines && step < listDrives.size(); step++)
            {
                if(clickRelativePositionInDriveList == step)
                {
                    g.setColor(fileClickedColor);
                    g.fillRect((int)driveArea.getX(), (int)driveArea.getY() + step*(driveHeight + fileSpacing), (int)driveArea.getWidth(), driveHeight);
                    g.setColor(Color.black);
                }

                // truncate the drive name down if needed
                g.drawString(getTruncatedString(listDrives.get(step + localScrollDrivePos).getDisplayName(), (int)driveArea.getWidth(), g), (int)driveArea.getX(), (int)driveArea.getY() + mainTextFontSize + step*(driveHeight + fileSpacing));
            }
        } else {scrollDrivePos = 0;}

        // set font for file area
        g.setFont(fileTextFont);

        // display the files
        if(!(listFiles == null))
        {
            // sets how wide we allow the file name to be
            int fileNameWidth = (int) (.6 * driveArea.getWidth());

            // need to store the value of the scroll in a local variable so it doesn't change mid-calculation
            int localScrollFilePos = adjustScrollInBounds(scrollFilePos, listFiles.size(), maxFileLines);
            scrollFilePos = localScrollFilePos;

            for(int step = 0; step < maxFileLines && step < listFiles.size(); step++)
            {
                if(clickRelativePositionInFileList == step)
                {
                    // highlights a file if it was clicked
                    g.setColor(fileClickedColor);
                    g.fillRect((int) fileArea.getX(), (int) fileArea.getY() + step * (fileHeight + fileSpacing), (int) fileArea.getWidth(), fileHeight);
                    g.setColor(Color.black);
                }
                //truncate down the file name and path name if necessary, then render them in the file area
                g.drawString(getTruncatedString(listFiles.get(step + localScrollFilePos).getName(), fileNameWidth, g), (int)fileArea.getX(), (int)fileArea.getY() + fileTextFontSize + step*(fileHeight + fileSpacing));
                g.drawString(getTruncatedString(listFiles.get(step + localScrollFilePos).getPath(), (int)(fileArea.getWidth() - (fileNameWidth + spacing)), g), (int)fileArea.getX() + fileNameWidth + spacing, (int)fileArea.getY() + fileTextFontSize + step*(fileHeight + fileSpacing));
            }
        }
        else {scrollFilePos = 0;/* if no files, then set scroll to 0 so we don't jump to the bottom of the list when files appear */}
    }

    /**
     * Adjusts the scroll value if the user attempts to scroll past the last entry of a list or above the first entry of the list
     * @param currentScrollPos The scroll value as the user has given us
     * @param numEntriesInList The total number of entries in the list to display (the whole list, not just that which will be shown)
     * @param maxLinesShow The maximum number of lines we can show data on
     * @return the potentially adjusted scroll value
     */
    private int adjustScrollInBounds(int currentScrollPos, int numEntriesInList, int maxLinesShow)
    {
        int newScroll = currentScrollPos;
        // if the number of files is less than number which we can render, proceed with no scrolling, or if negative scroll relative to top
        if(numEntriesInList <= maxLinesShow || currentScrollPos < 0)
        {
            newScroll = 0;
        }
        else if(currentScrollPos > numEntriesInList - maxLinesShow)// there exist too many files to render, so we respect the scroll
        {
            // if we have scrolled too far (such that the bottom line "should" be higher than the bottom of the screen)
            newScroll = numEntriesInList - maxLinesShow;
        }
        return newScroll;
    }

    /**
     * Truncates a String and returns the shortened version if necessary with ellipses given a max width in pixels
     * @param text String to potentially shorten
     * @param maxLength maximum length of the resulting String in pixels
     * @param g Graphics object
     * @return potentially truncated text String
     */
    private String getTruncatedString(String text, int maxLength, Graphics g)
    {
        String ellipses = "";
        while(g.getFontMetrics().stringWidth(text + ellipses) > maxLength && !text.isEmpty())
        {
            text = text.substring(0,text.length() - 1);
            ellipses = "...";
        }
        return text + ellipses;
    }

    /**
     * Sets all on-screen objects' sizes based on the window size
     */
    private void resizeWindow()
    {
        int windowHeight = getHeight();
        int windowWidth = getWidth();

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

        // define how many lines we can have in the drive and file areas
        maxDriveLines = (int)(driveArea.getHeight() / (driveHeight + fileSpacing));
        maxFileLines = (int)(fileArea.getHeight() / (fileHeight + fileSpacing));
    }

    /**
     * Handles when the user starts to click
     * @param e the event to be processed
     */
    @Override
    public void mousePressed(MouseEvent e)
    {
        /* marks where the user clicked, but does not perform the action -> action taken when the mouse is released */
        Point actualMousePosition = e.getPoint();

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

    /**
     * Handles when the user releases the mouse (finishes a click). This is where the actions occur.
     * @param e the event to be processed
     */
    @Override
    public void mouseReleased(MouseEvent e)
    {
        // upon release of the mouse, given that it is over the same section of screen that it began its click on, take action
        Point actualMousePosition = e.getPoint();

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
                        listFiles = database.getFiles(currentDrive);
                    }
                    else
                    {
                        pathText = pathText.substring(0, pathText.length()-2);
                        pathText = pathText.substring(0, pathText.lastIndexOf("/") + 1);
                        listFiles = database.getFiles(currentDrive, path.peek());
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
                // find connected drives
                List<Drive> connectedDrives = driveScanner.getDetectedDrives();
                int numDrives = connectedDrives.size();
                String[] connectedDriveNames = new String[numDrives];

                // pull out drive names for display
                for(int step = 0; step < numDrives; step++)
                {
                    connectedDriveNames[step] = connectedDrives.get(step).getDisplayName();
                }

                // user selects drive, and after confirmation, update the DB with the data
                int driveIndex = JOptionPane.showOptionDialog(null, "Please select drive to upload.", "Drive Upload Selection", JOptionPane.DEFAULT_OPTION,JOptionPane.QUESTION_MESSAGE, null, connectedDriveNames, null);
                if(driveIndex >= 0)
                {
                    int updateDB = JOptionPane.showConfirmDialog(null, "Confirm Update to Database with: \n" + connectedDriveNames[driveIndex], "Update Database", JOptionPane.YES_NO_OPTION);
                    if (updateDB == JOptionPane.YES_OPTION)
                    {
                        database.insertDrive(connectedDrives.get(driveIndex));
                    }
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
                    currentDrive = listDrives.get(clickRelativePositionInDriveList + scrollDrivePos);
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
                    FileItem holdFile = listFiles.get(clickRelativePositionInFileList + scrollFilePos);
                    System.out.println(holdFile.getName() + " has id of: " + holdFile.getFileID() + " and parent of " + holdFile.getParentID() + " and is folder:" + holdFile.isFolder());
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

    /**
     * Handles keyboard input.
     * @param e the event to be processed
     */
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
            else if(key == 8 && !searchBarText.isEmpty())
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

    /**
     * Sets all flags of what has been clicked back to their unclicked state
     */
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

    /**
     * Resets the application to the startup state.
     */
    public void resetToHome()
    {
        currentDrive = null;
        path.clear();
        pathText = "";
        searchBarText = "";
        listFiles = null;
        searching = false;
        scrollDrivePos = 0;
        scrollFilePos = 0;
    }

    /**
     * Handles scroll wheel actions.
     * @param e the event to be processed
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        Point mousePos = this.getMousePosition();
        // adds scroll wheel actions to respective scrollable areas
        if(mousePos.getY() > (2*spacing + buttonHeight))
        {
            if (mousePos.getX() > (2 * spacing + driveArea.getWidth()))
            {
                scrollFilePos += e.getWheelRotation();
            }
            else
            {
                scrollDrivePos += e.getWheelRotation();
            }
        }
    }

    // unused methods that are here because the Listeners require them to exist
    public void mouseClicked(MouseEvent e){}
    public void keyReleased(KeyEvent e){}
    public void mouseExited(MouseEvent e){}
    public void mouseEntered(MouseEvent e){}
	public void keyTyped(KeyEvent e){}
}