/* DB for video tracker, based on the CSCI-3100 things from before
 */

-- Note that "schema" is the same as "database" !!!
CREATE DATABASE IF NOT EXISTS `videoschema_db` ;

USE `videoschema_db` ;

-- The 2 table creation queries in here tested/ran; and worked!
DROP TABLE IF EXISTS `drive`; -- Line extraneous unless the schema is not dropped
CREATE TABLE `drive` (
  `driveID` int NOT NULL AUTO_INCREMENT,
  `driveSerialName` varchar(255) NOT NULL,
  `driveDisplayName` varchar(255),
  PRIMARY KEY (`driveID`)
);


-- file_item references drive in the driveID field, parentID references this same table
-- table has columns that enable int fileID, String name, String path, boolean isFolder, int driveID, long size, int parentID in that order
DROP TABLE IF EXISTS `fileItem`;

CREATE TABLE `fileItem` (
  `fileID` INT NOT NULL AUTO_INCREMENT,
  `name` varchar(1023) NOT NULL,
  `path` varchar(2047) NOT NULL,
  `isFolder` boolean not null,
  `driveID` int NOT NULL,
  `size` BIGINT,
  `parentID` int NOT NULL,
  PRIMARY KEY (`fileID`),

  CONSTRAINT `fKeyDriveID`
  FOREIGN KEY (`driveID`) REFERENCES `drive` (`driveID`)
  ON UPDATE CASCADE ON DELETE NO ACTION,
  CONSTRAINT `fKeySameTableParentID`
  FOREIGN KEY (`parentID`) REFERENCES `fileItem` (`fileID`)
  ON UPDATE CASCADE ON DELETE NO ACTION
);
-- I choose NO ACTION b/c I don't want issues with files getting "deleted" when things are moved for some weird reason (though shouldn't be an issue);


/*
-- Could add some test data to this file, similar to the example below from a previous database assignment

INSERT INTO Grade (STUDENT_ID, ASSIGNMENT, GRADE) values
 (13, 1, 'A'), (13, 2, 'A'), (13, 3, 'A'), (13, 4, 'A'), (13, 5, 'A'),
 (13, 6, 'A'), (13, 7, 'A'), (13, 8, 'A'), (13, 9, 'A'), (13, 10, 'A'),
 (14, 1, 'A'), (14, 2, 'A'), (14, 3, 'A'), (14, 4, 'A'), (14, 5, 'A'),
 (14, 6, 'A'), (14, 7, 'A'), (14, 8, 'A'), (14, 9, 'A'), (14, 10, 'A'),
 (15, 1, 'A'), (15, 2, 'A'), (15, 3, 'A'), (15, 4, 'A'), (15, 5, 'A'),
 (15, 6, 'A'), (15, 7, 'A'), (15, 8, 'A'), (15, 9, 'A'), (15, 10, 'A'),
 (16, 1, 'A'), (16, 2, 'A'), (16, 3, 'A'), (16, 4, 'A'), (16, 5, 'A'),
 (16, 6, 'A'), (16, 7, 'A'), (16, 8, 'A'), (16, 9, 'A'), (16, 10, 'A')
 ;
 */
 