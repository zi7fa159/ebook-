import { stackFramesSigmaClipping } from './pipeline/stacking.js';

/**
 * Advanced stacking using local offsets for each AP
 */
export function stackFramesMultiPoint(frames, offsetsMap, width, height, aps, sigma = 3) {
    const result = new Float32Array(width * height);
    const pixelValues = new Float32Array(frames.length);

    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            // Find the closest AP to this pixel to get its local offset
            // In a better implementation, we'd interpolate between APs
            let closestAP = 0;
            let minDist = Infinity;
            for (let i = 0; i < aps.length; i++) {
                const dx = x - aps[i].x;
                const dy = y - aps[i].y;
                const dist = dx * dx + dy * dy;
                if (dist < minDist) {
                    minDist = dist;
                    closestAP = i;
                }
            }

            let validCount = 0;
            for (let f = 0; f < frames.length; f++) {
                const offset = offsetsMap[f][closestAP];
                const srcY = y + offset.y;
                const srcX = x + offset.x;

                if (srcY >= 0 && srcY < height && srcX >= 0 && srcX < width) {
                    pixelValues[validCount++] = frames[f].data[srcY * width + srcX];
                }
            }

            if (validCount === 0) continue;

            let sum = 0;
            for (let i = 0; i < validCount; i++) sum += pixelValues[i];
            let mean = sum / validCount;

            if (sigma > 0 && validCount > 2) {
                let sqDiffSum = 0;
                for (let i = 0; i < validCount; i++) sqDiffSum += (pixelValues[i] - mean) ** 2;
                const stdDev = Math.sqrt(sqDiffSum / validCount);
                let clippedSum = 0, clippedCount = 0;
                for (let i = 0; i < validCount; i++) {
                    if (Math.abs(pixelValues[i] - mean) <= sigma * stdDev) {
                        clippedSum += pixelValues[i];
                        clippedCount++;
                    }
                }
                if (clippedCount > 0) mean = clippedSum / clippedCount;
            }
            result[y * width + x] = mean;
        }
    }
    return result;
}
