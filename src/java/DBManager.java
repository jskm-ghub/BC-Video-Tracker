// Java Libraries
import java.sql.*;
import java.util.*;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

// Encryption
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
import javax.json.Json;
import javax.json.stream.JsonParser;


public class DBManager{
          
     // Database details, retrieved from encrypted file
     int dbPort = 3306;
     String decryptedJson = "";
     String host = "";
     String dbName = "";
     String dbUser = "";
     String dbPass = "";
     
     Connection connection; //JDBC connection to MySQL database
     DriveScanner ds;

     /**
      * Constructor
      */
     public DBManager(DriveScanner scanner)
     {
          /* Ignore unless re-encryption is necessary; Encrypts ingored credentials.json file */
          // Encryptor.encryptFile("src/secure/credentials.json","src/secure/EncryptedCredentials.enc","BCRAVENS12345678");

          this.ds = scanner;
          decryptedJson = decryptFile("src/secure/EncryptedCredentials.enc","BCRAVENS12345678");
          parseJson(decryptedJson);
          System.out.println("Credentials decrypted and parsed successfully.");
          connectDatabase();
     }
     
     /**
      * Connects directly to MySQL database through port 3306.
      * Uses credentials previously decrypted
      */
     private void connectDatabase(){
          String dburl = "jdbc:mysql://" + host + ":" + dbPort + "/" + dbName
                    + "?useSSL=true"
                    + "&requireSSL=true"
                    + "&allowPublicKeyRetrieval=true"
                    + "&rewriteBatchedStatements=true"
                    + "&connectTimeout=5000"
                    + "&socketTimeout=5000";
          try{
               System.out.println("Connecting to database...");
               connection = DriverManager.getConnection(dburl, dbUser, dbPass);
               if(connection != null){
                    System.out.println("Database connected successfully.");
               }else{
                    System.out.println("Failed to connect to database.");
               }
          }catch(Exception e){
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
                    drives.add(d);
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
                    rows = stmt.executeUpdate();
               }
          }catch(Exception e)
          {
               e.printStackTrace();
          }
          drive.setDriveID(driveId);

          /* Scan Drive for files and add to database Individually */
          /* For testing, comment lines 178 through 180 */
          List<FileItem> files = ds.scan(drive);
          for (FileItem file : files)
          {
               System.out.println(file.toString());
               insertFile(file);
          }
          return "Inserted rows: " + rows;

          /* Insert files as a batch */
          /* For testing, uncomment lines below and comment lines above */
          // List<FileItem> files = ds.scan(drive);
          // batchInsertFiles(files);
          // return "Batch Inserted;";
          
     }

     private void batchInsertFiles(List<FileItem> files){
          String sql = "";
          PreparedStatement batchInsertStmt = null;
          for(FileItem file : files){
               try{
                    if(file.getParentID() == -1){ //no parent
                         sql = "INSERT INTO fileItem (fileIdWithinDrive, name, path, isFolder, driveID, size) VALUES (?, ?, ?, ?, ?, ?)";
                         batchInsertStmt = connection.prepareStatement(sql);
                         batchInsertStmt.setInt(1, file.getFileID());
                         batchInsertStmt.setString(2, file.getName());
                         batchInsertStmt.setString(3, file.getPath());
                         batchInsertStmt.setBoolean(4, file.isFolder());
                         batchInsertStmt.setInt(5, file.getDriveID());
                         batchInsertStmt.setLong(6, file.getSize());
                    }else{ //has parent
                         sql = "INSERT INTO fileItem (fileIdWithinDrive, name, path, isFolder, driveID, size, parentID) VALUES (?, ?, ?, ?, ?, ?, ?)";
                         batchInsertStmt = connection.prepareStatement(sql);
                         batchInsertStmt.setInt(1, file.getFileID());
                         batchInsertStmt.setString(2, file.getName());
                         batchInsertStmt.setString(3, file.getPath());
                         batchInsertStmt.setBoolean(4, file.isFolder());
                         batchInsertStmt.setInt(5, file.getDriveID());
                         batchInsertStmt.setLong(6, file.getSize());
                         batchInsertStmt.setInt(7, file.getParentID());
                    }
                    batchInsertStmt.addBatch();
               }catch(Exception e){
                    e.printStackTrace();
               }
          }
          try{
               batchInsertStmt.executeBatch();
               connection.commit();
          }catch(Exception e){
               try{
                    connection.rollback();
               }catch(Exception ex){
                    ex.printStackTrace();
               }
               e.printStackTrace();
          }finally{
               try{
                    connection.setAutoCommit(true); // TODO: you never set it to false, which i think you intended to, fyi
               }catch(Exception ex){
                    ex.printStackTrace();
               }
          }
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

     /**
      * Decrypts login credentials for server and MySQL
      * @param filePath relative path of .enc file
      * @param secret key used in encryption of file
      * @return json string of credentials
      */
     private static String decryptFile(String filePath, String secret) {
          byte[] decryptedBytes = new byte[0];
          try{
               byte[] keyBytes = secret.getBytes();
               SecretKey key = new SecretKeySpec(keyBytes, "AES");    
               Cipher cipher = Cipher.getInstance("AES");
     
               cipher.init(Cipher.DECRYPT_MODE, key);  
               byte[] encryptedBytes = Base64.getDecoder().decode(
                       Files.readAllBytes(Paths.get(filePath))); 
               decryptedBytes = cipher.doFinal(encryptedBytes);     
          }catch(Exception e){
               e.printStackTrace();
          }
          return new String(decryptedBytes);
     }

     /**
      * Parses through Json string of login credentials.
      * Assigns values to global variables.
      * @param json String of Json values
      */
     private void parseJson(String json){
          JsonParser parser = Json.createParser(new StringReader(json));
          String currentKey = "";
          String value = "";
          try{
               while(parser.hasNext()){
                    JsonParser.Event event = parser.next();
                    switch (event) {

                         case KEY_NAME:
                              currentKey = parser.getString();
                              break;     
                         case VALUE_STRING:
                              value = parser.getString();

                         switch (currentKey) {
                              case "host":
                                   host = value;
                                   break;
                              case "dbName":
                                   dbName = value;
                                   break;
                              case "dbUser":
                                   dbUser = value;
                                   break;
                              case "dbPass":
                                   dbPass = value;
                                   break;
                         }
                         break;
                         default:
                         break;
                    }
               }
          }catch(Exception e){
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
          } catch (SQLException sqle) {
               sqle.printStackTrace(System.err);
          }
          
          return resultFileList;
     }
}