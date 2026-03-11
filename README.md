# ALS Android Clone (Astro Live Stacker)

A 1:1 Android clone of Astro Live Stacker (ALS) for the Realme 8i (RMX3151), providing live RAW stacking with full manual camera control.

## Features
- Full 50MP RAW capture (DNG) using Camera2 API.
- Live stacking with incremental averaging in floating-point precision.
- Translation-only star alignment (ALS-style).
- Manual control over Exposure (1s - 32s) and ISO (100 - 6400).
- Real-time preview of the stacked result.
- Save final results as lossless PNG (expandable to TIFF/DNG).

## Build Requirements
- Android SDK Command-line Tools
- Java Development Kit (JDK) 11+
- `adb` for installation

## Environment Setup
Set `ANDROID_HOME` to your Android SDK path.
Example:
```bash
export ANDROID_HOME=/opt/android-sdk
```

## How to Build
Run the provided build script:
```bash
./scripts/build.sh
```
This script will:
1. Compile resources using `aapt2`.
2. Generate `R.java`.
3. Compile Java source using `javac`.
4. Convert classes to DEX using `d8`.
5. Package and align the APK using `zipalign`.
6. Sign the APK using `apksigner`.

The final APK will be located at `bin/app-signed.apk`.

## How to Install
Run the installation script:
```bash
./scripts/install.sh
```
This will install the APK to your connected device and launch it.

## Hardware Support
Optimized for Realme 8i (MediaTek Helio G96). Ensure the device is connected via ADB and has "USB Debugging" enabled.
