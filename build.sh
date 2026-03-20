#!/bin/bash
set -e

# Search for platform tools and jars
BUILD_TOOLS_DIR=$(ls -d /opt/android-sdk/build-tools/*/ | tail -1)
PLATFORM_JAR=$(ls -d /opt/android-sdk/platforms/android-*/android.jar | tail -1)
D8_BIN=$(which d8 || find /opt/android-sdk/cmdline-tools/latest/bin/ -name d8 | head -1)

AAPT2="$BUILD_TOOLS_DIR/aapt2"
ZIPALIGN="$BUILD_TOOLS_DIR/zipalign"
APKSIGNER="$BUILD_TOOLS_DIR/apksigner"

echo "Using Build Tools: $BUILD_TOOLS_DIR"
echo "Using Platform JAR: $PLATFORM_JAR"

# Clean
rm -rf out classes.dex base.apk aligned.apk final.apk compiled_res.zip
mkdir -p out

echo "Compiling resources..."
$AAPT2 compile --dir res -o compiled_res.zip

echo "Linking APK..."
$AAPT2 link -o base.apk -I $PLATFORM_JAR \
  --manifest AndroidManifest.xml \
  --java src \
  compiled_res.zip

echo "Compiling Java..."
javac -source 11 -target 11 -d out -cp $PLATFORM_JAR $(find src -name "*.java")

echo "Dexing..."
$D8_BIN out/com/example/astroapp/*.class --output . --lib $PLATFORM_JAR

echo "Adding classes to APK..."
zip -u base.apk classes.dex

echo "Aligning..."
$ZIPALIGN -v 4 base.apk aligned.apk

# Generate key if not exists (for build only)
if [ ! -f my-release-key.jks ]; then
  echo "Generating temporary build key..."
  keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias -storepass password -keypass password -dname "CN=AstroApp, O=Example, L=City, S=State, C=US"
fi

echo "Signing..."
$APKSIGNER sign --ks my-release-key.jks --ks-key-alias my-alias \
  --ks-pass pass:password --key-pass pass:password \
  --out final.apk aligned.apk

echo "Build complete: final.apk"
