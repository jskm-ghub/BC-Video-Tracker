/**
 * Drive stores the relevant data about a drive connected to user's computer.
 *
    • Attributes:
        -driveSerialName: String (the unique manufacturer’s serial number of the hard drive)
        -driveDisplayName: String (what the user called the hard drive)
    • Methods:
        ◦ + getters only
 * @author James Krishnan Myjak
 */

public class Drive {
    private String driveSerialName;
    private String driveDisplayName;

    public Drive(String serialName, String displayName) {
        driveSerialName = serialName;
        driveDisplayName = displayName;
    }
    public String getSerialName() {
        return this.driveSerialName;
    }
    public String getDisplayName() {
        return this.driveDisplayName;
    }
    //may be needed if someone renames the drive (if supported)
    public void setDisplayName(String displayName) {
        driveDisplayName = displayName;
    }
}
