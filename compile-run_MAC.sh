#!/bin/bash

# allow bash to use !() for the remove command
shopt -s extglob

# create the build folder cleanly, saving the images folder
mkdir -p build/images
rm -rf build/!(images)

# compile all java files in src/java and put them into build folder
# uses the mysql connector library as part of the classpath
javac -cp "src/lib/mysql-connector-j-8.0.33.jar" -d build src/java/*.java

# copies images and encrypted credentials into build folder
# only copies changes to images
rsync -a --delete src/images/ build/images/
cp src/lib/EncryptedCredentials.txt build

# copies the external jar library into the final jar
jar xf src/lib/mysql-connector-j-8.0.33.jar -C build

# builds the final jar
jar cfm VideoTrackerApplication.jar MANIFEST.MF -C build .

# sets the jar to be executable
chmod +x VideoTrackerApplication.jar

# runs the newly created jar file
java -jar ./VideoTrackerApplication.jar