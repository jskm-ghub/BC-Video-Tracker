import java.sql.*;
import java.util.*;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.util.Properties;


/**
 * Populate DB with test data
 * 
 */
public class DBManager{
     private Session session;
     private static final String HOST = "bc-marketingvideotracker.benedictine.edu";
     private static final String USER = "nico3528";
     private static final String PASSWORD = "ZanLuc2117729?";

     Connection connection;
     String dbURL;
     //DriveScanner ds;

     public boolean connect() throws Exception{
          if (session != null && session.isConnected()) return true; // already up
          String remoteHost = "127.0.0.1"; // MySQL host from server perspective
          int remotePort = 22;           // MySQL port on server
          int localPort = 3307;            // Local port for SSH tunnel

     
          JSch jsch = new JSch();
          session = jsch.getSession(USER, HOST, remotePort);
          session.setPassword(PASSWORD); // or use key auth

          Properties config = new Properties();
          // For development; in production you’d handle host keys properly
          config.put("StrictHostKeyChecking", "no");
          session.setConfig(config);

          System.out.println("Connecting SSH...");
          session.connect();
          System.out.println("SSH connected.");

          int assignedPort = session.setPortForwardingL(localPort, remoteHost, remotePort);
          System.out.println("SSH tunnel established on localhost: " + assignedPort);
          return true;
     }

     public List<Drive> getDrives(){
          return null;
     }

     public String insertDrive(Drive drive){
          return null;
     }

     public void closeConnection() throws SQLException{
          try{
               if(connection != null && !connection.isClosed()){
                    connection.close();
               }
          }catch(SQLException e){
               e.printStackTrace();
          }
     }

     public List<FileItem> getFiles(Drive d, FileItem f){
          return null;
     }

     public List<FileItem> getFiles(Drive d){
          return null;
     }

     public List<FileItem> getSearchResults(String query){
          return null;
     }
}