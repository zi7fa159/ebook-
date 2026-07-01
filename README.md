# OpenLucky - Professional Browser-Based Lucky Imaging

OpenLucky is a high-performance, scientific-grade Lucky Imaging application that runs entirely in the browser using pure JavaScript, CSS, and HTML.

## Features

- **SER Support**: Full parser for SER video files (8/16-bit, Mono/RGB/Bayer).
- **Video Support**: Support for MP4/MOV using Web APIs.
- **FITS Support**: Support for FITS image sequences (BITPIX 8, 16, 32, -32).
- **Quality Analysis**: Advanced frame quality estimation using Variance of Laplacian, parallelized via Web Workers.
- **Registration**: Multi-point local alignment points (APs) for atmospheric distortion correction.
- **Stacking**: Robust Sigma Clipping stacking with local alignment support.
- **Post-processing**: Real-time à trous wavelet transform and Unsharp Masking.
- **Export**: Save processed results as high-quality PNG.

## Technical Architecture

- **No Build Step**: Pure ES modules, zero dependencies.
- **GPU Acceleration**: Utilizes browser-native Canvas and Web APIs.
- **Memory Efficient**: Correct use of Transferable objects to minimize memory overhead.

## Getting Started

1. Serve the directory via HTTP (e.g., `python3 -m http.server 3000`).
2. Open `index.html` in a modern browser.
3. Load an SER, MP4, or FITS file.
4. Wait for quality analysis.
5. Click "Stack Selected Frames".
6. Adjust Wavelets and Export.

## Development

The application is built using standard web technologies. No `npm` or build tools are required.
