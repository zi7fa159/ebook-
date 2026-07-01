/**
 * 2D FFT implementation for image processing.
 */
export class FFT2D {
    static nextPowerOfTwo(n) {
        return Math.pow(2, Math.ceil(Math.log2(n)));
    }

    static transform(re, im, width, height, invert = false) {
        // Rows
        for (let y = 0; y < height; y++) {
            const rowRe = re.subarray(y * width, (y + 1) * width);
            const rowIm = im.subarray(y * width, (y + 1) * width);
            this.fft1d(rowRe, rowIm, invert);
        }

        // Cols
        const colRe = new Float32Array(height);
        const colIm = new Float32Array(height);
        for (let x = 0; x < width; x++) {
            for (let y = 0; y < height; y++) {
                colRe[y] = re[y * width + x];
                colIm[y] = im[y * width + x];
            }
            this.fft1d(colRe, colIm, invert);
            for (let y = 0; y < height; y++) {
                re[y * width + x] = colRe[y];
                im[y * width + x] = colIm[y];
            }
        }

        if (invert) {
            const n = width * height;
            for (let i = 0; i < n; i++) {
                re[i] /= n;
                im[i] /= n;
            }
        }
    }

    // Iterative 1D FFT with bit-reversal
    static fft1d(re, im, invert) {
        const n = re.length;
        let j = 0;
        for (let i = 0; i < n; i++) {
            if (i < j) {
                [re[i], re[j]] = [re[j], re[i]];
                [im[i], im[j]] = [im[j], im[i]];
            }
            let m = n >> 1;
            while (m >= 1 && j >= m) {
                j -= m;
                m >>= 1;
            }
            j += m;
        }

        for (let len = 2; len <= n; len <<= 1) {
            const angle = (invert ? 2 : -2) * Math.PI / len;
            const wlenRe = Math.cos(angle);
            const wlenIm = Math.sin(angle);
            for (let i = 0; i < n; i += len) {
                let wRe = 1;
                let wIm = 0;
                for (let k = 0; k < len / 2; k++) {
                    const uRe = re[i + k];
                    const uIm = im[i + k];
                    const vRe = re[i + k + len / 2] * wRe - im[i + k + len / 2] * wIm;
                    const vIm = re[i + k + len / 2] * wIm + im[i + k + len / 2] * wRe;
                    re[i + k] = uRe + vRe;
                    im[i + k] = uIm + vIm;
                    re[i + k + len / 2] = uRe - vRe;
                    im[i + k + len / 2] = uIm - vIm;
                    const nextWRe = wRe * wlenRe - wIm * wlenIm;
                    wIm = wRe * wlenIm + wIm * wlenRe;
                    wRe = nextWRe;
                }
            }
        }
    }
}
