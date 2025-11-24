import java.sql.*;
import java.util.*;
import com.jcraft.jsch.*;


/**
 * Populate DB with test data
 * 
 */
public class DBManager{
     private static final String URL = "";
     private static final String USER = "";
     private static final String PASSWORD = "";

     Connection connection;
     String dbURL;
     //DriveScanner ds;

     public boolean connect() throws SQLException{
          return true;
          // connection = DriverManager.getConnection(URL, USER, PASSWORD);
          // return connection != null;
     }

     public List<Drive> getDrives(){
          return null;
     }

     public String insertDrive(Drive drive){
          return null;
     }

     public void closeConnection(){

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