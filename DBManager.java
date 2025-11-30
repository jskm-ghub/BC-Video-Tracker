// Java Libraries
import java.sql.*;
import java.util.*;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

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
     String dbName = "test_db"; // Database being used
     String dbUser = "root"; // ssh: mysql -u root -p
     String dbPassword = "Benedictine";

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
      * Establishes an SSH tunnel to the remote database server
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
               //System.out.println("SSH tunnel established on localhost: " + assignedPort);
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
               PreparedStatement stmt = connection.prepareStatement(sql);
               ResultSet rs = stmt.executeQuery();

               while(rs.next()){
                    int id = rs.getInt("id");
                    String driveName = rs.getString("driveName");
                    String serialName = rs.getString("serialName");

                    // Drive d = new Drive(driveName, serialName);
                    // drives.add(d);
                    System.out.println("ID: " + id + " | Name: " + driveName + " | Serial: " + serialName);
               }
          }catch(Exception e){
               e.printStackTrace();
          }
          return drives;
     }

     /**
      * Inserts a drive into the database
      * Data is retrieved from the Drive object passed in
      * @param drive Drive object to insert
      * @return String confirmation message
      */
     public String insertDrive(Drive drive) {
          int rows = 1;
          try{
               int id = 1;
               String driveName = drive.getDisplayName();
               String serialName = drive.getSerialName();
               String sql = "INSERT INTO drive (id, driveName, serialName) VALUES (?, ?, ?)";
               PreparedStatement stmt = connection.prepareStatement(sql);

               stmt.setInt(1, id);
               stmt.setString(2, driveName);
               stmt.setString(3, serialName);

               rows = stmt.executeUpdate();
          }catch(Exception e){
               e.printStackTrace();
          }
          return "Inserted rows: " + rows;
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
               String sql = "SELECT * FROM fileitem WHERE driveId = ? AND parentId = ?";
               PreparedStatement stmt = connection.prepareStatement(sql);
               stmt.setInt(1, d.getId());
               stmt.setInt(2, f.getId());
               ResultSet rs = stmt.executeQuery();

               while(rs.next()){
                    int id = rs.getInt("id");
                    String fileName = rs.getString("fileName");
                    String filePath = rs.getString("filePath");
                    int parentId = rs.getInt("parentId");
                    int driveId = rs.getInt("driveId");

                    FileItem fileItem = new FileItem(fileName, filePath, parentId, driveId);
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
               String sql = "SELECT * FROM fileitem WHERE driveId = ?";
               PreparedStatement stmt = connection.prepareStatement(sql);
               stmt.setInt(1, d.getId());
               ResultSet rs = stmt.executeQuery();

               while(rs.next()){
                    int id = rs.getInt("id");
                    String fileName = rs.getString("fileName");
                    String filePath = rs.getString("filePath");
                    int parentId = rs.getInt("parentId");
                    int driveId = rs.getInt("driveId");

                    FileItem fileItem = new FileItem(fileName, filePath, parentId, driveId);
                    files.add(fileItem);
               }
          
          return files;
     }

     public List<FileItem> getSearchResults(String query){
          return null;
     }
}