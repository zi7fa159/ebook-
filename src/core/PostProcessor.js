import { Wavelets } from './Wavelets.js';

export class PostProcessor {
    static applyLevels(frame, blackPoint, whitePoint, gamma) {
        const data = frame.data;
        const len = data.length;
        const range = whitePoint - blackPoint;

        for (let i = 0; i < len; i++) {
            let v = (data[i] - blackPoint) / range;
            v = Math.max(0, Math.min(1, v));
            if (gamma !== 1) {
                v = Math.pow(v, 1 / gamma);
            }
            data[i] = v;
        }
    }

    static applyUnsharpMask(frame, amount, radius) {
        const blurred = this.gaussianBlur(frame, radius);
        const data = frame.data;
        const bData = blurred.data;
        for (let i = 0; i < data.length; i++) {
            data[i] = data[i] + amount * (data[i] - bData[i]);
            data[i] = Math.max(0, Math.min(1, data[i]));
        }
    }

    static gaussianBlur(frame, sigma) {
        const w = frame.width;
        const h = frame.height;
        const stride = frame.channels;
        const data = frame.data;
        const kernel = this.getGaussianKernel(sigma);
        const r = Math.floor(kernel.length / 2);

        const temp = new Float32Array(data.length);
        const output = new Float32Array(data.length);

        // Horizontal pass
        for (let y = 0; y < h; y++) {
            for (let x = 0; x < w; x++) {
                for (let c = 0; c < stride; c++) {
                    let sum = 0;
                    for (let k = -r; k <= r; k++) {
                        const jx = Math.min(w - 1, Math.max(0, x + k));
                        sum += data[(y * w + jx) * stride + c] * kernel[k + r];
                    }
                    temp[(y * w + x) * stride + c] = sum;
                }
            }
        }

        // Vertical pass
        for (let y = 0; y < h; y++) {
            for (let x = 0; x < w; x++) {
                for (let c = 0; c < stride; c++) {
                    let sum = 0;
                    for (let k = -r; k <= r; k++) {
                        const iy = Math.min(h - 1, Math.max(0, y + k));
                        sum += temp[(iy * w + x) * stride + c] * kernel[k + r];
                    }
                    output[(y * w + x) * stride + c] = sum;
                }
            }
        }

        return { width: w, height: h, channels: stride, data: output };
    }

    static getGaussianKernel(sigma) {
        const r = Math.ceil(sigma * 3);
        const size = 2 * r + 1;
        const kernel = new Float32Array(size);
        let sum = 0;
        const s2 = 2 * sigma * sigma;

        for (let i = -r; i <= r; i++) {
            const v = Math.exp(-(i * i) / s2);
            kernel[i + r] = v;
            sum += v;
        }

        for (let i = 0; i < size; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }
}
