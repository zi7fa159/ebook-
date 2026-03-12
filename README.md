# A2LS (Astro Live Stacker for Android)

A native Android application for high-performance astrophotography, performing real-time RAW stacking and star alignment.

## Features
- Full resolution RAW capture (DNG) using Camera2 API.
- Live stacking with Mean, Sum, and Sigma Clipping methods.
- Translation-only star alignment on full-resolution frames.
- Manual control over Exposure, ISO, and Focus.
- Real-time preview with professional power-law (Gamma) stretch.
- Integrated Histogram with logarithmic visualization and manual stretch markers.
- Lossless output: 16-bit Color TIFF (Linear and Stretched), PNG, and exact sensor RAW (DNG).
- Fully native Java implementation with no external library dependencies.
- Build system designed for command-line use without Gradle or Android Studio.

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
Designed for Android devices with RAW_SENSOR support. While optimized for high-resolution sensors (like the 50MP sensor on Realme 8i), it is compatible with most modern Android devices providing manual camera control.
