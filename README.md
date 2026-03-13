# A2LS (Astro Live Stacker for Android)

A native Android application for high-performance astrophotography, performing real-time RAW stacking and star alignment. Designed for the Realme 8i (RMX3151) 50MP sensor.

## Key Features

### 🔭 Full 50MP RAW Pipeline
- **Lossless Bayer Domain Stacking**: Processes full-resolution RAW frames directly without downscaling, preserving every photon.
- **Floating Point Precision**: 32-bit incremental averaging ensures no data loss during long stacking sessions.

### 🎨 Tune Studio (Professional Grading)
- **Live Histogram**: Real-time logarithmic view of the image data.
- **Advanced Stretching**:
    - **Black Point**: Set the floor to remove sky glow.
    - **Mid Point (Gamma)**: Stretch the signal to reveal faint nebulosity.
    - **White Point**: Control highlight clipping.
- **Color Correction**: Adjustable Color Temperature (K) and Tint, with one-tap Auto White Balance.
- **Hot Pixel Removal**: Vote-based outlier suppression with a dedicated "Aggression" slider to clean up sensor noise during long exposures.

### 🎯 Manual Focus Tools
- **Overlaid Focus Slider**: A transparent, vertical slider on the live preview for ergonomic, real-time focus adjustments.
- **Focus Zoom**: 800x800 pixel center-crop at full sensor resolution. Allows precise focusing on distant stars.

### 🛡️ Noise & Calibration
- **30-Frame Dark Calibration**: Automated sequence to create a clean Master Dark for thermal noise subtraction.
- **Kappa-Sigma Clipping**: Advanced stacking method that automatically identifies and removes satellites, planes, and cosmic rays.

### 💾 High-End Exports
- **16-bit TIFF**: Available in both Linear (for Siril/PixInsight) and Stretched (ready for viewing) formats.
- **RAW DNG**: Standard DNG export for single-frame calibration or editing.
- **Optimized PNG**: High-resolution, processed output for quick sharing.

## Build Requirements
- Android SDK Command-line Tools (build-tools, platforms)
- Java Development Kit (JDK) 11+
- `adb` for device deployment

## Environment Setup
Ensure `ANDROID_HOME` is exported in your shell:
```bash
export ANDROID_HOME=/path/to/your/android-sdk
```

## Compilation & Installation
A2LS is designed to be built **without Gradle** for maximum speed and simplicity.

1. **Build the APK**:
   ```bash
   ./scripts/build.sh
   ```
2. **Install to Device**:
   ```bash
   ./scripts/install.sh
   ```

## Astrophotography Tips for Realme 8i
1. **Tripod is Mandatory**: Stacking requires steady frames for the star alignment algorithm to work.
2. **Dark Frames First**: Always perform a Dark Frame Calibration (30 frames) with the lens covered before starting your session to ensure a clean result.
3. **Use Focus Zoom**: Toggle Focus Zoom and use the vertical slider to make the stars as small and sharp as possible.
4. **Exposure Timing**: For the Realme 8i, 15-20s exposures at ISO 1600 are usually the "sweet spot" for deep sky objects.
5. **Hot Pixel Aggression**: Start with 50%. If you see "static" noise, increase it; if faint stars are disappearing, decrease it.

## Technical Notes
- **Memory**: A2LS uses `largeHeap` to manage the massive memory requirements of 50MP Bayer stacking (~200MB per buffer).
- **Architecture**: Asynchronous pipeline ensures the Camera HAL remains responsive while heavy math is performed in the background.
- **No Libraries**: 100% pure Java implementation of TIFF writers, Star Detection, and Alignment logic.
