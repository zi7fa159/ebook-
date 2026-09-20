#!/usr/bin/env bash
set -e

echo "=== Building MusicRemover.apk ==="

BUILD_TOOLS="/opt/android-sdk/build-tools/34.0.0"
PLATFORM="/opt/android-sdk/platforms/android-34/android.jar"

# Cleanup
rm -rf build app-aligned.apk app-unaligned.apk MusicRemover.apk
mkdir -p build/gen build/obj build/res_compiled

# Generate keystore if not existing
if [ ! -f debug.keystore ]; then
    echo "Generating debug keystore..."
    keytool -genkeypair -v -keystore debug.keystore -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android \
        -dname "CN=Android Debug,O=Android,C=US"
fi

# 1. Compile resources
$BUILD_TOOLS/aapt2 compile --dir res -o build/res_compiled.zip

# 2. Link resources and generate R.java
$BUILD_TOOLS/aapt2 link -o build/app-unaligned.apk \
    -I $PLATFORM \
    --manifest AndroidManifest.xml \
    build/res_compiled.zip \
    --java build/gen

# 3. Compile Java sources
javac -source 1.8 -target 1.8 -g -cp $PLATFORM:build/gen \
    -d build/obj \
    $(find src build/gen -name "*.java")

# 4. Dexing with d8
$BUILD_TOOLS/d8 --lib $PLATFORM \
    --output build/ \
    $(find build/obj -name "*.class")

# 5. Add classes.dex to APK
zip -j build/app-unaligned.apk build/classes.dex

# 6. Zipalign
$BUILD_TOOLS/zipalign -f 4 build/app-unaligned.apk build/app-aligned.apk

# 7. Sign APK
$BUILD_TOOLS/apksigner sign --ks debug.keystore \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out MusicRemover.apk \
    build/app-aligned.apk

echo "=== Build Successful! Output: MusicRemover.apk ==="
ls -lh MusicRemover.apk
