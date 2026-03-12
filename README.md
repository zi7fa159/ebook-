# A2LS (Astro Live Stacker for Android)

A native Android application for high-performance astrophotography, performing real-time RAW stacking and star alignment.

## Features
- **Full 50MP RAW Capture**: Utilizes the maximum sensor resolution of the Realme 8i (RMX3151) for maximum detail.
- **Professional Camera Control**: Full manual overrides for Exposure (up to 32s), ISO (100-6400), and Focus (Infinite/Macro).
- **A2LS-Style Live Stacking**: Incremental averaging at 32-bit floating-point precision for noise reduction.
- **High-Quality Alignment**: Translation-only star alignment using 2D cross-correlation to preserve sharp star points.
- **Advanced Stacking Methods**:
    - **Mean Stacking**: Optimal for static scenes.
    - **Sum Stacking**: Maximizes signal for very faint deep-sky objects.
    - **Kappa-Sigma Clipping**: Advanced outlier rejection to eliminate satellites, planes, and cosmic rays (Sequator/DSS style).
- **Tune Studio**: Real-time post-processing engine featuring:
    - **Live Histogram**: Logarithmic visualization of data distribution.
    - **Dynamic Stretching**: Precise control over Black, Mid, and White points.
    - **Auto-Stretch**: One-tap optimization for faint nebulosity.
- **Color Calibration Engine**:
    - **Temperature (Kelvin) & Tint**: Pro-grade color correction.
    - **Auto White Balance**: Intelligent gain extraction from sensor metadata.
    - **Hot Pixel Removal**: Aggressive outlier suppression for long exposures.
- **Lossless Export Options**:
    - **16-bit Linear TIFF**: For professional post-processing (PixInsight/Siril).
    - **16-bit Stretched TIFF**: Ready-to-use high-dynamic-range output.
    - **RAW DNG**: Exact sensor data with correct orientation and metadata.
    - **Full-Res PNG**: High-quality preview-style export.
- **No Heavy Frameworks**: Pure Java implementation for maximum efficiency on MediaTek Helio G96.
- **Build-System Agnostic**: No Gradle or Android Studio required. Built with raw SDK tools.

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
