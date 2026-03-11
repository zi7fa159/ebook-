# ALS Android Clone (Astro Live Stacker)

A 1:1 Android clone of Astro Live Stacker (ALS) for the Realme 8i (RMX3151), providing live RAW stacking with full manual camera control.

## Features
- Full 50MP RAW capture (DNG) using Camera2 API.
- Live stacking with Mean, Sum, and Sigma Clipping methods.
- Translation-only star alignment (ALS-style) on full 50MP frames.
- Manual control over Exposure (1s - 32s), ISO (100 - 6400), and Focus.
- Real-time preview with auto-scaling and square-root stretch for star visibility.
- Manual Color Calibration (R/G/B gains and Black Level) to eliminate sensor tint.
- Lossless output: 16-bit Color TIFF, full-res PNG, and exact sensor RAW (DNG).

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
