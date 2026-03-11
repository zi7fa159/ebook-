#!/bin/bash
set -e

# Configuration
SDK_PATH="${ANDROID_HOME:-/opt/android-sdk}"
BUILD_TOOLS_VER="35.0.0"
PLATFORM_VER="34"

AAPT2="${SDK_PATH}/build-tools/${BUILD_TOOLS_VER}/aapt2"
D8="${SDK_PATH}/build-tools/${BUILD_TOOLS_VER}/d8"
ZIPALIGN="${SDK_PATH}/build-tools/${BUILD_TOOLS_VER}/zipalign"
APKSIGNER="${SDK_PATH}/build-tools/${BUILD_TOOLS_VER}/apksigner"
ANDROID_JAR="${SDK_PATH}/platforms/android-${PLATFORM_VER}/android.jar"

# Clean
rm -rf obj bin
mkdir -p obj bin

echo "Compiling resources..."
$AAPT2 compile --dir res -o obj/res.zip

echo "Linking resources..."
$AAPT2 link --manifest AndroidManifest.xml \
    -I "$ANDROID_JAR" \
    --java obj/src \
    -o bin/ProAstro.unaligned.apk \
    obj/res.zip

echo "Compiling Java source..."
javac -source 11 -target 11 -d obj -classpath "$ANDROID_JAR" $(find src -name "*.java") $(find obj/src -name "*.java")

echo "Converting to DEX..."
CLASSES=$(find obj -name "*.class")
$D8 --release --output bin --lib "$ANDROID_JAR" $CLASSES

echo "Adding DEX to APK..."
cd bin
jar uf ProAstro.unaligned.apk classes.dex
cd ..

echo "Aligning APK..."
$ZIPALIGN -f 4 bin/ProAstro.unaligned.apk bin/ProAstro.aligned.apk

echo "Signing APK..."
if [ ! -f debug.keystore ]; then
    keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi
$APKSIGNER sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out bin/ProAstro.apk bin/ProAstro.aligned.apk

echo "Build successful: bin/ProAstro.apk"
