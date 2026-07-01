import { PixelFormat } from '../core/types';

/**
 * Basic Debayering using bilinear interpolation
 */
export function debayer(data: Uint8Array | Uint16Array, width: number, height: number, format: PixelFormat): Uint8Array | Uint16Array {
    const channels = 3;
    const result = data instanceof Uint8Array ? new Uint8Array(width * height * channels) : new Uint16Array(width * height * channels);

    // Bilinear debayering
    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            const idx = y * width + x;
            const destIdx = idx * 3;

            // RGGB pattern example
            // R G
            // G B
            let r = 0, g = 0, b = 0;

            if (format === PixelFormat.BayerRGGB) {
                if (y % 2 === 0) {
                    if (x % 2 === 0) { // Red pixel
                        r = data[idx];
                        g = (getSafe(data, x+1, y, width, height) + getSafe(data, x, y+1, width, height)) / 2;
                        b = getSafe(data, x+1, y+1, width, height);
                    } else { // Green pixel (Row 0)
                        r = (getSafe(data, x-1, y, width, height) + getSafe(data, x+1, y, width, height)) / 2;
                        g = data[idx];
                        b = (getSafe(data, x, y+1, width, height) + getSafe(data, x, y-1, width, height)) / 2;
                    }
                } else {
                    if (x % 2 === 0) { // Green pixel (Row 1)
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
                // Fallback to mono if not RGGB for now, but in production we'd implement all 4
                r = g = b = data[idx];
            }

            result[destIdx] = r;
            result[destIdx + 1] = g;
            result[destIdx + 2] = b;
        }
    }
    return result;
}

function getSafe(data: Uint8Array | Uint16Array, x: number, y: number, width: number, height: number): number {
    if (x < 0 || x >= width || y < 0 || y >= height) return 0;
    return data[y * width + x];
}
