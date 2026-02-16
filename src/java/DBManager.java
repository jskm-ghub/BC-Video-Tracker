// javac -cp "src/lib/*" -d out src/java/*.java
// java -cp "out:src/lib/*" MainApp
// ./compile-run_MAC.sh

// Java Libraries
import java.sql.*;
import java.util.*;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

//Encryption
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

// External Libraries
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.ChannelSftp;


public class DBManager{
     private static final String HOST = "bc-marketingvideotracker.benedictine.edu";
     private static final String USER = "nico3528";
     private static final String PASSWORD = "ZanLuc2117729?";

     // Server details
     String remoteHost = "127.0.0.1"; // MySQL host from server perspective
     int remotePort = 22;             // MySQL port on server
     int localForwardPort = 3307;     // Local port for SSH tunnel

     // Connection objects
     Session session;         // SSH session
     ChannelSftp channelSftp; // Connection to the server
     Connection connection;   // Connection to the database

     // Database details
     String dbName = "videoschema_db"; // Database being used videoschema_db or test_db
     String dbUser = "root"; // ssh: mysql -u root -p
     String dbPassword = "Benedictine";
     String decryptedJson = "";

     DriveScanner ds;
     //DBManager should handle calling scan() from DriveScanner, and handle list of files that is returned
     //insertFiles

     public DBManager(DriveScanner scanner)
     {
          this.ds = scanner;
          connect();
          decryptFile("/Users/lsnicotra/Desktop/BC-Video-Tracker/src/secure/EncryptedCredentials.enc",
               "BCRAVENS12345678");
     }

     /**
      * Calls all connection methods
      * @return true if connections are successful
      */
     public boolean connect(){
          startSession();
          connectSFTP();
          connectDB();

          return true;
     }
     
     /**
      * Establishes an SSH tunnel to the remote server
      * Session is stored in the 'session' field
      */
     private void startSession() {
          if (session != null && session.isConnected()) return; // already up
          
          try{
               // Set up JSch session and 'Log in'
               JSch jsch = new JSch();
               session = jsch.getSession(USER, HOST, remotePort);
               session.setPassword(PASSWORD);

               Properties config = new Properties();
               // For development; in production you’d handle host keys properly
               config.put("StrictHostKeyChecking", "no");
               session.setConfig(config);

               System.out.println("Connecting SSH...");
               session.connect();
               int assignedPort = session.setPortForwardingL(localForwardPort, remoteHost, 3306);
               System.out.println("SSH connected: " + session.isConnected());
          }catch(Exception e){
               e.printStackTrace();
          }
     }

     /**
      * Establishes SFTP connection to the remote server
      * Channel is stored in the 'channelSftp' field
      * Could be of possible use for file stuff in future, 
      * Not required for SSH or DB connection, may later remove
      */
     private void connectSFTP() {
          try{
               // Secured File Transfer Protocol (SFTP) channel
               channelSftp = (ChannelSftp) session.openChannel("sftp");
               channelSftp.connect();
          }catch(Exception e){
               e.printStackTrace();
          }
     }

     /**
      * Establishes a connection to the database through the SSH tunnel
      * Connection is stored in the 'connection' field
      * Currently set to use root user
      */
     private void connectDB() {
          try {
               if (connection != null && !connection.isClosed()) return; // already connected
               System.out.println("Connecting to database...");
               String dburl = "jdbc:mysql://127.0.0.1:" + localForwardPort + "/" + dbName
                    + "?useSSL=false"
                    + "&connectTimeout=5000"
                    + "&socketTimeout=5000";
               connection = DriverManager.getConnection(dburl, dbUser, dbPassword);
               System.out.println("Database connected: " + (connection != null && !connection.isClosed()));
          } catch (SQLException e) {
               System.out.println("SQLState: " + e.getSQLState());
               System.out.println("ErrorCode: " + e.getErrorCode());
               System.out.println("Message: " + e.getMessage());
               e.printStackTrace();
          }
     }

     /**
      * Retrieves a list of all drives from the database
      * @return List of Drive objects
      */
     public List<Drive> getDrives(){
          List<Drive> drives = new ArrayList<>();
          try{
               String sql = "SELECT * FROM drive";
               PreparedStatement query = connection.prepareStatement(sql);
               ResultSet rs = query.executeQuery();

               while(rs.next()){
                    int id = rs.getInt("driveID");
                    String serialName = rs.getString("driveSerialName");
                    String driveName = rs.getString("driveDisplayName");

                    Drive d = new Drive(serialName, driveName);
                    d.setDriveID(id);
                    // Drive d = new Drive(id, serialName, driveName); 
                    drives.add(d);
                    //System.out.println("ID: " + id + " | Name: " + driveName + " | Serial: " + serialName);
               }
          }catch(Exception e){
               e.printStackTrace();
          }
          return drives;
     }

