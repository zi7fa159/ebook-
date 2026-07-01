/**
 * Wavelet transformation for sharpening (A Trous Wavelet)
 */
export class Wavelets {
    static atrous(frame, layers = 5) {
        const w = frame.width;
        const h = frame.height;
        const data = frame.data;
        const stride = frame.channels;

        let currentLayer = new Float32Array(data);
        const results = [];

        for (let l = 0; l < layers; l++) {
            const nextLayer = this.convolveAtrous(currentLayer, w, h, stride, l);
            const detail = new Float32Array(currentLayer.length);
            for (let i = 0; i < detail.length; i++) {
                detail[i] = currentLayer[i] - nextLayer[i];
            }
            results.push(detail);
            currentLayer = nextLayer;
        }
        results.push(currentLayer); // Residual
        return results;
    }

    static convolveAtrous(data, w, h, stride, level) {
        const step = Math.pow(2, level);
        const output = new Float32Array(data.length);
        const kernel = [1/16, 4/16, 6/16, 4/16, 1/16];

        for (let y = 0; y < h; y++) {
            for (let x = 0; x < w; x++) {
                for (let c = 0; c < stride; c++) {
                    let sum = 0;
                    for (let i = -2; i <= 2; i++) {
                        for (let j = -2; j <= 2; j++) {
                            const iy = Math.min(h - 1, Math.max(0, y + i * step));
                            const jx = Math.min(w - 1, Math.max(0, x + j * step));
                            sum += data[(iy * w + jx) * stride + c] * kernel[i + 2] * kernel[j + 2];
                        }
                    }
                    output[(y * w + x) * stride + c] = sum;
                }
            }
        }
        return output;
    }

    static reconstruct(layers, weights) {
        const len = layers[0].length;
        const output = new Float32Array(len);
        for (let l = 0; l < weights.length; l++) {
            const w = weights[l];
            const layer = layers[l];
            for (let i = 0; i < len; i++) {
                output[i] += layer[i] * w;
            }
        }
        // Add residual (last layer)
        const residual = layers[layers.length - 1];
        for (let i = 0; i < len; i++) {
            output[i] += residual[i];
        }
        return output;
    }
}
