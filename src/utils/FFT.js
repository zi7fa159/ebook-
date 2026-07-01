/**
 * Fast Fourier Transform (Cooley-Tukey)
 */
export class FFT {
    static fft(re, im) {
        const n = re.length;
        if (n <= 1) return;

        const reEven = new Float32Array(n / 2);
        const imEven = new Float32Array(n / 2);
        const reOdd = new Float32Array(n / 2);
        const imOdd = new Float32Array(n / 2);

        for (let i = 0; i < n / 2; i++) {
            reEven[i] = re[2 * i];
            imEven[i] = im[2 * i];
            reOdd[i] = re[2 * i + 1];
            imOdd[i] = im[2 * i + 1];
        }

        this.fft(reEven, imEven);
        this.fft(reOdd, imOdd);

        for (let k = 0; k < n / 2; k++) {
            const angle = -2 * Math.PI * k / n;
            const wRe = Math.cos(angle);
            const wIm = Math.sin(angle);

            const tRe = wRe * reOdd[k] - wIm * imOdd[k];
            const tIm = wRe * imOdd[k] + wIm * reOdd[k];

            re[k] = reEven[k] + tRe;
            im[k] = imEven[k] + tIm;
            re[k + n / 2] = reEven[k] - tRe;
            im[k + n / 2] = imEven[k] - tIm;
        }
    }

    // 2D FFT would be needed for registration
    static fft2d(re, im, width, height) {
        // Rows
        for (let y = 0; y < height; y++) {
            const rowRe = re.subarray(y * width, (y + 1) * width);
            const rowIm = im.subarray(y * width, (y + 1) * width);
            this.fft(rowRe, rowIm);
        }
        // Cols
        const colRe = new Float32Array(height);
        const colIm = new Float32Array(height);
        for (let x = 0; x < width; x++) {
            for (let y = 0; y < height; y++) {
                colRe[y] = re[y * width + x];
                colIm[y] = im[y * width + x];
            }
            this.fft(colRe, colIm);
            for (let y = 0; y < height; y++) {
                re[y * width + x] = colRe[y];
                im[y * width + x] = colIm[y];
            }
        }
    }
}
