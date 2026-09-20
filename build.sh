#!/bin/bash
set -e

SDK="/opt/android-sdk"
BUILD_TOOLS="$SDK/build-tools/34.0.0"
PLATFORM="$SDK/platforms/android-34/android.jar"

echo "=== Cleaning previous build artifacts ==="
rm -rf build/obj/* build/*.zip build/*.apk build/*.dex src/com/example/musicremover/R.java

echo "=== Compiling resources with AAPT2 ==="
$BUILD_TOOLS/aapt2 compile --dir res -o build/resources.zip
$BUILD_TOOLS/aapt2 link -I $PLATFORM \
    --manifest AndroidManifest.xml \
    --java src \
    -o build/unaligned.apk \
    build/resources.zip

echo "=== Compiling Java source files ==="
JAVA_FILES=$(find src -name "*.java" ! -name "DspTestRunner.java")
javac -g -source 8 -target 8 -bootclasspath $PLATFORM \
    -d build/obj \
    $JAVA_FILES

echo "=== Dexing with D8 ==="
CLASS_FILES=$(find build/obj -name "*.class")
$BUILD_TOOLS/d8 --lib $PLATFORM \
    --output build \
    $CLASS_FILES

echo "=== Adding classes.dex to APK ==="
cd build
zip -u unaligned.apk classes.dex
cd ..

echo "=== Zip aligning APK ==="
$BUILD_TOOLS/zipalign -f -p 4 build/unaligned.apk build/aligned.apk

echo "=== Signing APK ==="
if [ ! -f build/debug.keystore ]; then
    keytool -genkeypair -v -keystore build/debug.keystore \
        -alias androiddebugkey -storepass android -keypass android \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null
fi

$BUILD_TOOLS/apksigner sign --ks build/debug.keystore \
    --ks-pass pass:android --key-pass pass:android \
    --out MusicRemover.apk build/aligned.apk

echo "=== APK Successfully Built ==="
ls -lh MusicRemover.apk
