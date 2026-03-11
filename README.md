# AstroStacker (Native Android Astrophotography Stacker)

A professional, minimal, and dependency-free Android application for LIVE astrophotography stacking with full manual camera control.

## Key Features
- **Full Manual Camera2 Control:** Exposure (up to 30s), ISO (up to 6400+), and Manual Focus.
- **Expert Star Detection:** Adaptive thresholding (Mean + K*StdDev) with sub-pixel centroid refinement.
- **Robust Alignment:** Translation-only alignment with frame rejection for motion or cloud cover.
- **High-Precision Stacking:** 32-bit float incremental averaging pipeline.
- **Live Visualization:** Real-time auto-histogram stretch (percentile-based) for previewing faint objects.
- **Expert Build System:** Custom Bash-based build pipeline (No Gradle, No Android Studio).

## Requirements
- Android SDK Command Line Tools
- `javac` (JDK 11+)
- Android device with Camera2 API "Level 3" or "Full" support (e.g., Realme 8i).

## Build Instructions
1. Set your `ANDROID_HOME` environment variable:
   ```bash
   export ANDROID_HOME=/path/to/your/android-sdk
   ```
2. Run the build script:
   ```bash
   bash scripts/build.sh
   ```
   This will generate `bin/AstroStacker.apk`.

## Installation
Run the install script to install the APK on a connected device and launch the app:
```bash
bash scripts/install.sh
```

## Developer Notes
- **No External Libraries:** Every algorithm (star detection, TIFF writing, image processing) is implemented in pure Java.
- **Optimization:** Frames are downscaled to ~3MP for the live stacking pipeline to ensure high performance on mid-range SoCs like the Helio G96.
- **File Export:** Saves final stacks as 16-bit Grayscale TIFF in the `Pictures/AstroStacker` directory.
