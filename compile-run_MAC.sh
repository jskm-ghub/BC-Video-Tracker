#!/bin/bash
mkdir -p build/fat/images
javac -cp "src/lib/jsch-0.1.55.jar:src/lib/mysql-connector-j-8.0.33.jar" -d src/out src/java/*.java
jar cf build/tempclasses.jar -C src/out .

if [ ! -f build/fat/com/jcraft/jsch/JSch.class ]; then
    jar xf src/lib/jsch-0.1.55.jar -C build/fat
fi

if [ ! -f build/fat/com/mysql/cj/jdbc/Driver.class ]; then
    jar xf src/lib/mysql-connector-j-8.0.33.jar -C build/fat
fi

jar xf build/tempclasses.jar -C build/fat
rsync -a --ignore-existing src/images/ build/fat/images/
jar cfm VideoTrackerApplication.jar MANIFEST.MF -C build/fat .
java -jar ./VideoTrackerApplication.jar