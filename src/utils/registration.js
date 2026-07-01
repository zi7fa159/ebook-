import { FFT2D } from './fft.js';

export class Registration {
    /**
     * Phase Correlation for translation estimation.
     * Works in frequency domain.
     */
    static phaseCorrelation(refData, targetData, width, height) {
        // Find next power of two for FFT
        const paddedWidth = FFT2D.nextPowerOfTwo(width);
        const paddedHeight = FFT2D.nextPowerOfTwo(height);
        const n = paddedWidth * paddedHeight;

        const re1 = new Float32Array(n);
        const im1 = new Float32Array(n);
        const re2 = new Float32Array(n);
        const im2 = new Float32Array(n);

        // Pad images
        for (let y = 0; y < height; y++) {
            for (let x = 0; x < width; x++) {
                re1[y * paddedWidth + x] = refData[y * width + x];
                re2[y * paddedWidth + x] = targetData[y * width + x];
            }
        }

        FFT2D.transform(re1, im1, paddedWidth, paddedHeight, false);
        FFT2D.transform(re2, im2, paddedWidth, paddedHeight, false);

        // Cross-power spectrum
        const reCP = new Float32Array(n);
        const imCP = new Float32Array(n);

        for (let i = 0; i < n; i++) {
            const r = re1[i] * re2[i] + im1[i] * im2[i];
            const j = re1[i] * im2[i] - im1[i] * re2[i];
            const mag = Math.sqrt(r * r + j * j) + 1e-10;
            reCP[i] = r / mag;
            imCP[i] = j / mag;
        }

        FFT2D.transform(reCP, imCP, paddedWidth, paddedHeight, true);

        // Find peak
        let maxVal = -1;
        let peakIdx = 0;
        for (let i = 0; i < n; i++) {
            if (reCP[i] > maxVal) {
                maxVal = reCP[i];
                peakIdx = i;
            }
        }

        let py = Math.floor(peakIdx / paddedWidth);
        let px = peakIdx % paddedWidth;

        if (px > paddedWidth / 2) px -= paddedWidth;
        if (py > paddedHeight / 2) py -= paddedHeight;

        return { dx: px, dy: py };
    }
}
