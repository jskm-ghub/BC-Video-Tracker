/* DB for video tracker, based on earlier database sql files for format

NOTES: From server, run mysqldump -u root -p --column-statistics=0 test_db [or videoschema_db] to verify the current state of the DBs (prints to terminal). The flag --host=000.000.000.000 option may be needed for nonstandard configurations.
To export to a file from mysqldump, run e.g. mysqldump -u root -p --column-statistics=0 videoschema_db > [EXPORT_FILENAME].sql (with EXPORT_FILENAME set to correct file/directory).

Also, the `test_drive` table in videoschema_db is OUTDATED and should not be used as of 1/26/26!
 */

-- Note that "schema" is the same as "database" !!!
CREATE DATABASE IF NOT EXISTS `videoschema_db` ;

USE `videoschema_db` ;

-- The 2 table creation queries in here tested/ran; and worked!
DROP TABLE IF EXISTS `fileItem`;
DROP TABLE IF EXISTS `drive`; -- Line extraneous unless the schema is not dropped

CREATE TABLE `drive` (
  `driveID` int NOT NULL AUTO_INCREMENT,
  `driveSerialName` varchar(255) NOT NULL UNIQUE,
  `driveDisplayName` varchar(255),
  PRIMARY KEY (`driveID`)
);


-- file_item references drive in the driveID field, parentID references this same table
-- table has columns that enable int fileID, String name, String path, boolean isFolder, int driveID, long size, int parentID in that order

CREATE TABLE `fileItem` (
  `fileIdWithinDrive` INT NOT NULL,
  `name` varchar(1023) NOT NULL,
  `path` varchar(2047) NOT NULL,
  `isFolder` boolean NOT NULL,
  `driveID` int NOT NULL,
  `size` BIGINT,
  `parentID` int,
  PRIMARY KEY (`fileIdWithinDrive`, `driveID`),
  CONSTRAINT `fKeyDriveID`
  FOREIGN KEY (`driveID`) REFERENCES `drive` (`driveID`)
  ON UPDATE CASCADE ON DELETE CASCADE
);

-- Some test data for the "real" DB, examples taken from the "test_db" test data, as given by running mysqldump. (Could the auto-increment column be omitted safely?):
INSERT INTO `drive` VALUES (1,'1A','drive1'),(2,'2B','drive2'),(3,'3C','drive3'),(4,'4D','drive4'),(5,'5E','drive5');

INSERT INTO `fileItem` VALUES (1,'file1','/drive1/file1/',0,1,1028,NULL),(2,'file2','/drive1/file2/',0,1,1028,NULL),(3,'file3','/drive2/file3/',0,2,1028,NULL),(4,'file4','/drive2/file4/',0,2,1028,NULL),(5,'file5','/drive3/file5/',0,3,1028,NULL),(6,'file6','/drive3/file6/',0,3,1028,NULL),(7,'file7','/drive4/file7/',0,4,1028,NULL),(8,'file8','/drive4/file8/',0,4,1028,NULL),(9,'file9','/drive5/file9/',0,5,1028,NULL),(10,'file10','/drive5/file10/',0,5,1028,NULL),(101,'file101','/drive1/file1/file101/',0,1,256,1),(102,'file102','/drive1/file1/file102/',0,1,256,1),(201,'file201','/drive1/file2/file201/',0,1,256,2),(202,'file202','/drive1/file2/file202/',0,1,256,2);