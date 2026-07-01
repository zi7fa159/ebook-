export function alignFrames(ref, src, width, height) {
    const searchRadius = Math.min(20, Math.floor(width / 10), Math.floor(height / 10));
    const coarseStep = 4;

    let maxCorr = -1;
    let bestX = 0;
    let bestY = 0;

    const centerX = Math.floor(width / 2);
    const centerY = Math.floor(height / 2);
    // Use a dynamic window size based on image dimensions
    const winSize = Math.min(128, width - 2 * searchRadius, height - 2 * searchRadius);
    const halfWin = Math.floor(winSize / 2);

    if (winSize < 4) return { x: 0, y: 0 };

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

    const coarseX = bestX;
    const coarseY = bestY;
    maxCorr = -1;
    for (let dy = coarseY - 2; dy <= coarseY + 2; dy++) {
        if (dy < -searchRadius || dy > searchRadius) continue;
        for (let dx = coarseX - 2; dx <= coarseX + 2; dx++) {
            if (dx < -searchRadius || dx > searchRadius) continue;
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
