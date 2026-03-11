# AstroStacker

A minimal Android application for LIVE astrophotography stacking with full manual camera control.

## Features
- Full Camera2 manual control (Exposure, ISO, Focus).
- Real-time star detection and alignment.
- Live stacking using incremental averaging.
- Export final stacked image as PNG.

## Build Requirements
- Android SDK Command Line Tools
- JDK 11 or 21
- `ANDROID_HOME` environment variable pointing to your SDK path.

## How to Build
1. Ensure the paths in `scripts/build.sh` match your SDK installation.
2. Run the build script:
   ```bash
   ./scripts/build.sh
   ```
   The final APK will be in `bin/AstroStacker.apk`.

## How to Install
1. Connect your Android device via USB and enable ADB debugging.
2. Run the install script:
   ```bash
   ./scripts/install.sh
   ```

## Project Structure
- `src/`: Java source code.
- `res/`: Android resources (layouts).
- `scripts/`: Build and installation scripts.
- `AndroidManifest.xml`: App manifest.
