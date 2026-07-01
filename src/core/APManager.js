/**
 * AP Manager
 */
export class APManager {
    /**
     * Generate alignment points across the frame
     */
    static generatePoints(frame, size = 64, minContrast = 0.05) {
        const w = frame.width;
        const h = frame.height;
        const points = [];
        const stride = frame.channels;
        const data = frame.data;

        // Simple grid-based AP generation
        const spacing = Math.floor(size * 0.7);
        for (let y = size; y < h - size; y += spacing) {
            for (let x = size; x < w - size; x += spacing) {
                const contrast = this.getLocalContrast(frame, x, y, size / 2);
                if (contrast > minContrast) {
                    points.push({ x, y, size });
                }
            }
        }
        return points;
    }

    static getLocalContrast(frame, cx, cy, radius) {
        const w = frame.width;
        const stride = frame.channels;
        const data = frame.data;
        let min = 1, max = 0;

        for (let y = cy - radius; y < cy + radius; y += 2) {
            for (let x = cx - radius; x < cx + radius; x += 2) {
                const val = data[(y * w + x) * stride];
                if (val < min) min = val;
                if (val > max) max = val;
            }
        }
        return max - min;
    }
}
