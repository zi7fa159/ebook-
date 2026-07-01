export function stackFramesSigmaClipping(frames, offsets, sigma = 3) {
    const { width, height } = frames[0].metadata;
    const result = new Float32Array(width * height);
    // Reuse this array to avoid massive allocations in the loop
    const pixelValues = new Float32Array(frames.length);

    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            let validCount = 0;
            for (let f = 0; f < frames.length; f++) {
                const offset = offsets[f];
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
                for (let i = 0; i < validCount; i++) {
                    const diff = pixelValues[i] - mean;
                    sqDiffSum += diff * diff;
                }
                const stdDev = Math.sqrt(sqDiffSum / validCount);

                let clippedSum = 0;
                let clippedCount = 0;
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
