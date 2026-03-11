# AstroStacker

A minimal but powerful Android application for LIVE astrophotography stacking with full manual camera control.

## Requirements

- Android SDK Command Line Tools
- Java JDK 11
- Android device with Camera2 API support (Manual Sensor capability required)

## Setup

1. Install the Android SDK command line tools.
2. Set `ANDROID_HOME` environment variable to your SDK path.
3. Ensure `javac` is in your `PATH`.

## Build

Run the build script:

```bash
chmod +x project/scripts/build.sh
./project/scripts/build.sh
```

The build script uses:
- `aapt2` to compile and link resources.
- `javac` to compile Java source code.
- `d8` to convert classes to DEX.
- `zipalign` and `apksigner` to finalize the APK.

## Install

Ensure your device is connected via ADB and run:

```bash
chmod +x project/scripts/install.sh
./project/scripts/install.sh
```

## Features

- **Manual Control**: Adjust exposure time (1s - 30s) and ISO.
- **Star Alignment**: Automatically aligns frames based on detected stars (translation only).
- **Live Stacking**: Incremental averaging of frames for improved signal-to-noise ratio.
- **Real-time Preview**: See the stacked result as it grows.
- **16-bit Saving**: Save the final result as a PNG (8-bit for now, 16-bit logic can be extended).

## Architecture

- `MainActivity`: UI and lifecycle management.
- `CameraController`: Camera2 API interaction, manual parameter setting.
- `FrameProcessor`: Orchestrates the processing pipeline (star detection -> alignment -> stacking).
- `StarDetector`: Simple local maxima detection for stars.
- `FrameAligner`: Translation-based alignment.
- `StackEngine`: Float-based incremental averaging.
- `Renderer`: Renders the stack buffer to a SurfaceView.
