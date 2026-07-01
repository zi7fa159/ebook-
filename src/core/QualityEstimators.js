/**
 * Quality Estimators for Lucky Imaging
 */
export class QualityEstimators {
    /**
     * Variance of Laplacian (standard sharpness metric)
     */
    static varianceOfLaplacian(frame) {
        const w = frame.width;
        const h = frame.height;
        const data = frame.data;
        // Use only first channel if multi-channel
        const stride = frame.channels;

        let sum = 0;
        let sumSq = 0;
        let count = 0;

        // Laplacian kernel:
        //  0  1  0
        //  1 -4  1
        //  0  1  0

        for (let y = 1; y < h - 1; y++) {
            for (let x = 1; x < w - 1; x++) {
                const idx = (y * w + x) * stride;
                const lap = (
                    data[((y - 1) * w + x) * stride] +
                    data[((y + 1) * w + x) * stride] +
                    data[(y * w + (x - 1)) * stride] +
                    data[(y * w + (x + 1)) * stride] -
                    4 * data[idx]
                );
                sum += lap;
                sumSq += lap * lap;
                count++;
            }
        }

        const mean = sum / count;
        const variance = (sumSq / count) - (mean * mean);
        return variance;
    }

    /**
     * RMS Contrast
     */
    static rmsContrast(frame) {
        const data = frame.data;
        const stride = frame.channels;
        let sum = 0;
        let sumSq = 0;
        const count = frame.width * frame.height;

        for (let i = 0; i < count; i++) {
            const v = data[i * stride];
            sum += v;
            sumSq += v * v;
        }

        const mean = sum / count;
        return Math.sqrt(Math.max(0, (sumSq / count) - (mean * mean)));
    }
}
