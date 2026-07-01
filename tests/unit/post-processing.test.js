import { describe, it, expect } from 'vitest';
import { PostProcessing } from '../../src/utils/post-processing.js';

describe('PostProcessing', () => {
    it('should decompose and recompose wavelets correctly', () => {
        const width = 16, height = 16;
        const data = new Float32Array(width * height);
        for(let i=0; i<data.length; i++) data[i] = Math.random();

        const weights = [1, 1, 1]; // No extra sharpening
        const result = PostProcessing.sharpenWavelets(data, width, height, weights);

        for(let i=0; i<data.length; i++) {
            expect(result[i]).toBeCloseTo(data[i], 5);
        }
    });
});
