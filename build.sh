#!/bin/bash
set -e

# Setup paths
export ANDROID_HOME=/opt/android-sdk
export PLATFORM=$ANDROID_HOME/platforms/android-34/android.jar
export BUILD_TOOLS=$ANDROID_HOME/build-tools/34.0.0
export D8_PATH=$ANDROID_HOME/cmdline-tools/latest/bin/d8
export AAPT2=$BUILD_TOOLS/aapt2
export ZIPALIGN=$BUILD_TOOLS/zipalign
export APKSIGNER=$BUILD_TOOLS/apksigner

# Cleanup
rm -rf out compiled_res.zip base.apk aligned.apk final.apk base_with_dex.apk
mkdir -p out/src

echo "1. Compiling resources..."
$AAPT2 compile --dir res -o compiled_res.zip

echo "2. Linking resources and generating R.java..."
$AAPT2 link -o base.apk -I $PLATFORM --manifest AndroidManifest.xml \
  --java out/src \
  compiled_res.zip

echo "3. Compiling Java source..."
javac -source 11 -target 11 -d out \
  -cp $PLATFORM \
  $(find out/src -name "*.java") \
  $(find src -name "*.java")

echo "4. Converting to DEX..."
$D8_PATH out/com/astroapp/*.class --output out/ --lib $PLATFORM

echo "5. Adding DEX to APK..."
cp base.apk base_with_dex.apk
# Note: we need to use 'zip' carefully, or use aapt again
# 'zip -uj' puts files in the root of the zip, which is what we want for classes.dex
cd out && zip -uj ../base_with_dex.apk classes.dex && cd ..

echo "6. Aligning APK..."
$ZIPALIGN -f -v 4 base_with_dex.apk aligned.apk

echo "7. Signing APK..."
$APKSIGNER sign --ks my-release-key.jks --ks-pass pass:password --out final.apk aligned.apk

echo "Build complete: final.apk"
