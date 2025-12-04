javac -cp src/lib/jsch-0.1.55.jar;/src/lib/mysql-connector-j-8.0.33.jar -d src/out src/java/*.java
jar cfm VideoTrackerApplication.jar MANIFEST.MF -C src/out . -C src/images .
java -jar ./VideoTrackerApplication.jar