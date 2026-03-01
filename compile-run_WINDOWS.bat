@echo off

:: to start clean, this deletes the build directory
:: since the image files are not being saved this will take a little longer than the sh script
rmdir /s /q build 2>nul

:: creates the build directory
mkdir build
mkdir build\images

:: compiles java files into build folder
javac -cp "src/lib/mysql-connector-j-8.0.33.jar" -d build src\java\*.java

:: copy images and credential file into build directory
robocopy src\images build\images /E >nul
copy /Y src\lib\EncryptedCredentials.txt build\

:: unpackages the external library into the build directory
jar xf src\lib\mysql-connector-j-8.0.33.jar -C build

:: builds the final jar file
jar cfm VideoTrackerApplication.jar MANIFEST.MF -C build .

:: runs the application
java -jar ./VideoTrackerApplication.jar