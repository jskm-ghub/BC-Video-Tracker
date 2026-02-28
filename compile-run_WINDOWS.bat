mkdir build 2>nul
mkdir build\fat 2>nul
mkdir build\fat\images 2>nul
javac -cp "src/lib/javax.json-1.1.4.jar;src/lib/javax.json-api-1.1.4.jar;src/lib/jsch-0.1.55.jar;src/lib/mysql-connector-j-8.0.33.jar" -d src/out src/java/*.java
jar cf build\tempclasses.jar -C src/out .

if not exist "build\fat\javax\json\Json.class" (
    jar xf src\lib\javax.json-api-1.1.4.jar -C build/fat
)

if not exist "build\fat\org\glassfish\json\JsonProviderImpl.class" (
    jar xf src\lib\javax.json-1.1.4.jar -C build/fat
)

if not exist "build\fat\com\jcraft\jsch\JSch.class" (
    jar xf src\lib\jsch-0.1.55.jar -C build/fat
)

if not exist "build\fat\com\mysql\cj\jdbc\Driver.class" (
    jar xf src\lib/mysql-connector-j-8.0.33.jar -C build/fat
)

jar xf build\tempclasses.jar -C build/fat
robocopy src\images build\fat\images /E /NFL /NDL /NJH /NJS /NC /NS /NP
copy /Y src\lib\EncryptedCredentials.txt build\fat\
jar cfm VideoTrackerApplication.jar MANIFEST.MF -C build/fat .
java -jar ./VideoTrackerApplication.jar