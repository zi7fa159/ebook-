#!/bin/bash
set -e

# Configuration
SDK_PATH="${ANDROID_HOME:-/opt/android-sdk}"
BUILD_TOOLS_VERSION="34.0.0"
PLATFORM_VERSION="34"

AAPT2="${SDK_PATH}/build-tools/${BUILD_TOOLS_VERSION}/aapt2"
ZIPALIGN="${SDK_PATH}/build-tools/${BUILD_TOOLS_VERSION}/zipalign"
APKSIGNER="${SDK_PATH}/build-tools/${BUILD_TOOLS_VERSION}/apksigner"
D8="${SDK_PATH}/cmdline-tools/latest/bin/d8"
ANDROID_JAR="${SDK_PATH}/platforms/android-${PLATFORM_VERSION}/android.jar"

# Cleanup
rm -rf gen obj bin
mkdir -p gen obj bin

# 1. Compile resources
echo "Compiling resources..."
$AAPT2 compile --dir res -o bin/res.zip

# 2. Link resources and generate R.java
echo "Linking resources..."
$AAPT2 link -o bin/app.apk \
    -I $ANDROID_JAR \
    --manifest AndroidManifest.xml \
    --java gen \
    --auto-add-overlay \
    bin/res.zip

# 3. Compile Java source
echo "Compiling Java source..."
javac -source 11 -target 11 \
    -Xlint:-options \
    -cp $ANDROID_JAR \
    -d obj \
    gen/com/alsclone/astrostacker/R.java \
    src/*.java

# 4. Convert classes to DEX
echo "Converting to DEX..."
# Use find to get all .class files, including those in subdirectories
CLASS_FILES=$(find obj -name "*.class")
$D8 --release --output bin/classes.zip \
    --lib $ANDROID_JAR \
    $CLASS_FILES

# 5. Add DEX to APK
echo "Adding DEX to APK..."
unzip -p bin/classes.zip classes.dex > bin/classes.dex
cd bin
zip -u app.apk classes.dex
cd ..

# 6. Align APK
echo "Aligning APK..."
$ZIPALIGN -f 4 bin/app.apk bin/app-aligned.apk

# 7. Sign APK
echo "Signing APK..."
# Generate a debug key if it doesn't exist
if [ ! -f debug.keystore ]; then
    keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
fi

$APKSIGNER sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out bin/app-signed.apk bin/app-aligned.apk

echo "Build successful: bin/app-signed.apk"
