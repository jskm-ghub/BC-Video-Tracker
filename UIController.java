// For the Display Window
import javax.swing.JPanel;
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
import java.util.ArrayList;

public class UIController extends JPanel implements ActionListener, MouseListener, KeyListener
{
    // window management
    private JFrame window;
    private Timer clock;
    private boolean screenUpdated;
    private boolean searching;
    private final int spacing = 12; // spacing between buttons
    private final int buttonHeight = 35; // height (and width) of buttons
    private final int driveHeight = 25; // height of drives shown in drive area
    private final int fileHeight = 20; // height of files shown in file area
    private final int fileSpacing = 7; // spacing between files and drives in their respective areas
    private final Color topPanelColor = new Color(255, 238, 176);
    private final Color mainPanelColor = new Color(255, 246, 212);
    private final Color fileClickedColor = new Color(206, 231, 240);
    private boolean clickingBack, clickingSearch, clickingRefresh, clickingUpdate, clickingDrive, clickingFile;
    int clickRelativePositionInDriveList, clickRelativePositionInFileList;

    // application specific objects
    private DBManager database;
    private DriveScanner driveScanner;

    // file navigation
    private Stack<Integer> path;
    private Drive currentDrive;

    // buttons
    Rectangle backButton, pathBar, searchBar, refreshButton, updateButton, fileArea, driveArea;
    String searchBarText;

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
                System.out.println("closing now");
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

        // constant update to window
        clock = new Timer(10,this);
        clock.start();

        // create window sections
        backButton = new Rectangle();
        pathBar = new Rectangle();
        searchBar = new Rectangle();
        refreshButton = new Rectangle();
        updateButton = new Rectangle();

        fileArea = new Rectangle();
        driveArea = new Rectangle();
        resizeWindow();

        // checks where we are clicking
        resetClick();

