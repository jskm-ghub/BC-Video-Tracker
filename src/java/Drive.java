import java.io.File;

/**
 * Drive stores the relevant data about a drive connected to user's computer.
 *
    • Attributes:
        -driveSerialName: String (the unique manufacturer’s serial number of the hard drive)
        -driveDisplayName: String (what the user called the hard drive)
        -driveID: int (the integer ID of the drive, as used in SQL table)
    • Methods:
        ◦ + getters
          + setter for driveID
 * @author James Krishnan Myjak
 */

public class Drive {
    private String driveSerialName;
    private String driveDisplayName;
    private int driveID;
    private File driveRootFolder;

    /**
     * Constructor; sets the driveSerialName and driveDisplayName of the drive
     * created based on parameters. The drive's ID is not set by this 
     * constructor; it must be set using the <code>setDriveID</code> method.
     * @param serialName the serial number of the drive (should be unique!)
     * @param displayName the name of the drive as displayed on a file browser
     */
    public Drive(String serialName, String displayName) {
        driveSerialName = serialName;
        driveDisplayName = displayName;
        driveRootFolder = null;
    }
    /**
     * Constructor; sets the driveSerialName and driveDisplayName of the drive
     * created based on parameters. The drive's ID is not set by this 
     * constructor; it must be set using the <code>setDriveID</code> method.
     * @param serialName the serial number of the drive (should be unique!)
     * @param displayName the name of the drive as displayed on a file browser
     * @param rootFolder File object representing the root folder on this drive
     */
    public Drive(String serialName, String displayName, File rootFolder) {
        driveSerialName = serialName;
        driveDisplayName = displayName;
        driveRootFolder = rootFolder;
    }

    /**
     * returns the driveSerialName field (the unique serial number of the drive)
     */
    public String getSerialName() {
        return this.driveSerialName;
    }
    /**
     * returns the driveDisplayName field (the name that the drive is displayed
     * as in a file browser; e.g. "MyBigDrive")
     */
    public String getDisplayName() {
        return this.driveDisplayName;
    }
    //may be needed if someone renames the drive (if supported)
    /**
     * Sets the driveDisplayName field of this Drive with the argument given.
     */
    public void setDisplayName(String displayName) {
        driveDisplayName = displayName;
    }

    /**
     * returns the driveID field (the numerical integer ID of the drive, as
     * used in the SQL database!)
     * 
     * Watch out to not use this method without setting the ID first!
     */
    public int getDriveID() {
        return driveID;
    }
    /**
     * Sets the driveID field (the numerical integer ID of the drive, as used
     * in the SQL database).
     */
    public void setDriveID(int newID) {
        this.driveID = newID;
    }

    /**
     * returns the driveRootFolder object (the immutable File object which
     * represents the root folder of this drive; could be null!)
     * 
     * Watch out for whether the root folder is null!
     */
    public File getDriveRootFolder() {
        return driveRootFolder;
    }
    //no setter necessary since the folder is either known on creation (if used when scanning drive) or not used at all)

    /**
     * Returns a string containing the driveSerialName and driveDisplayName 
     * attributes ONLY.
     * If it is certain that driveID has been set for this object, and that 
     * field is desired, use <code>toStringWithID()</code> instead.
     */
    public String toString() {
        StringBuilder outputBuilder = new StringBuilder("Drive(");
        outputBuilder.append(this.driveSerialName);
        outputBuilder.append(", ");
        outputBuilder.append(this.driveDisplayName);
        outputBuilder.append(")");
        return outputBuilder.toString();
    }

    /**
     * Returns a string containing the driveID, driveSerialName, and 
     * driveDisplayName attributes.
     * Beware of trying to call this if the driveID hasn't been set yet!!
     */
    public String toStringWithID() { //TODO: remove and handle this in the toString (toString is called by println())
        StringBuilder outputBuilder = new StringBuilder("Drive(");
        outputBuilder.append(this.driveID);
        outputBuilder.append(", ");
        outputBuilder.append(this.driveSerialName);
        outputBuilder.append(", ");
        outputBuilder.append(this.driveDisplayName);
        outputBuilder.append(")");
        return outputBuilder.toString();
    }
}
