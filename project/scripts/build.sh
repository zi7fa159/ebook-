#!/bin/bash
set -e

# Use ANDROID_HOME environment variable if set, otherwise default to a common path
SDK_PATH="${ANDROID_HOME:-/opt/android-sdk}"

# Find build tools version
BUILD_TOOLS_VERSION=$(ls "$SDK_PATH/build-tools" | sort -V | tail -n 1)
BUILD_TOOLS="$SDK_PATH/build-tools/$BUILD_TOOLS_VERSION"

# Find platform version
PLATFORM_VERSION=$(ls "$SDK_PATH/platforms" | sort -V | tail -n 1)
PLATFORM="$SDK_PATH/platforms/$PLATFORM_VERSION"

AAPT2="$BUILD_TOOLS/aapt2"
# d8 is often in cmdline-tools or build-tools
if [ -f "$SDK_PATH/cmdline-tools/latest/bin/d8" ]; then
    D8="$SDK_PATH/cmdline-tools/latest/bin/d8"
else
    D8="$BUILD_TOOLS/d8"
fi
ZIPALIGN="$BUILD_TOOLS/zipalign"
APKSIGNER="$BUILD_TOOLS/apksigner"
JAVAC="javac"

PROJECT_ROOT=$(pwd)/project
SRC="$PROJECT_ROOT/src"
RES="$PROJECT_ROOT/res"
MANIFEST="$PROJECT_ROOT/AndroidManifest.xml"
BIN="$PROJECT_ROOT/bin"
OBJ="$PROJECT_ROOT/obj"
GEN="$PROJECT_ROOT/gen"

mkdir -p "$BIN" "$OBJ" "$GEN"

echo "--- Compiling Resources ---"
"$AAPT2" compile --dir "$RES" -o "$OBJ/res.zip"

echo "--- Linking Resources ---"
"$AAPT2" link --manifest "$MANIFEST" \
    -I "$PLATFORM/android.jar" \
    "$OBJ/res.zip" \
    -o "$BIN/app.unaligned.apk" \
    --java "$GEN" \
    --auto-add-overlay

echo "--- Compiling Java Source ---"
"$JAVAC" -source 11 -target 11 \
    -classpath "$PLATFORM/android.jar" \
    -d "$OBJ" \
    "$SRC"/*.java "$GEN"/com/example/astrostacker/R.java

echo "--- Converting to DEX ---"
# Find all .class files and pass them individually to d8
CLASS_FILES=$(find "$OBJ" -name "*.class")
"$D8" --release --output "$BIN" \
    --lib "$PLATFORM/android.jar" \
    $CLASS_FILES

echo "--- Packaging APK ---"
pushd "$BIN"
zip -u app.unaligned.apk classes.dex
popd

echo "--- Aligning APK ---"
"$ZIPALIGN" -f 4 "$BIN/app.unaligned.apk" "$BIN/app.aligned.apk"

echo "--- Signing APK ---"
# Generate debug key if not exists
if [ ! -f debug.keystore ]; then
    keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
fi

"$APKSIGNER" sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out "$BIN/app.apk" "$BIN/app.aligned.apk"

echo "Done! APK is at $BIN/app.apk"
