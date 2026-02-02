
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

    /**
     * Constructor for a FileItem object, including the optional size variable.
     * @param fileID the integer ID of the file, as used in SQL fileItem table
     * @param name the filename (e.g. "QuidHocEst.jpg")
     * @param path the full file/directory path of this (e.g. "/drive2/thing/")
     * @param isFolder true if this object represents a folder/directory; false
     * if it represents a file
     * @param driveID the integer ID of the drive that this file/folder is on,
     * as used in the SQL drive table
     * @param size the (optional) size of this file/folder, as a long integer
     * @param parentID the integer ID of the parent folder of this file/folder
     * (as used in the SQL fileItem table)
     */
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
    /**
     * Constructor for a FileItem object, without the optional size variable.
     * In this, this.size is set to null to avoid errors when returning it.
     * 
     * See the other constructor for more information.
     */
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
    /**
     * returns the fileID field as noted.
     */
    public int getFileID() {
        return this.fileID;
    }
    /**
     * returns the name field as noted.
     */
    public String getName() {
        return this.name;
    }
    /**
     * returns the path field as noted.
     */
    public String getPath() {
        return this.path;
    }
    /**
     * returns the isFolder field as noted.
     * @return true if this FileItem object represents a folder; false otherwise
     */
    public boolean isFolder() {
        return this.isFolder;
    }
    /**
     * returns the driveID field as noted (the drive that this FileItem is on).
     */
    public int getDriveID() {
        return this.driveID;
    }
    /**
     * Returns the value of the optional size field.
     * @return the size of this fileItem (if set) - returns <code>null</code>
     * if it is not set.
     */
    public Long getSize() {
        return this.size;
    }
    /**
     * returns the parentID field (the fileID of the parent folder) as noted.
     */
    public int getParentID() {
        return this.parentID;
    }
    /**
     * Sets the size field of this FileItem (especially if it isn't yet)
     * @param size the new size.
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Returns a string containing all 7 attributes of this FileItem object.
     */
    public String toString() {
        StringBuilder outputBuilder = new StringBuilder("FileItem(");
        outputBuilder.append(this.fileID);
        outputBuilder.append(", ");
        outputBuilder.append(this.name);
        outputBuilder.append(", ");
        outputBuilder.append(this.path);
        outputBuilder.append(", ");
        outputBuilder.append(this.isFolder);
        outputBuilder.append(", ");
        outputBuilder.append(this.driveID);
        outputBuilder.append(", ");
        outputBuilder.append(this.size);
        outputBuilder.append(", ");
        outputBuilder.append(this.parentID);
        outputBuilder.append(")");
        return outputBuilder.toString();
    }
}
