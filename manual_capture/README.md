# Manual Raw Capture

A simple, full-manual RAW capture app for Android.

## Features
- **Full Resolution RAW**: Captures 12-bit/14-bit RAW Sensor data (DNG).
- **Manual Controls**:
    - **Exposure**: Dynamic range (default 0.01s - 2s).
    - **ISO**: Dynamic range (default 100 - 3200).
    - **Focus**: Manual focus (Infinity to Macro).
    - **White Balance**: Kelvin adjustment.
- **Dynamic Limits**: Long-click on any value (e.g., the "1.0s" text) to set new Min/Max limits for that slider. This allows for extremely high-resolution control.
- **Folder Selection**: Choose exactly where to save your DNGs using the System Folder Picker.
- **Modes**:
    - **Single (SNGL)**: Capture one frame and stop.
    - **Continuous (CONT)**: Shoot, save, and immediately start next capture until stopped.

## Build Instructions
1. Set `ANDROID_HOME`.
2. Run `./scripts/build.sh`.
3. Install via `./scripts/install.sh`.

## UI Guide
- **DIR**: Pick the save directory.
- **SNGL/CONT**: Toggle shooting mode.
- **CAPTURE**: Start or stop shooting.
- **HUD**: Shows elapsed time and capture count.
