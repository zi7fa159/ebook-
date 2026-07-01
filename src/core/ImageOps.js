/**
 * Image processing utilities
 */
export class ImageOps {
    static getStats(frame) {
        let min = Infinity;
        let max = -Infinity;
        let sum = 0;
        const data = frame.data;
        const len = data.length;
        for (let i = 0; i < len; i++) {
            const v = data[i];
            if (v < min) min = v;
            if (v > max) max = v;
            sum += v;
        }
        return { min, max, mean: sum / len };
    }

    static normalize(frame) {
        const stats = this.getStats(frame);
        const range = stats.max - stats.min;
        if (range === 0) return;
        const data = frame.data;
        for (let i = 0; i < data.length; i++) {
            data[i] = (data[i] - stats.min) / range;
        }
    }

    /**
     * Bilinear Debayering
     * Patterns: 8=RGGB, 9=GRBG, 10=GBRG, 11=BGGR
     */
    static debayer(frame, patternID) {
        if (frame.channels !== 1) return frame;
        const w = frame.width;
        const h = frame.height;
        const out = new Float32Array(w * h * 3);
        const src = frame.data;

        for (let y = 0; y < h; y++) {
            for (let x = 0; x < w; x++) {
                const idx = y * w + x;
                const outIdx = idx * 3;
                const isEvenRow = y % 2 === 0;
                const isEvenCol = x % 2 === 0;

                let r, g, b;

                // RGGB Pattern (8)
                // R G
                // G B
                if (patternID === 8) {
                    if (isEvenRow && isEvenCol) { // R pixel
                        r = src[idx];
                        g = this.avg(src, w, h, x, y, [[-1, 0], [1, 0], [0, -1], [0, 1]]);
                        b = this.avg(src, w, h, x, y, [[-1, -1], [1, -1], [-1, 1], [1, 1]]);
                    } else if (isEvenRow && !isEvenCol) { // G pixel (row 0)
                        r = this.avg(src, w, h, x, y, [[-1, 0], [1, 0]]);
                        g = src[idx];
                        b = this.avg(src, w, h, x, y, [[0, -1], [0, 1]]);
                    } else if (!isEvenRow && isEvenCol) { // G pixel (row 1)
                        r = this.avg(src, w, h, x, y, [[0, -1], [0, 1]]);
                        g = src[idx];
                        b = this.avg(src, w, h, x, y, [[-1, 0], [1, 0]]);
                    } else { // B pixel
                        r = this.avg(src, w, h, x, y, [[-1, -1], [1, -1], [-1, 1], [1, 1]]);
                        g = this.avg(src, w, h, x, y, [[-1, 0], [1, 0], [0, -1], [0, 1]]);
                        b = src[idx];
                    }
                } else {
                    // Fallback to Mono if pattern not supported yet
                    r = g = b = src[idx];
                }

                out[outIdx] = r;
                out[outIdx + 1] = g;
                out[outIdx + 2] = b;
            }
        }

        return { width: w, height: h, channels: 3, data: out };
    }

    static avg(src, w, h, x, y, offsets) {
        let sum = 0;
        let count = 0;
        for (const [ox, oy] of offsets) {
            const nx = x + ox;
            const ny = y + oy;
            if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                sum += src[ny * w + nx];
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }
}
