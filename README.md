# A2LS (Astro Live Stacker for Android)

A native Android application for high-performance astrophotography, performing real-time RAW stacking and star alignment. Optimized for the Realme 8i (RMX3151) 50MP sensor.

## Key Features

### 🔭 Full 50MP RAW Pipeline
- **Lossless Bayer Domain Stacking**: Processes full-resolution RAW frames directly without downscaling, preserving every photon.
- **Floating Point Precision**: 32-bit incremental averaging ensures no data loss during long stacking sessions.

### 🎨 Tune Studio (Professional Grading)
- **Live Histogram**: Real-time logarithmic view of the image data.
- **Advanced Stretching**: Black Point, Mid Point (Gamma), and White Point controls.
- **Hot Pixel Removal**: Vote-based outlier suppression with a dedicated "Aggression" slider.

### 🎯 Manual Focus Tools
- **Overlaid Focus Slider**: Transparent vertical slider for real-time focus adjustments.
- **Focus Zoom**: 800x800 pixel center-crop at full sensor resolution for precise star focusing.

### 🛡️ Noise & Calibration
- **30-Frame Dark Calibration**: Automated Master Dark sequence for thermal noise subtraction.
- **20-Frame Flat Calibration**: Corrects vignetting and sensor dust for a perfectly uniform field.
- **Kappa-Sigma Clipping**: Removes satellites, planes, and cosmic rays from the final stack.

## Resource & RAM Analysis (Realme 8i)
Running 50MP stacking in Java is an intensive operation. The app is optimized to run within the following footprint:

- **Baseline Pipeline**: ~905 MB (Mean Stacking)
- **Advanced Mode**: ~1105 MB (Sigma Clipping + Flats)
- **Heap Status**: Monitored via the "MEM" display in the status bar.

### Roadmap for Higher Sharpness
The following features are proposed for further improving results:
1. **Software Drizzle (1.5x)**: Sub-pixel alignment to recover resolution (+200MB RAM).
2. **Richardson-Lucy Deconvolution**: Post-processing sharpness recovery (+400MB RAM).
3. **Adaptive Star Sharpening**: Localized high-pass filter on star points (Negligible RAM).

## Compilation & Installation
1. **Build**: `./scripts/build.sh`
2. **Install**: `./scripts/install.sh`

## Astrophotography Tips
- **Tripod is Mandatory**.
- **Run Darks First**: Cover the lens and capture 30 frames.
- **Run Flats Second**: Point at a uniform light source (tablet or sky) and capture 20 frames.
- **Use Focus Zoom**: Toggle zoom and adjust the vertical slider until stars are pin-sharp points.
- **Monitor Memory**: If the MEM value exceeds 90% of the total, consider resetting the stack or using Mean instead of Sigma clipping.
