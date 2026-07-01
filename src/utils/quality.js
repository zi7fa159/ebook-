/**
 * Quality estimation algorithms for astronomical imaging.
 */

export class QualityEstimators {
    /**
     * Variance of Laplacian (focus measure).
     * Good for sharp edges.
     */
    static varianceOfLaplacian(data, width, height) {
        const laplacian = new Float32Array(width * height);
        // Simple 3x3 Laplacian kernel
        //  0  1  0
        //  1 -4  1
        //  0  1  0

        let sum = 0;
        let count = 0;

        for (let y = 1; y < height - 1; y++) {
            for (let x = 1; x < width - 1; x++) {
                const i = y * width + x;
                const val = (
                    data[i - width] +
                    data[i - 1] - 4 * data[i] + data[i + 1] +
                    data[i + width]
                );
                laplacian[i] = val;
                sum += val;
                count++;
            }
        }

        const mean = sum / count;
        let variance = 0;
        for (let y = 1; y < height - 1; y++) {
            for (let x = 1; x < width - 1; x++) {
                const i = y * width + x;
                const diff = laplacian[i] - mean;
                variance += diff * diff;
            }
        }

        return variance / count;
    }

    /**
     * RMS Contrast.
     * Measures the standard deviation of pixel intensities.
     */
    static rmsContrast(data) {
        let sum = 0;
        const n = data.length;
        for (let i = 0; i < n; i++) {
            sum += data[i];
        }
        const mean = sum / n;

        let variance = 0;
        for (let i = 0; i < n; i++) {
            const diff = data[i] - mean;
            variance += diff * diff;
        }

        return Math.sqrt(variance / n);
    }
}
