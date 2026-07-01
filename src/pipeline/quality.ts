export interface QualityResult {
    score: number;
    metrics: Record<string, number>;
}

export function varianceOfLaplacian(data: Uint8Array | Uint16Array, width: number, height: number): number {
    // Standard Laplacian kernel:
    //  0  1  0
    //  1 -4  1
    //  0  1  0

    let sum = 0;
    let sumSq = 0;
    const count = (width - 2) * (height - 2);

    for (let y = 1; y < height - 1; y++) {
        for (let x = 1; x < width - 1; x++) {
            const idx = y * width + x;
            const laplacian =
                data[idx - 1] +
                data[idx + 1] +
                data[idx - width] +
                data[idx + width] -
                4 * data[idx];

            sum += laplacian;
            sumSq += laplacian * laplacian;
        }
    }

    const mean = sum / count;
    const variance = (sumSq / count) - (mean * mean);
    return variance;
}

export function estimateSNR(data: Uint8Array | Uint16Array, width: number, height: number): number {
    // Very simple SNR estimation based on mean and standard deviation of a central patch
    const patchSize = Math.min(width, height, 100);
    const startX = Math.floor((width - patchSize) / 2);
    const startY = Math.floor((height - patchSize) / 2);

    let sum = 0;
    let sumSq = 0;
    const count = patchSize * patchSize;

    for (let y = startY; y < startY + patchSize; y++) {
        for (let x = startX; x < startX + patchSize; x++) {
            const val = data[y * width + x];
            sum += val;
            sumSq += val * val;
        }
    }

    const mean = sum / count;
    const variance = (sumSq / count) - (mean * mean);
    const stdDev = Math.sqrt(Math.max(0, variance));

    return stdDev === 0 ? 0 : mean / stdDev;
}
