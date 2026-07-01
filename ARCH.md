# Lucky Imaging Web Application - Architecture

## Overview
This application is a production-grade Lucky Imaging tool built entirely for the browser. It follows a modular, high-performance architecture designed to handle large astronomical datasets (10,000+ frames) locally.

## Core Principles
- **Local-first**: All processing happens on the client.
- **Non-blocking**: Heavy computations are offloaded to Web Workers.
- **Hardware-accelerated**: WebGPU/WebGL2 used for image processing and FFTs.
- **Memory-efficient**: Use of `SharedArrayBuffer`, `Transferable` objects, and streaming to manage memory.
- **Scientific Accuracy**: Internal processing uses 32-bit or 64-bit floating point.

## Component Architecture

### 1. UI Layer (Main Thread)
- **Framework**: Minimalist component-based architecture (Vanilla JS/ES Modules).
- **State Management**: Centralized store with an observer pattern.
- **Renderer**: `OffscreenCanvas` for high-performance image display and interaction (zoom/pan).
- **Components**:
    - Project Explorer
    - Frame Viewer
    - Quality Plotter
    - Alignment Point Manager
    - Post-processing Panel

### 2. IO Subsystem
- **File System Access API**: For direct file reading/writing.
- **Parsers**:
    - `SERParser`: High-speed binary parser for SER format.
    - `FITSParser`: Metadata and image data extractor for FITS.
    - `VideoDecoder`: Using WebCodecs API for MP4/AVI/etc.
- **Streaming**: Data is processed in chunks to keep memory usage low.

### 3. Orchestration (Processing Pipeline)
The pipeline manages the Lucky Imaging workflow:
1. **Import/Ingestion**: Load metadata and index frames.
2. **Quality Analysis**: Run estimators on each frame.
3. **Registration**: Calculate offsets (Global and Local Alignment Points).
4. **Stacking**: Accumulate frames using robust statistical methods (Sigma Clipping, etc.).
5. **Post-processing**: Apply sharpening, levels, and deconvolution.

### 4. Worker Pool
- **Job Queue**: Manages tasks across multiple Web Workers.
- **Algorithm Modules**:
    - `MathUtils`: FFT, Matrix ops, Statistics.
    - `RegistrationEngine`: Phase Correlation, Feature matching.
    - `StackingEngine`: Weighted accumulation, Drizzle.

### 5. GPU Subsystem
- **WebGPU/WebGL2 Kernels**:
    - Convolution (Wavelets, Unsharp Mask).
    - FFT (Registration, Deconvolution).
    - Stacking accumulation.
    - Real-time display transforms (Debayer, Levels).

## Data Representation
- **Internal Image Format**: `Float32Array` for maximum precision during processing.
- **Frame Metadata**: Stored in IndexedDB for persistence and quick access.
- **Memory Management**: A custom `BufferManager` tracks allocations and reuses memory to avoid GC thrashing.

## Technology Stack
- **Languages**: ES2023 JavaScript, GLSL/WGSL (GPU), C++/Rust (via WASM for specialized math).
- **APIs**: Web Workers, WebGPU, WebGL2, File System Access API, IndexedDB, WebCodecs.

## Directory Structure
- `src/core/`: Main pipeline and orchestration logic.
- `src/io/`: File parsers and export modules.
- `src/ui/`: UI components and rendering.
- `src/workers/`: Web Worker scripts for parallel processing.
- `src/gpu/`: GPU shaders and compute kernels.
- `src/utils/`: Math and helper libraries.
- `tests/`: Unit and integration tests.
