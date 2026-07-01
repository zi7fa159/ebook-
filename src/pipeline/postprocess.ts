export function unsharpMask(data: Float32Array, width: number, height: number, amount: number, radius: number): Float32Array {
    const result = new Float32Array(data.length);
    const blurred = gaussianBlur(data, width, height, radius);

    for (let i = 0; i < data.length; i++) {
        result[i] = data[i] + (data[i] - blurred[i]) * amount;
    }
    return result;
}

function gaussianBlur(data: Float32Array, width: number, height: number, radius: number): Float32Array {
    // Simple box blur as approximation for performance if radius is small,
    // or implement a proper Gaussian. For now, a 3x3 box blur.
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

export function applyWavelets(data: Float32Array, width: number, height: number, layers: number[]): Float32Array {
    // Basic ATrous wavelet transform
    // Each layer represents a detail scale
    // layers[i] is the weight for detail at 2^i scale

    let current = new Float32Array(data);
    const result = new Float32Array(data.length);

    for (let i = 0; i < layers.length; i++) {
        const next = boxBlur(current, width, height, Math.pow(2, i));
        for (let j = 0; j < data.length; j++) {
            const detail = current[j] - next[j];
            result[j] += detail * layers[i];
        }
        current = next;
    }

    // Add remaining residual
    for (let j = 0; j < data.length; j++) {
        result[j] += current[j];
    }

    return result;
}

function boxBlur(data: Float32Array, width: number, height: number, step: number): Float32Array {
    const result = new Float32Array(data.length);
    const s = Math.floor(step);
    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            let sum = 0;
            let count = 0;
            // 5-point star kernel for performance in ATrous
            const points = [[0,0], [s,0], [-s,0], [0,s], [0,-s]];
            for (const [dx, dy] of points) {
                const ix = x + dx;
                const iy = y + dy;
                if (ix >= 0 && ix < width && iy >= 0 && iy < height) {
                    sum += data[iy * width + ix];
                    count++;
                }
            }
            result[y * width + x] = sum / count;
        }
    }
    return result;
}
