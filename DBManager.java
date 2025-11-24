import java.sql.*;
import java.util.*;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.ChannelSftp;

/**
 * Populate DB with test data
 * 
 */
public class DBManager{
     private static final String HOST = "bc-marketingvideotracker.benedictine.edu";
     private static final String USER = "nico3528";
     private static final String PASSWORD = "ZanLuc2117729?";

     Session session; // SSH session
     ChannelSftp channelSftp; // Connection to the server
     Connection connection; // Connection to the database
     //DriveScanner ds;

     public boolean connect() throws Exception{
          startSession();

          // Secured File Transfer Protocol (SFTP) channel
          channelSftp = (ChannelSftp) session.openChannel("sftp");
          channelSftp.connect();

          System.out.println("SFTP Channel created, reading test file:");
          String filePath = "/srv/test.txt";
          InputStream inputStream = channelSftp.get(filePath);
          BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
          String line;
          while ((line = reader.readLine()) != null) {
               System.out.println(line);
          }
          reader.close();

          closeConnection();
          return true;
     }
     
     /**
      * Establishes an SSH tunnel to the remote database server
      * Session is stored in the 'session' field
      * @throws Exception
      */
     private void startSession() throws Exception {
          if (session != null && session.isConnected()) return; // already up
          String remoteHost = "127.0.0.1"; // MySQL host from server perspective
          int remotePort = 22;           // MySQL port on server
          int localPort = 3307;            // Local port for SSH tunnel
          
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
          System.out.println("SSH connected.");

          int assignedPort = session.setPortForwardingL(localPort, remoteHost, remotePort);
          System.out.println("SSH tunnel established on localhost: " + assignedPort);
     }

     public List<Drive> getDrives(){
          return null;
     }

     public String insertDrive(Drive drive){
          return null;
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

     public List<FileItem> getFiles(Drive d){
          return null;
     }

     public List<FileItem> getSearchResults(String query){
          return null;
     }
}