import java.sql.*;
import java.util.*;


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
          
          //assume connected already? CHECK FOR CORRECT NAMES also!
          try {
               //should be case insensitive by default
               PreparedStatement prepdSearchStmt = connection.prepareStatement("SELECT * from `fileItem` WHERE name LIKE ?;");
               prepdSearchStmt.setString(1, queryForLike);
               ResultSet searchResults = prepdSearchStmt.executeQuery();

               while (searchResults.next()) {
               //assuming the Files table has columns int fileID, String name, String path, boolean isFolder, int driveID, long size, int parentID in that order i.e. column index 1-7 (could use String column names, but this supposedly more efficient)
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