        searchBarText = "";

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        window.repaint();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        render(g);
        resizeWindow();
    }

    private void render(Graphics g)
    {
        int fontSize = 16;
        g.setFont(new Font("Arial", Font.PLAIN, fontSize));
        g.setColor(topPanelColor);
        g.fillRect(0, 0, window.getWidth(), spacing*2 + buttonHeight);

        g.setColor(Color.black);
        g.fillRect((int)backButton.getX(), (int)backButton.getY(), (int)backButton.getWidth(), (int)backButton.getHeight());
        g.fillRect((int)pathBar.getX(), (int)pathBar.getY(), (int)pathBar.getWidth(), (int)pathBar.getHeight());
        g.fillRect((int)searchBar.getX(), (int)searchBar.getY(), (int)searchBar.getWidth(), (int)searchBar.getHeight());
        g.fillRect((int)refreshButton.getX(), (int)refreshButton.getY(), (int)refreshButton.getWidth(), (int)refreshButton.getHeight());
        g.fillRect((int)updateButton.getX(), (int)updateButton.getY(), (int)updateButton.getWidth(), (int)updateButton.getHeight());

        g.drawLine(0, spacing*2 + buttonHeight, window.getWidth(), spacing*2 + buttonHeight);
        g.drawLine(spacing*2 + (int)driveArea.getWidth(), spacing*2 + buttonHeight, spacing*2 + (int)driveArea.getWidth(), window.getHeight());

        //g.fillRect((int)driveArea.getX(), (int)driveArea.getY(), (int)driveArea.getWidth(), (int)driveArea.getHeight());
        //g.fillRect((int)fileArea.getX(), (int)fileArea.getY(), (int)fileArea.getWidth(), (int)fileArea.getHeight());

        // display the drives
        g.setColor(Color.black);
        for(int step = 0; step < 6; step++)
        {
            if(clickRelativePositionInDriveList == step)
            {
                g.setColor(fileClickedColor);
                g.fillRect((int)driveArea.getX(), (int)driveArea.getY() + step*(driveHeight + fileSpacing), (int)driveArea.getWidth(), driveHeight);
                g.setColor(Color.black);
            }
            g.drawString("ugga bugga", (int)driveArea.getX(), (int)driveArea.getY() + fontSize + step*(driveHeight + fileSpacing));
        }

        fontSize = 14;
        g.setFont(new Font("Arial", Font.PLAIN, fontSize));
        // display the files
        if(searching)
        {
            // display search results
        }
        else
        {
            // otherwise display files in the folder given in the path
            for(int step = 0; step < 25; step++)
            {
                if(clickRelativePositionInFileList == step)
                {
                    g.setColor(fileClickedColor);
                    g.fillRect((int)fileArea.getX(), (int)fileArea.getY() + step*(fileHeight + fileSpacing), (int)fileArea.getWidth(), fileHeight);
                    g.setColor(Color.black);
                }
                g.drawString("many long file \t many long very much path yes \t maybe some other info", (int)fileArea.getX(), (int)fileArea.getY() + fontSize + step*(fileHeight + fileSpacing));
            }
        }
        
    }

    private void resizeWindow()
    {
        int windowHeight = window.getHeight() - 35;
        int windowWidth = window.getWidth() - 12;
        double cubit = (windowWidth - (buttonHeight*3 + spacing*6)) / 128.0;

        backButton.setBounds(spacing, spacing, buttonHeight, buttonHeight);
        pathBar.setBounds(spacing + (int)backButton.getX() + (int)backButton.getWidth(), spacing, (int)(cubit*80.0), buttonHeight);
        searchBar.setBounds(spacing + (int)pathBar.getX() + (int)pathBar.getWidth(), spacing, (int)(cubit*48.0), buttonHeight);
        refreshButton.setBounds(spacing + (int)searchBar.getX() + (int)searchBar.getWidth(), spacing, buttonHeight, buttonHeight);
        updateButton.setBounds(spacing + (int)refreshButton.getX() + (int)refreshButton.getWidth(), spacing, buttonHeight, buttonHeight);

        driveArea.setBounds(spacing, spacing*3 + buttonHeight, 250, windowHeight - (spacing*4 + buttonHeight));
        fileArea.setBounds(spacing*3 + (int)driveArea.getWidth(), spacing*3 + buttonHeight, windowWidth - (spacing*4 + (int)driveArea.getWidth()), windowHeight - (spacing*4 + buttonHeight));

    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        /* marks where the user clicked, but does not perform the action -> action taken when the mouse is released */

        // adjusts for the fact that the mouse at (0,0) on screen is (7,30) when printed
        Point actualMousePosition = new Point(e.getX() - 7, e.getY() - 30);

        if(!searching && backButton.contains(actualMousePosition))
        {
            // clicking back button
            // back button disabled during search
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
            System.out.println(clickRelativePositionInDriveList);
        }
        else if(fileArea.contains(actualMousePosition))
        {
            // user clicking on a file
            clickingFile = true;

            // calculates which file (relative position)
            clickRelativePositionInFileList = (int)((actualMousePosition.getY() - (int)fileArea.getY()) / (fileHeight + fileSpacing));
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
            System.out.println("clicked back");
        }
        else if(clickingSearch && searchBar.contains(actualMousePosition))
        {
            // user wants to search
            searching = true;
        }
        else if(clickingRefresh && refreshButton.contains(actualMousePosition))
        {
            // refreshing connection to database
        }
        else if(clickingUpdate && updateButton.contains(actualMousePosition))
        {
            // handles clicking the update button
            int updateDB = JOptionPane.showConfirmDialog(null, "Confirm Update to Database with Drive ()?", "Update Database", JOptionPane.YES_NO_OPTION);
            if (updateDB == JOptionPane.YES_OPTION)
            {
                System.out.println("User chose Yes.");
            }
        }
        else if(clickingDrive && driveArea.contains(actualMousePosition))
        {
            // user has clicked on a drive
            
            // calculate the area where the user ought to have clicked for a successful drive selection
            // this includes checking which relative drive we unclicked on and verifying it is the same as the one we started to click on
            // since the initial drive selection did not take into account the spacing beneath the drive, now we do
            // use a checking method rather than recalculating the position
            int holdMouseYPos = (int)actualMousePosition.getY();
            int holdTopPosDrive = (int)(driveArea.getY() + clickRelativePositionInDriveList*(driveHeight + fileSpacing));
            if(holdMouseYPos >= holdTopPosDrive && holdMouseYPos <= (holdTopPosDrive + driveHeight))
            {
                // indeed have clicked on a drive
                System.out.println("Truly clicked on drive: " + clickRelativePositionInDriveList);
                searching = false;
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
                System.out.println("Truly clicked on file: " + clickRelativePositionInFileList);
            }
        }

        resetClick();
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        // handle keyboard input
        System.out.println("pressed: " + e.getKeyCode());
        if(searching)
        {
            
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
    
    public void mouseClicked(MouseEvent e) {}
    public void keyReleased(KeyEvent e){}
    public void mouseExited(MouseEvent e) {}
    public void mouseEntered(MouseEvent e){}
	public void keyTyped(KeyEvent e) {}

}