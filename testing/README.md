# A2LS (Testing Branch)

This folder contains the experimental version of A2LS with high-quality roadmap features implemented.

## Experimental Features

### 💎 Software Drizzle (Sub-pixel Alignment)
- **Feature**: Replaced nearest-neighbor Bayer alignment with **Bilinear Bayer Interpolation**.
- **Benefit**: Significantly sharper stars and reduced aliasing by using the fractional part of the alignment vector.
- **RAM Impact**: None (Same buffer size, more CPU cycles per frame).

### 📐 Flat Field Calibration
- **Feature**: Dedicated "FLAT" calibration button and 20-frame sequence.
- **Benefit**: Corrects vignetting (dark corners) and sensor dust spots. Resulting fields are perfectly uniform.
- **Process**: Cover the lens with a white cloth and a uniform light source (e.g., tablet screen), then tap FLAT.
- **RAM Impact**: +200 MB (float buffer for Master Flat).

### ✨ Adaptive Sharpening
- **Feature**: On-the-fly Laplacian sharpening in the Bayer domain.
- **UI**: Added "Adaptive Sharpening" slider in Tune Studio.
- **Benefit**: Recovers fine detail without the need for heavy post-processing.
- **RAM Impact**: Negligible.

## Resource Monitor
Use the yellow "MEM" display to ensure you are not exceeding the 1024 MB heap limit when using both Darks and Flats (~1100 MB combined).

## Compilation
```bash
./scripts/build.sh
```
The APK will be generated at `testing/app.apk`.
