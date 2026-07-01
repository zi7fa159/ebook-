import { FFT2D } from './fft.js';

export class Registration {
    /**
     * Phase Correlation for translation estimation.
     * Works in frequency domain.
     */
    static phaseCorrelation(refData, targetData, width, height) {
        // Assume width and height are powers of two for simplicity in this implementation
        const n = width * height;
        const re1 = new Float32Array(refData);
        const im1 = new Float32Array(n);
        const re2 = new Float32Array(targetData);
        const im2 = new Float32Array(n);

        FFT2D.transform(re1, im1, width, height, false);
        FFT2D.transform(re2, im2, width, height, false);

        // Cross-power spectrum
        const reCP = new Float32Array(n);
        const imCP = new Float32Array(n);

        for (let i = 0; i < n; i++) {
            // Conj(F1) * F2
            const r = re1[i] * re2[i] + im1[i] * im2[i];
            const j = re1[i] * im2[i] - im1[i] * re2[i];
            const mag = Math.sqrt(r * r + j * j) + 1e-10;
            reCP[i] = r / mag;
            imCP[i] = j / mag;
        }

        FFT2D.transform(reCP, imCP, width, height, true);

        // Find peak in spatial domain
        let maxVal = -1;
        let peakIdx = 0;
        for (let i = 0; i < n; i++) {
            if (reCP[i] > maxVal) {
                maxVal = reCP[i];
                peakIdx = i;
            }
        }

        let py = Math.floor(peakIdx / width);
        let px = peakIdx % width;

        if (px > width / 2) px -= width;
        if (py > height / 2) py -= height;

        return { dx: px, dy: py };
    }
}
