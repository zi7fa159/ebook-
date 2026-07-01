import { ImageFrame } from './ImageFrame.js';

export class Stacking {
    /**
     * Weighted Mean Stacking
     */
    static weightedMeanStack(frames, weights, width, height, channels) {
        const accumulator = new Float64Array(width * height * channels);
        let totalWeight = 0;

        for (let f = 0; f < frames.length; f++) {
            const data = frames[f];
            const w = weights[f];
            totalWeight += w;
            for (let i = 0; i < data.length; i++) {
                accumulator[i] += data[i] * w;
            }
        }

        const output = new Float32Array(accumulator.length);
        for (let i = 0; i < accumulator.length; i++) {
            output[i] = accumulator[i] / totalWeight;
        }
        return output;
    }
}
