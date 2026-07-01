import { describe, it, expect } from 'vitest';
import { Registration } from '../src/utils/registration.js';
import { FFT2D } from '../src/utils/fft.js';

describe('Registration', () => {
    it('should estimate translation correctly using Phase Correlation', () => {
        const size = 32; // Power of 2
        const ref = new Float32Array(size * size);
        const target = new Float32Array(size * size);

        // Put a "star" at (10, 10)
        ref[10 * size + 10] = 1.0;

        // Offset it by (3, -2) -> (13, 8)
        target[8 * size + 13] = 1.0;

        const result = Registration.phaseCorrelation(ref, target, size, size);

        expect(result.dx).toBe(3);
        expect(result.dy).toBe(-2);
    });
});
