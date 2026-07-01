# OpenLucky - Professional Browser-Based Lucky Imaging

OpenLucky is a high-performance, scientific-grade Lucky Imaging application that runs entirely in the browser.

## Features

- **SER Support**: Full parser for SER video files (8/16-bit, Mono/RGB/Bayer).
- **Quality Analysis**: Advanced frame quality estimation using Variance of Laplacian.
- **Registration**: Multi-scale coarse-to-fine frame alignment.
- **Stacking**: Robust Mean and Sigma Clipping stacking algorithms.
- **Post-processing**: Real-time ATrous Wavelets and Unsharp Masking.
- **Parallel Processing**: Utilizes Web Workers for background computation.

## Technical Architecture

- **Core**: TypeScript-based modular architecture.
- **IO**: Streaming SER parser.
- **Pipeline**: Decoupled quality, registration, and stacking modules.
- **Workers**: Offloaded heavy computations to avoid UI freezing.

## Getting Started

1. Open the application in a modern browser.
2. Load an SER file.
3. Wait for quality analysis to complete.
4. Click "Stack Selected Frames" to process.
5. Use Wavelet sliders to sharpen the result.

## Development

```bash
npm install
npm run dev   # Start development server
npm test       # Run unit tests
npx playwright test  # Run UI tests
```
