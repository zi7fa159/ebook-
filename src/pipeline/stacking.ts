import { Frame, PixelFormat } from '../core/types';

export function stackFrames(frames: Frame[], offsets: { x: number, y: number }[]): Float32Array {
    if (frames.length === 0) return new Float32Array(0);

    const { width, height } = frames[0].metadata;
    const accumulation = new Float32Array(width * height);
    const countMap = new Int32Array(width * height);

    for (let f = 0; f < frames.length; f++) {
        const frame = frames[f];
        const offset = offsets[f];
        const data = frame.data;

        for (let y = 0; y < height; y++) {
            const srcY = y + offset.y;
            if (srcY < 0 || srcY >= height) continue;

            for (let x = 0; x < width; x++) {
                const srcX = x + offset.x;
                if (srcX < 0 || srcX >= width) continue;

                const destIdx = y * width + x;
                const srcIdx = srcY * width + srcX;

                accumulation[destIdx] += data[srcIdx];
                countMap[destIdx]++;
            }
        }
    }

    for (let i = 0; i < accumulation.length; i++) {
        if (countMap[i] > 0) {
            accumulation[i] /= countMap[i];
        }
    }

    return accumulation;
}

export function stackFramesSigmaClipping(frames: Frame[], offsets: { x: number, y: number }[], sigma: number = 3): Float32Array {
    const { width, height } = frames[0].metadata;
    const result = new Float32Array(width * height);

    // For each pixel, collect all values, compute mean/stddev, clip, re-mean
    // This is slow on CPU but very robust.

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

            // Simple mean for now, sigma clipping would go here
            let sum = 0;
            for (let i = 0; i < validCount; i++) sum += pixelValues[i];
            let mean = sum / validCount;

            if (sigma > 0 && validCount > 2) {
                let sqDiffSum = 0;
                for (let i = 0; i < validCount; i++) sqDiffSum += (pixelValues[i] - mean) ** 2;
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
