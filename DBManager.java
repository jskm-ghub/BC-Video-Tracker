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
      * @throws Exception
      */
     public boolean connect() throws Exception{
          startSession();
          connectSFTP();
          connectDB();

          return true;
     }
     
     /**
      * Establishes an SSH tunnel to the remote database server
      * Session is stored in the 'session' field
      * @throws Exception
      */
     private void startSession() throws Exception {
          if (session != null && session.isConnected()) return; // already up
          
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
     }

     /**
      * Establishes SFTP connection to the remote server
      * Channel is stored in the 'channelSftp' field
      * Could be of possible use for file stuff in future, 
      * Not required for SSH or DB connection, may later remove
      * @throws Exception
      */
     private void connectSFTP() throws Exception{
          // Secured File Transfer Protocol (SFTP) channel
          channelSftp = (ChannelSftp) session.openChannel("sftp");
          channelSftp.connect();
     }

     /**
      * Establishes a connection to the database through the SSH tunnel
      * Connection is stored in the 'connection' field
      * Currently set to use root user
      * @throws SQLException
      */
     private void connectDB() throws SQLException {
          if (connection != null && !connection.isClosed()) return; // already connected

          System.out.println("Connecting to database...");
          String dburl = "jdbc:mysql://127.0.0.1:" + localForwardPort + "/" + dbName
               + "?useSSL=false"
               + "&connectTimeout=5000"
               + "&socketTimeout=5000";

          try {
               connection = DriverManager.getConnection(dburl, dbUser, dbPassword);
               System.out.println("Database connected: " + (connection != null && !connection.isClosed()));
          } catch (SQLException e) {
               System.out.println("SQLState: " + e.getSQLState());
               System.out.println("ErrorCode: " + e.getErrorCode());
               System.out.println("Message: " + e.getMessage());
               e.printStackTrace();
          }
     }

     public List<Drive> getDrives(){
          return null;
     }

     /**
      * Inserts a drive into the database
      * Data is retrieved from the Drive object passed in
      * @param drive Drive object to insert
      * @return String confirmation message
      * @throws SQLException
      */
     public String insertDrive(Drive drive) throws SQLException {
          int id = 1;
          String driveName = drive.getDisplayName();
          String serialName = drive.getSerialName();
          String sql = "INSERT INTO drive (id, driveName, serialName) VALUES (?, ?, ?)";
          PreparedStatement stmt = connection.prepareStatement(sql);

          stmt.setString(1, id);
          stmt.setString(2, driveName);
          stmt.setString(3, serialName);

          int rows = stmt.executeUpdate();
          
          return "Inserted rows: " + rows;
     }

     public void closeConnection() throws Exception{
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

     public List<FileItem> getFiles(Drive d, FileItem f){
          return null;
     }

     public List<FileItem> getFiles(Drive d) throws SQLException{
          try{

          }catch(Exception e){
               e.printStackTrace();
          }
          return null;
     }

     public List<FileItem> getSearchResults(String query){
          return null;
     }
}