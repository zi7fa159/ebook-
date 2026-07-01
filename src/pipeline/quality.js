export function varianceOfLaplacian(data, width, height) {
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
