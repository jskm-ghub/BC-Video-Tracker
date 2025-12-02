
/**
 * FileItem stores the relevant data for a file or folder in the system (read ultimately from a drive connected to user's computer).
 *
    • Attributes:
        - fileID : int (primary key)
        - name : String (name of file/folder)
        - path : String (path in the drive)
        - isFolder: boolean
        - driveID : int (Foreign Key reference to drives Table)
        - size : int (long?) (file size (not 100% necessary))
        - parentID: int (parent folder ID( in between string or int))
    • Methods:
        ◦ + FileItem(name : String, path : String, type : String, driveId : int, size : long, parentId : creates a new item that represents a file/folder
        ◦ + getName() : 
        ◦ + getPath() :
        ◦ + getType() : 
        ◦ + getDriveId() : 
        ◦ + getSize() : 
        ◦ + getParentId() : 
        ◦ + toString() :
 * @author James Krishnan Myjak
 */
public class FileItem {
    private int fileID;
    private String name;
    private String path;
    //False means the item object is a file; true is a folder
    private boolean isFolder;
    private int driveID;
    //Contradictory documentation given; I think this should be a long?
    private Long size;
    private int parentID;

    public FileItem(int fileID, String name, String path, boolean isFolder, int driveID, long size, int parentID) {
        this.fileID = fileID;
        this.name = name;
        this.path = path;
        this.isFolder = isFolder;
        this.driveID = driveID;
        this.size = size;
        this.parentID = parentID;
    }

    /* Since the size wasn't considered 100% necessary, I thought I'd give an option to make without it */
    public FileItem(int fileID, String name, String path, boolean isFolder, int driveID, int parentID) {
        this.fileID = fileID;
        this.name = name;
        this.path = path;
        this.isFolder = isFolder;
        this.driveID = driveID;
        //Is the below strictly necessary? May be null already
        this.size = null;
        this.parentID = parentID;
    }

    /* getters (I can't think of a reason for any setters other than for size since all fields are required to begin with and changing a file/folder entity midway through seems like a bad idea!) */
    public int getFileID() {
        return this.fileID;
    }
    public String getName() {
        return this.name;
    }
    public String getPath() {
        return this.path;
    }
    public boolean isFolder() {
        return this.isFolder;
    }
    public int getDriveID() {
        return this.driveID;
    }
    public long getSize() {
        return this.size;
    }
    public int getParentID() {
        return this.parentID;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String toString() {
        return "FileItem(" + this.fileID + this.name + this.path + this.isFolder + this.driveID + this.size + this.parentID + ")";
    }
}