     /**
      * Inserts a drive into the database if it doesn't already exist.
      * If drive already exists, wipes existing fileItems under that driveID and rescans drive for files to update database
      * Calls insertFile() for each file found on the drive to add to the database
      * 
      * @param drive Drive object to insert
      * @return String confirmation message
      */
     public String insertDrive(Drive drive) {
          /* check database for existing driveSerialName */
          String driveName = drive.getDisplayName();
          String serialName = drive.getSerialName();
          PreparedStatement stmt;
          String sql = "";
          ResultSet rs = null;
          int driveId = 0;
          int rows = 0;
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
                    rows = stmt.executeUpdate();
                    sql = "SELECT driveID FROM drive WHERE driveSerialName = '" + serialName + "';";
                    stmt = connection.prepareStatement(sql);
                    ResultSet rs2 = stmt.executeQuery();
                    driveId = 0;
                    if(rs2.next()){
                         driveId = rs2.getInt("driveID");
                    }
               }else{ //drive already exists, wipe existing fileItems under that driveID and rescan drive for files to update database
                    System.out.println("Drive with serial name " + serialName + " already exists in database.");
                    
                    sql = "SELECT driveID FROM drive WHERE driveSerialName = '" + serialName + "';";
                    stmt = connection.prepareStatement(sql);
                    ResultSet rs2 = stmt.executeQuery();
                    driveId = 0;
                    if(rs2.next()){
                         driveId = rs2.getInt("driveID");
                    }
                    sql = "DELETE FROM fileItem WHERE driveID = " + driveId + ";";
                    stmt = connection.prepareStatement(sql);
                    rows = stmt.executeUpdate();
               }
          }catch(Exception e){
               e.printStackTrace();
          }
          drive.setDriveID(driveId);

          /* Scan Drive for files and add to database */
          List<FileItem> files = ds.scan(drive);
          for (FileItem file : files) {
               System.out.println(file.toString());
               insertFile(file);
          }

          return "Inserted rows: " + rows;
     }

     /**
      * Inserts a single fileItem into the database.
      * Called by insertDrive()
      * @param file FileItem object to insert
      * @return Name of fileItem added
      */
     private String insertFile(FileItem file){
          /* Insert fileItem into the database */
          int rows =0;
          try{
               String sql = "";
               PreparedStatement stmt;
               System.out.println("Parent ID: " + file.getParentID());
               if(file.getParentID() == -1){ //no parent
                    sql = "INSERT INTO fileItem (fileIdWithinDrive, name, path, isFolder, driveID, size) VALUES (?, ?, ?, ?, ?, ?)";
                    stmt = connection.prepareStatement(sql);
                    stmt.setInt(1, file.getFileID());
                    stmt.setString(2, file.getName());
                    stmt.setString(3, file.getPath());
                    stmt.setBoolean(4, file.isFolder());
                    stmt.setInt(5, file.getDriveID());
                    stmt.setLong(6, file.getSize());
               }else{ //has parent
                    sql = "INSERT INTO fileItem (fileIdWithinDrive, name, path, isFolder, driveID, size, parentID) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    stmt = connection.prepareStatement(sql);
                    stmt.setInt(1, file.getFileID());
                    stmt.setString(2, file.getName());
                    stmt.setString(3, file.getPath());
                    stmt.setBoolean(4, file.isFolder());
                    stmt.setInt(5, file.getDriveID());
                    stmt.setLong(6, file.getSize());
                    stmt.setInt(7, file.getParentID());
               }
               rows = stmt.executeUpdate();
          }catch(Exception e){
               e.printStackTrace();
          }
          return "Inserted file: " + file.getName();
     }

     /**
      * Closes all open connections
      */
     public void closeConnection(){
          try{
               if(connection != null && !connection.isClosed()){
                    connection.close();
                    session.disconnect();
                    channelSftp.disconnect();
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
               System.out.println("Displaying files under parent ID: " + f.getFileID());
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

     public static String decryptFile(String filePath, String secret) {
          byte[] decryptedBytes = new byte[0];
          try{
               byte[] keyBytes = secret.getBytes();
               SecretKey key = new SecretKeySpec(keyBytes, "AES");    
               Cipher cipher = Cipher.getInstance("AES");
     
               cipher.init(Cipher.DECRYPT_MODE, key);  
               byte[] encryptedBytes = Base64.getDecoder().decode(
                       Files.readAllBytes(Paths.get(filePath))); 
               decryptedBytes = cipher.doFinal(encryptedBytes);
     
               System.out.println(new String(decryptedBytes));
          }catch(Exception e){
               e.printStackTrace();
          }
          return new String(decryptedBytes);
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
          } catch (SQLException sqle) {
               sqle.printStackTrace(System.err);
          }
          
          return resultFileList;
     }
}