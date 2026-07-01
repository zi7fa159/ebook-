import { describe, it, expect } from 'vitest';
import { Stacking } from '../src/utils/stacking.js';

describe('Stacking', () => {
    it('should calculate weighted mean correctly', () => {
        const width = 2, height = 2;
        const frames = [
            { data: new Float32Array([1, 1, 1, 1]), dx: 0, dy: 0 },
            { data: new Float32Array([0, 0, 0, 0]), dx: 0, dy: 0 }
        ];
        const weights = [0.75, 0.25];

        const result = Stacking.weightedMean(frames, weights, width, height);
        expect(result[0]).toBe(0.75);
    });

    it('should reject outliers in sigma clipping', () => {
        const width = 1, height = 1;
        const frames = [
            { data: new Float32Array([10]) },
            { data: new Float32Array([11]) },
            { data: new Float32Array([10]) },
            { data: new Float32Array([100]) } // Outlier
        ];

        const result = Stacking.sigmaClipping(frames, width, height, 2.0);
        expect(result[0]).toBeLessThan(20); // Should be around 10.33
    });
});
