import { describe, it, expect } from 'vitest';
import { QualityEstimators } from '../../src/utils/quality.js';

describe('QualityEstimators', () => {
    it('should calculate higher variance of laplacian for sharper images', () => {
        const width = 10;
        const height = 10;

        // Blurred image (uniform)
        const blurred = new Float32Array(width * height).fill(0.5);
        const q1 = QualityEstimators.varianceOfLaplacian(blurred, width, height);

        // Sharp image (checkerboard)
        const sharp = new Float32Array(width * height);
        for(let i=0; i<sharp.length; i++) sharp[i] = (i % 2 === 0) ? 1.0 : 0.0;
        const q2 = QualityEstimators.varianceOfLaplacian(sharp, width, height);

        expect(q2).toBeGreaterThan(q1);
    });

    it('should calculate RMS contrast correctly', () => {
        const data = new Float32Array([0, 1, 0, 1]);
        const rms = QualityEstimators.rmsContrast(data);
        expect(rms).toBe(0.5);
    });
});
