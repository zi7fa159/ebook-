#!/bin/bash
set -e

SDK_PATH="/opt/android-sdk"
BUILD_TOOLS="$SDK_PATH/build-tools/34.0.0"
PLATFORM="$SDK_PATH/platforms/android-34"

echo "=== Building Resources ==="
rm -rf build/obj build/apk build/gen build/compiled_res
mkdir -p build/obj build/apk build/gen build/compiled_res

$BUILD_TOOLS/aapt2 compile --dir app/src/main/res -o build/compiled_res/
$BUILD_TOOLS/aapt2 link -o build/apk/resources.ap_ \
    -I $PLATFORM/android.jar \
    --manifest app/src/main/AndroidManifest.xml \
    --java build/gen \
    --auto-add-overlay \
    build/compiled_res/*.flat

echo "=== Compiling Java ==="
javac -source 8 -target 8 -g:none -d build/obj \
    -classpath $PLATFORM/android.jar \
    -sourcepath app/src/main/java:build/gen \
    app/src/main/java/com/example/musicremover/*.java \
    build/gen/com/example/musicremover/R.java

echo "=== Dexing Classes ==="
$BUILD_TOOLS/d8 --output build/apk/ --lib $PLATFORM/android.jar $(find build/obj -name "*.class")

echo "=== Packaging APK ==="
cd build/apk
zip -u resources.ap_ classes.dex
cd ../..

echo "=== Zipalign APK ==="
$BUILD_TOOLS/zipalign -f -p 4 build/apk/resources.ap_ build/MusicRemover-unaligned.apk

echo "=== Signing APK ==="
$BUILD_TOOLS/apksigner sign --ks build/debug.keystore --ks-pass pass:android --out MusicRemover.apk build/MusicRemover-unaligned.apk

echo "=== Build Complete: MusicRemover.apk ==="
ls -lh MusicRemover.apk
