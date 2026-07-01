import FFT from 'fft.js';

/**
 * Computes the best global translation between ref and src using
 * Simplified Cross-Correlation in the spatial domain for robustness,
 * but multi-scale for performance.
 */
export function alignFrames(ref: Uint8Array | Uint16Array, src: Uint8Array | Uint16Array, width: number, height: number): { x: number, y: number } {
    // Multi-scale approach
    // 1. Downsample (skip pixels)
    // 2. Find coarse alignment
    // 3. Refine at full resolution

    const searchRadius = 20;
    const coarseStep = 4;

    let maxCorr = -1;
    let bestX = 0;
    let bestY = 0;

    const centerX = Math.floor(width / 2);
    const centerY = Math.floor(height / 2);
    const winSize = 128;
    const halfWin = 64;

    // Coarse pass
    for (let dy = -searchRadius; dy <= searchRadius; dy += 2) {
        for (let dx = -searchRadius; dx <= searchRadius; dx += 2) {
            let corr = 0;
            for (let y = -halfWin; y < halfWin; y += coarseStep) {
                for (let x = -halfWin; x < halfWin; x += coarseStep) {
                    const refVal = ref[(centerY + y) * width + (centerX + x)];
                    const srcVal = src[(centerY + y + dy) * width + (centerX + x + dx)];
                    corr += Math.abs(refVal - srcVal);
                }
            }
            if (maxCorr === -1 || corr < maxCorr) {
                maxCorr = corr;
                bestX = dx;
                bestY = dy;
            }
        }
    }

    // Fine pass
    const coarseX = bestX;
    const coarseY = bestY;
    maxCorr = -1;
    for (let dy = coarseY - 2; dy <= coarseY + 2; dy++) {
        for (let dx = coarseX - 2; dx <= coarseX + 2; dx++) {
            let corr = 0;
            for (let y = -halfWin; y < halfWin; y += 2) {
                for (let x = -halfWin; x < halfWin; x += 2) {
                    const refVal = ref[(centerY + y) * width + (centerX + x)];
                    const srcVal = src[(centerY + y + dy) * width + (centerX + x + dx)];
                    corr += Math.abs(refVal - srcVal);
                }
            }
            if (maxCorr === -1 || corr < maxCorr) {
                maxCorr = corr;
                bestX = dx;
                bestY = dy;
            }
        }
    }

    return { x: bestX, y: bestY };
}
