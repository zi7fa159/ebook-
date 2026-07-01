import { PixelFormat } from '../core/types.js';

export function debayer(data, width, height, format) {
    const channels = 3;
    const result = new Uint8ClampedArray(width * height * 4); // Use RGBA for canvas compatibility

    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            const idx = y * width + x;
            const destIdx = idx * 4;

            let r = 0, g = 0, b = 0;

            if (format === PixelFormat.BayerRGGB) {
                if (y % 2 === 0) {
                    if (x % 2 === 0) { // Red pixel
                        r = data[idx];
                        g = (getSafe(data, x+1, y, width, height) + getSafe(data, x, y+1, width, height)) / 2;
                        b = getSafe(data, x+1, y+1, width, height);
                    } else { // Green pixel
                        r = (getSafe(data, x-1, y, width, height) + getSafe(data, x+1, y, width, height)) / 2;
                        g = data[idx];
                        b = (getSafe(data, x, y+1, width, height) + getSafe(data, x, y-1, width, height)) / 2;
                    }
                } else {
                    if (x % 2 === 0) { // Green pixel
                        r = (getSafe(data, x, y-1, width, height) + getSafe(data, x, y+1, width, height)) / 2;
                        g = data[idx];
                        b = (getSafe(data, x-1, y, width, height) + getSafe(data, x+1, y, width, height)) / 2;
                    } else { // Blue pixel
                        r = getSafe(data, x-1, y-1, width, height);
                        g = (getSafe(data, x-1, y, width, height) + getSafe(data, x, y-1, width, height)) / 2;
                        b = data[idx];
                    }
                }
            } else {
                r = g = b = data[idx];
            }

            result[destIdx] = r;
            result[destIdx + 1] = g;
            result[destIdx + 2] = b;
            result[destIdx + 3] = 255;
        }
    }
    return result;
}

function getSafe(data, x, y, width, height) {
    if (x < 0 || x >= width || y < 0 || y >= height) return 0;
    return data[y * width + x];
}
