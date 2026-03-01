// Java Libraries
import java.sql.*;
import java.util.*;
import java.io.InputStream;

// Encryption
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class DBManager{
          
     // Database details, retrieved from encrypted file
     private final int dbPort = 3306;
     private final String host = "bc-marketingvideotracker.benedictine.edu";
     private final String dbName = "videoschema_db";
     private final String SECRET = "BCRAVENS12345678"; // This needs to match the SECRET attribute in Encryptor
     private final String CRED_DELIMITER = "<:>"; // This needs to match the CRED_DELIMITER in Encryptor
     private String dbUser, dbPass;

     private Connection connection; //JDBC connection to MySQL database
     private DriveScanner ds;

     /**
      * Constructor
      * @param scanner
      */
     public DBManager(DriveScanner scanner)
     {
          this.dbUser = "";
          this.dbPass = "";
          this.ds = scanner;

          /* Ignore unless re-encryption is necessary; Encrypts ingored credentials.json file */
          // Encryptor.encryptFile("src/secure/credentials.json","src/secure/EncryptedCredentials.enc","BCRAVENS12345678");

          decryptDbCredentials();
          System.out.println("Credentials decrypted and parsed successfully.");
          connectDatabase();
     }
     
     /**
      * Connects directly to MySQL database through port 3306.
      * Uses credentials previously decrypted
      */
     private void connectDatabase()
     {
          String dburl = "jdbc:mysql://" + host + ":" + dbPort + "/" + dbName
                    + "?useSSL=true"
                    + "&requireSSL=true"
                    + "&allowPublicKeyRetrieval=true"
                    + "&rewriteBatchedStatements=true"
                    + "&connectTimeout=5000"
                    + "&socketTimeout=5000";
          try
          {
               System.out.println("Connecting to database...");
               connection = DriverManager.getConnection(dburl, dbUser, dbPass);
               if(connection == null)
               {
                    UIController.displayDataMessage("Error: Failed to Connect to DB. Please check connectivity to BC Network");
               }
               else
               {
                    System.out.println("Database connected successfully.");
               }
          }catch(Exception e)
          {
               UIController.displayDataMessage("Error: Failed to Connect to DB. Please check connectivity to BC Network");
               e.printStackTrace();
          }
     }

     /**
      * Retrieves a map of all drives from the database
      * @return Map of Drive objects indexed by their DB ID
      */
     public Map<Integer, Drive> getDrives(){
          Map<Integer, Drive> drives = new HashMap<>();
          try
          {
               String sql = "SELECT * FROM drive";
               PreparedStatement query = connection.prepareStatement(sql);
               ResultSet rs = query.executeQuery();

               while(rs.next())
               {
                    int id = rs.getInt("driveID");
                    String serialName = rs.getString("driveSerialName");
                    String driveName = rs.getString("driveDisplayName");

                    Drive d = new Drive(id, serialName, driveName);
                    drives.put(id, d);
               }
          }catch(Exception e)
          {
               e.printStackTrace();
          }
          return drives;
     }

     /**
      * Deletes a drive from the database
      * Database function will cascade deletions to all files under that driveID
      * @param drive Drive object to delete
      */
     public void deleteDrive(Drive drive){
          try{
               String sql = "DELETE FROM drive WHERE driveID = ?;";
               PreparedStatement stmt = connection.prepareStatement(sql);
               stmt.setInt(1, drive.getDriveID());
               int rowsDeleted = stmt.executeUpdate();
               if(rowsDeleted > 0){
                    System.out.println("Drive deleted successfully.");
               }else{
                    System.out.println("Error with drive deletion.");
               }
          }catch(Exception e)
          {
               e.printStackTrace();
          }
     }

     /**
      * Inserts a drive into the database if it doesn't already exist.
      * If drive already exists, wipes existing fileItems under that driveID and rescans drive for files to update database
      * Calls insertFile() for each file found on the drive to add to the database
      * 
      * @param drive Drive object to insert
      */
     public void insertDrive(Drive drive)
     {
          /* check database for existing driveSerialName */
          String driveName = drive.getDisplayName();
          String serialName = drive.getSerialName();
          PreparedStatement stmt;
          String sql;
          ResultSet rs = null;
          int driveId = 0;
          try{
               sql = "SELECT * FROM drive WHERE driveSerialName = ?;";
               stmt = connection.prepareStatement(sql);
               stmt.setString(1, serialName);
               rs = stmt.executeQuery();
          }catch(Exception e){
               e.printStackTrace();
          }

          /* insert Drive into database if not already exists */
          try{
               if(!rs.next()){ //drive does not exist, insert
                    sql = "INSERT INTO drive (driveSerialName, driveDisplayName) VALUES (?, ?)"
                         + " ON DUPLICATE KEY UPDATE driveSerialName = VALUES(driveSerialName),"
                         + " driveDisplayName = VALUES(driveDisplayName);";
                    stmt = connection.prepareStatement(sql);
                    stmt.setString(1, serialName);
                    stmt.setString(2, driveName);
                    stmt.executeUpdate();
                    sql = "SELECT driveID FROM drive WHERE driveSerialName = '" + serialName + "';";
                    stmt = connection.prepareStatement(sql);
                    ResultSet rs2 = stmt.executeQuery();
                    driveId = 0;
                    if(rs2.next()){
                         driveId = rs2.getInt("driveID");
                    }
               }
               else
               { //drive already exists, wipe existing fileItems under that driveID and rescan drive for files to update database
                    sql = "SELECT driveID FROM drive WHERE driveSerialName = '" + serialName + "';";
                    stmt = connection.prepareStatement(sql);
                    ResultSet rs2 = stmt.executeQuery();
                    if(rs2.next())
                    {
                         driveId = rs2.getInt("driveID");
                    }
                    sql = "DELETE FROM fileItem WHERE driveID = " + driveId + ";";
                    stmt = connection.prepareStatement(sql);
                    stmt.executeUpdate();
               }
          }catch(Exception e)
          {
               UIController.displayDataMessage("Error: Failed to Insert Drive into Database -> " + drive.getDisplayName());
               e.printStackTrace();
          }
          drive.setDriveID(driveId);

          /* Insert files as a batch */
          batchInsertFiles(ds.scan(drive));
     }

     /**
      * Inserts a list of FileItems into the database as a batch for efficiency.
      * @param files List of FileItem objects to insert
      */
     private void batchInsertFiles(List<FileItem> files)
     {
          String sql = "INSERT INTO fileItem " +
               "(fileIdWithinDrive, name, path, isFolder, driveID, size, parentID) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?)";
          PreparedStatement stmt = null;

          try
          {
               connection.setAutoCommit(false);
               stmt = connection.prepareStatement(sql);

               for(FileItem file : files)
               {
                    stmt.setInt(1, file.getFileID());
                    stmt.setString(2, file.getName());
                    stmt.setString(3, file.getPath());
                    stmt.setBoolean(4, file.isFolder());
                    stmt.setInt(5, file.getDriveID());
                    stmt.setLong(6, file.getSize());

                    if(file.getParentID() == -1)
                    {
                         stmt.setNull(7, java.sql.Types.INTEGER);
                    }
                    else
                    {
                         stmt.setInt(7, file.getParentID());
                    }
               
                    stmt.addBatch();
               }

               stmt.executeBatch();
               connection.commit();
               UIController.displayDataMessage("Drive Update Successful");
          } catch(Exception e)
          {
               UIController.displayDataMessage("Error: Failed to Properly Update Drive");
               try
               {
                    connection.rollback();
               }catch(Exception ex)
               {
                    ex.printStackTrace();
               }
               e.printStackTrace();
          } finally
          {
               try
               {
                    connection.setAutoCommit(true);
                    if(stmt != null) stmt.close();
               } catch(Exception ex){
                    ex.printStackTrace();
               }
          }
     }

     /**
      * Closes all open connections
      */
     public void closeConnection(){
          try{
               if(connection != null && !connection.isClosed()){
                    connection.close();
               }
          }catch(Exception e){
               e.printStackTrace();
          }
          System.out.println("Connections closed.");
     }

     /**
      * Retrieves a list of FileItems from the database for a 
      * given Drive and parent FileItem (directory)
      * @param d Drive object
      * @param f Parent FileItem object
      * @return List of FileItem objects
      */
     public List<FileItem> getFiles(Drive d, FileItem f) {
          List<FileItem> files = new ArrayList<>();

          try{               
               int driveId = d.getDriveID();

               /* query files with given driveId and parentId */
               String sql = "SELECT * FROM fileItem WHERE driveID = ? AND parentID = ?";
               PreparedStatement query = connection.prepareStatement(sql);
               query.setInt(1, driveId);
               query.setInt(2, f.getFileID());
               ResultSet rs = query.executeQuery();

               /* parse results of query and create list of fileItems */
               while(rs.next()){
                    int id = rs.getInt("fileIdWithinDrive");
                    String fileName = rs.getString("name");
                    String filePath = rs.getString("path");
                    int parentId = rs.getInt("parentID");
                    long size = rs.getLong("size");
                    boolean isFolder = rs.getBoolean("isFolder");

                    FileItem fileItem = new FileItem(id, fileName, filePath, isFolder, driveId, size, parentId);
                    files.add(fileItem);
               }
          }catch(Exception e){
               e.printStackTrace();
          }
          return files;
     }

     /**
      * Retrieves a list of FileItems from the database for a 
      * given Drive
      * @param d Drive object
      * @return List of FileItem objects
      */
     public List<FileItem> getFiles(Drive d) {
          List<FileItem> files = new ArrayList<>();

          try{
               /* query root files from drive */
               int driveId = d.getDriveID();
               String sql = "SELECT * FROM fileItem WHERE driveID = ? AND parentID IS NULL";
               PreparedStatement query = connection.prepareStatement(sql);
               query.setInt(1, driveId);
               ResultSet rs = query.executeQuery();

               /* parse results of query and create list of fileItems */
               while(rs.next()){
                    int id = rs.getInt("fileIdWithinDrive");
                    String fileName = rs.getString("name");
                    String filePath = rs.getString("path");
                    boolean isFolder = rs.getBoolean("isFolder");
                    long size = rs.getLong("size");
                    int parentId = -1;
                                       
                    FileItem fileItem = new FileItem(id, fileName, filePath, isFolder, driveId, size, parentId);
                    files.add(fileItem);
               }
          }catch(Exception e){
               e.printStackTrace();
          }
          return files;
     }

     /**
      * Decrypts DB credentials and passes their values into the proper attributes
      */
     private void decryptDbCredentials()
     {
          try
          {
               byte[] keyBytes = SECRET.getBytes();
               SecretKey key = new SecretKeySpec(keyBytes, "AES");    
               Cipher cipher = Cipher.getInstance("AES");
               cipher.init(Cipher.DECRYPT_MODE, key);

               try(InputStream fileIn = UIController.class.getResourceAsStream("/EncryptedCredentials.txt"))
               {
                   if(fileIn != null)
                   {
                        byte[] encryptedBytes = Base64.getDecoder().decode(fileIn.readAllBytes());
                        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
                        String[] decryptedCredentials = new String(decryptedBytes).split(CRED_DELIMITER);
                        dbUser = decryptedCredentials[0];
                        dbPass = decryptedCredentials[1];
                   }
               }
          }catch(Exception e)
          {
               e.printStackTrace();
          }
     }

     /**
      * Searches database (using LIKE/regular expressions) for any file/folder
      * names matching the query. Special characters are removed and replaced
      * with "wildcards" for the search.
      * @param query The string that the user typed into the search interface/UI
      * @return List of FileItem objects with names matching the query
      */
     public List<FileItem> getSearchResults(String query){
          //shouldn't need the processFile in DriveScanner, since this only searches the database, doesn't actually scan drives (ds.processFile(file) would NOT work)

          //Replaces all characters other than -,_, and numbers/letters with % and adds % at beginning and end to use LIKE in SQL. \w class matches all a-zA-Z0-9 and _, so only need to add -.
          String queryForLike = "%" + query.replaceAll("[^-\\w]+", "%") + "%"; //tested using jshell String query = "this is  a++*# sIlLy^tEsT's_tEsT- string ";
          ArrayList<FileItem> resultFileList = new ArrayList<>(); //must initialize, avoid null pointer, & List = only an interface!
          
          try {
               //should be case insensitive by default. Could also sort by driveID, path instead (name seems a bit easier to use?)
               PreparedStatement prepdSearchStmt = connection.prepareStatement("SELECT * from `fileItem` WHERE name LIKE ?  ORDER BY `driveID`,`name` ASC;");
               prepdSearchStmt.setString(1, queryForLike);
               ResultSet searchResults = prepdSearchStmt.executeQuery();

               while (searchResults.next()) {
               //assuming the Files table has columns int fileIdwithinDrive, String name, String path, boolean isFolder, int driveID, long size, int parentID in that order i.e. column index 1-7 (could use String column names, but this supposedly more efficient)
                    FileItem itemFromRow = new FileItem(searchResults.getInt(1), searchResults.getString(2), searchResults.getString(3), searchResults.getBoolean(4), searchResults.getInt(5), searchResults.getLong(6), searchResults.getInt(7));
                    //strange behavior if boolean value NULL in SQL shouldn't be an issue b/c not allowed to be null in this DB.

                    resultFileList.add(itemFromRow);
               }
               searchResults.close();
               prepdSearchStmt.close();
          } catch (SQLException sqle)
          {
               UIController.displayDataMessage("Error: Search has failed");
               sqle.printStackTrace(System.err);
          }
          
          return resultFileList;
     }
}