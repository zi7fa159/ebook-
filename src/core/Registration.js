import { FFT } from '../utils/FFT.js';

export class Registration {
    /**
     * FFT-based Phase Correlation for global alignment
     */
    static findTranslationFFT(refFrame, targetFrame) {
        const w = refFrame.width;
        const h = refFrame.height;

        // FFT requires power of 2. For simplicity, we'll use a fixed 256x256 or 512x512 patch
        const size = 256;
        if (w < size || h < size) return this.findTranslation(refFrame, targetFrame);

        const cx = Math.floor(w / 2);
        const cy = Math.floor(h / 2);

        const re1 = new Float32Array(size * size);
        const im1 = new Float32Array(size * size);
        const re2 = new Float32Array(size * size);
        const im2 = new Float32Array(size * size);

        for (let y = 0; y < size; y++) {
            for (let x = 0; x < size; x++) {
                const srcIdx = ((cy - size/2 + y) * w + (cx - size/2 + x)) * refFrame.channels;
                re1[y * size + x] = refFrame.data[srcIdx];
                re2[y * size + x] = targetFrame.data[srcIdx];
            }
        }

        FFT.fft2d(re1, im1, size, size);
        FFT.fft2d(re2, im2, size, size);

        // Cross-power spectrum
        const reCP = new Float32Array(size * size);
        const imCP = new Float32Array(size * size);
        for (let i = 0; i < size * size; i++) {
            // Conj(F1) * F2
            const r1 = re1[i], i1 = -im1[i];
            const r2 = re2[i], i2 = im2[i];
            const real = r1 * r2 - i1 * i2;
            const imag = r1 * i2 + i1 * r2;
            const mag = Math.sqrt(real * real + imag * imag) + 1e-10;
            reCP[i] = real / mag;
            imCP[i] = imag / mag;
        }

        // Inverse FFT
        // For inverse, we can use the same FFT by swapping im sign and dividing by N
        for (let i = 0; i < size * size; i++) imCP[i] = -imCP[i];
        FFT.fft2d(reCP, imCP, size, size);

        let maxVal = -1;
        let peakX = 0, peakY = 0;
        for (let y = 0; y < size; y++) {
            for (let x = 0; x < size; x++) {
                const val = reCP[y * size + x];
                if (val > maxVal) {
                    maxVal = val;
                    peakX = x;
                    peakY = y;
                }
            }
        }

        if (peakX > size / 2) peakX -= size;
        if (peakY > size / 2) peakY -= size;

        return { dx: -peakX, dy: -peakY };
    }

    // Original spatial correlation for small patches or fallback
    static findTranslation(refFrame, targetFrame, searchRadius = 20) {
        const w = refFrame.width;
        const h = refFrame.height;
        const refData = refFrame.data;
        const targetData = targetFrame.data;
        const stride = refFrame.channels;

        let bestDX = 0;
        let bestDY = 0;
        let maxCorr = -Infinity;

        const patchSize = 128;
        const cx = Math.floor(w / 2);
        const cy = Math.floor(h / 2);
        const xStart = Math.max(searchRadius, cx - patchSize / 2);
        const yStart = Math.max(searchRadius, cy - patchSize / 2);
        const xEnd = Math.min(w - searchRadius, cx + patchSize / 2);
        const yEnd = Math.min(h - searchRadius, cy + patchSize / 2);

        for (let dy = -searchRadius; dy <= searchRadius; dy++) {
            for (let dx = -searchRadius; dx <= searchRadius; dx++) {
                let corr = 0;
                for (let y = yStart; y < yEnd; y++) {
                    for (let x = xStart; x < xEnd; x++) {
                        const refIdx = (y * w + x) * stride;
                        const targetIdx = ((y + dy) * w + (x + dx)) * stride;
                        corr += refData[refIdx] * targetData[targetIdx];
                    }
                }
                if (corr > maxCorr) {
                    maxCorr = corr;
                    bestDX = dx;
                    bestDY = dy;
                }
            }
        }
        return this.refineSubpixel(bestDX, bestDY, maxCorr, refFrame, targetFrame, xStart, xEnd, yStart, yEnd);
    }

    static refineSubpixel(dx, dy, centerCorr, refFrame, targetFrame, xStart, xEnd, yStart, yEnd) {
        const w = refFrame.width;
        const stride = refFrame.channels;
        const refData = refFrame.data;
        const targetData = targetFrame.data;

        const getCorr = (ox, oy) => {
            let corr = 0;
            for (let y = yStart; y < yEnd; y++) {
                for (let x = xStart; x < xEnd; x++) {
                    const refIdx = (y * w + x) * stride;
                    const targetIdx = ((y + oy) * w + (x + ox)) * stride;
                    corr += refData[refIdx] * targetData[targetIdx];
                }
            }
            return corr;
        };

        const c10 = getCorr(dx + 1, dy);
        const cm10 = getCorr(dx - 1, dy);
        const c01 = getCorr(dx, dy + 1);
        const c0m1 = getCorr(dx, dy - 1);

        const dxx = (c10 + cm10 - 2 * centerCorr);
        const dyy = (c01 + c0m1 - 2 * centerCorr);

        let subDX = dx;
        let subDY = dy;

        if (dxx !== 0) subDX -= (c10 - cm10) / (2 * dxx);
        if (dyy !== 0) subDY -= (c01 - c0m1) / (2 * dyy);

        return { dx: subDX, dy: subDY };
    }
}
