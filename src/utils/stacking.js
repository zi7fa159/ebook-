/**
 * Stacking algorithms for astronomical imaging.
 */
export class Stacking {
    /**
     * Weighted Mean Stacking with Kahan Summation for numerical stability.
     */
    static weightedMean(frames, weights, width, height) {
        const n = width * height;
        const result = new Float32Array(n);
        const compensation = new Float32Array(n); // For Kahan summation
        let totalWeight = 0;

        for (let f = 0; f < frames.length; f++) {
            const data = frames[f].data;
            const w = weights[f];
            const dx = frames[f].dx || 0;
            const dy = frames[f].dy || 0;
            totalWeight += w;

            for (let y = 0; y < height; y++) {
                const sy = y + dy;
                if (sy < 0 || sy >= height) continue;

                for (let x = 0; x < width; x++) {
                    const sx = x + dx;
                    if (sx < 0 || sx >= width) continue;

                    const idx = y * width + x;
                    const sIdx = sy * width + sx;

                    // Kahan summation: result += data * w
                    const yVal = (data[sIdx] * w) - compensation[idx];
                    const t = result[idx] + yVal;
                    compensation[idx] = (t - result[idx]) - yVal;
                    result[idx] = t;
                }
            }
        }

        if (totalWeight > 0) {
            for (let i = 0; i < n; i++) {
                result[i] /= totalWeight;
            }
        }

        return result;
    }

    /**
     * Sigma Clipping Stacking.
     * Rejects outliers (satellites, planes, cosmic rays).
     * Uses median for robust initial estimate.
     */
    static sigmaClipping(frames, width, height, sigma = 3.0) {
        const n = width * height;
        const result = new Float32Array(n);
        const frameCount = frames.length;

        for (let i = 0; i < n; i++) {
            const pixels = new Float32Array(frameCount);
            for (let f = 0; f < frameCount; f++) {
                pixels[f] = frames[f].data[i];
            }

            // Robust estimate using Median
            const sorted = new Float32Array(pixels).sort();
            const median = frameCount % 2 === 0
                ? (sorted[frameCount / 2 - 1] + sorted[frameCount / 2]) / 2
                : sorted[Math.floor(frameCount / 2)];

            // Median Absolute Deviation (MAD) is more robust than StdDev
            const diffs = new Float32Array(frameCount);
            for (let f = 0; f < frameCount; f++) {
                diffs[f] = Math.abs(pixels[f] - median);
            }
            const sortedDiffs = diffs.sort();
            const mad = frameCount % 2 === 0
                ? (sortedDiffs[frameCount / 2 - 1] + sortedDiffs[frameCount / 2]) / 2
                : sortedDiffs[Math.floor(frameCount / 2)];

            // Standard deviation approximation from MAD
            const stdDev = 1.4826 * mad;

            let finalSum = 0;
            let finalCount = 0;
            const lower = median - sigma * stdDev;
            const upper = median + sigma * stdDev;

            for (let f = 0; f < frameCount; f++) {
                if (pixels[f] >= lower && pixels[f] <= upper) {
                    finalSum += pixels[f];
                    finalCount++;
                }
            }

            result[i] = finalCount > 0 ? finalSum / finalCount : median;
        }

        return result;
    }
}
