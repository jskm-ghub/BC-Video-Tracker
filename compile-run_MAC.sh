#!/bin/bash
set -e
javac -cp src/lib/jsch-0.1.55.jar -d src/out src/java/*.java
jar cfm VideoTrackerApplication.jar MANIFEST.MF -C src/out . -C src/images .
java -jar ./VideoTrackerApplication.jar