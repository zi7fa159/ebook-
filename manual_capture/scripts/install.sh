#!/bin/bash
set -e

APK_PATH="manual_capture.apk"
PACKAGE_NAME="com.manual.capture"

# Change to manual_capture root
cd "$(dirname "$0")/.."

if [ ! -f "$APK_PATH" ]; then
    echo "APK not found: $APK_PATH. Run build.sh first."
    exit 1
fi

echo "Installing $APK_PATH..."
adb install -r "$APK_PATH"

echo "Launching $PACKAGE_NAME..."
adb shell am start -n "$PACKAGE_NAME/$PACKAGE_NAME.MainActivity"
