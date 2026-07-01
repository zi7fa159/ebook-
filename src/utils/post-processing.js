/**
 * Post-processing filters and enhancements.
 */
export class PostProcessing {
    /**
     * Discrete Wavelet Transform (simple Haar or B3-Spline).
     * Used for multi-scale sharpening (À Trous).
     */
    static aTrousWavelet(data, width, height, levels = 5) {
        let current = new Float32Array(data);
        const kernels = [
            [1/16, 1/4, 3/8, 1/4, 1/16], // B3-Spline
        ];

        const scales = [];
        let previous = current;

        for (let l = 0; l < levels; l++) {
            const step = Math.pow(2, l);
            const smoothed = this.convolveSeparable(previous, width, height, kernels[0], step);
            const detail = new Float32Array(width * height);
            for (let i = 0; i < detail.length; i++) {
                detail[i] = previous[i] - smoothed[i];
            }
            scales.push(detail);
            previous = smoothed;
        }

        scales.push(previous); // Residual (last smoothed)
        return scales;
    }

    static convolveSeparable(data, width, height, kernel, step) {
        const temp = new Float32Array(data.length);
        const result = new Float32Array(data.length);
        const r = Math.floor(kernel.length / 2);

        // Horizontal
        for (let y = 0; y < height; y++) {
            for (let x = 0; x < width; x++) {
                let sum = 0;
                for (let k = -r; k <= r; k++) {
                    const sx = Math.min(Math.max(x + k * step, 0), width - 1);
                    sum += data[y * width + sx] * kernel[k + r];
                }
                temp[y * width + x] = sum;
            }
        }

        // Vertical
        for (let y = 0; y < height; y++) {
            for (let x = 0; x < width; x++) {
                let sum = 0;
                for (let k = -r; k <= r; k++) {
                    const sy = Math.min(Math.max(y + k * step, 0), height - 1);
                    sum += temp[sy * width + x] * kernel[k + r];
                }
                result[y * width + x] = sum;
            }
        }

        return result;
    }

    /**
     * Apply sharpening by scaling wavelet layers.
     */
    static sharpenWavelets(data, width, height, weights) {
        const scales = this.aTrousWavelet(data, width, height, weights.length);
        const result = new Float32Array(width * height);

        for (let i = 0; i < result.length; i++) {
            let sum = 0;
            for (let l = 0; l < weights.length; l++) {
                sum += scales[l][i] * weights[l];
            }
            // Add weighted details to residual
            result[i] = scales[scales.length - 1][i] + sum;
        }

        return result;
    }
}
