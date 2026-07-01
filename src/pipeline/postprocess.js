export function unsharpMask(data, width, height, amount, radius) {
    const result = new Float32Array(data.length);
    const blurred = gaussianBlur(data, width, height, radius);

    for (let i = 0; i < data.length; i++) {
        result[i] = data[i] + (data[i] - blurred[i]) * amount;
    }
    return result;
}

function gaussianBlur(data, width, height, _radius) {
    const result = new Float32Array(data.length);
    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            let sum = 0;
            let count = 0;
            for (let ky = -1; ky <= 1; ky++) {
                for (let kx = -1; kx <= 1; kx++) {
                    const iy = y + ky;
                    const ix = x + kx;
                    if (iy >= 0 && iy < height && ix >= 0 && ix < width) {
                        sum += data[iy * width + ix];
                        count++;
                    }
                }
            }
            result[y * width + x] = sum / count;
        }
    }
    return result;
}

/**
 * Implementation of the à trous wavelet transform
 */
export function applyWavelets(data, width, height, layers) {
    let current = new Float32Array(data);
    const result = new Float32Array(data.length);

    for (let i = 0; i < layers.length; i++) {
        // Step size increases by powers of 2 (holes)
        const next = atrousBlur(current, width, height, Math.pow(2, i));
        for (let j = 0; j < data.length; j++) {
            const detail = current[j] - next[j];
            result[j] += detail * layers[i];
        }
        current = next;
    }

    // Add the final residual (the smoothest layer)
    for (let j = 0; j < data.length; j++) {
        result[j] += current[j];
    }

    return result;
}

/**
 * 5x5 B3-Spline kernel for à trous wavelet
 */
function atrousBlur(data, width, height, step) {
    const result = new Float32Array(data.length);
    const kernel = [1/16, 4/16, 6/16, 4/16, 1/16];

    // Temporary buffer for separable convolution
    const temp = new Float32Array(data.length);

    // Horizontal pass
    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            let sum = 0;
            for (let k = -2; k <= 2; k++) {
                const ix = Math.min(Math.max(x + k * step, 0), width - 1);
                sum += data[y * width + ix] * kernel[k + 2];
            }
            temp[y * width + x] = sum;
        }
    }

    // Vertical pass
    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            let sum = 0;
            for (let k = -2; k <= 2; k++) {
                const iy = Math.min(Math.max(y + k * step, 0), height - 1);
                sum += temp[iy * width + x] * kernel[k + 2];
            }
            result[y * width + x] = sum;
        }
    }

    return result;
}
