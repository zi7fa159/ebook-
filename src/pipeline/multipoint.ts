import { Point, AlignmentPoint } from '../core/types';

export function generateAPs(width: number, height: number, spacing: number, size: number): AlignmentPoint[] {
    const aps: AlignmentPoint[] = [];
    for (let y = spacing; y < height - spacing; y += spacing) {
        for (let x = spacing; x < width - spacing; x += spacing) {
            aps.push({
                x,
                y,
                size,
                weight: 1.0,
                searchRadius: size / 2
            });
        }
    }
    return aps;
}

export function getLocalShift(ref: Uint8Array | Uint16Array, src: Uint8Array | Uint16Array, width: number, height: number, ap: AlignmentPoint): Point {
    const halfSize = Math.floor(ap.size / 2);
    const searchRadius = ap.searchRadius;

    let maxCorr = -1;
    let bestX = 0;
    let bestY = 0;

    for (let dy = -searchRadius; dy <= searchRadius; dy++) {
        for (let dx = -searchRadius; dx <= searchRadius; dx++) {
            let corr = 0;
            let count = 0;
            for (let y = -halfSize; y < halfSize; y++) {
                const ry = ap.y + y;
                const sy = ap.y + y + dy;
                if (ry < 0 || ry >= height || sy < 0 || sy >= height) continue;

                for (let x = -halfSize; x < halfSize; x++) {
                    const rx = ap.x + x;
                    const sx = ap.x + x + dx;
                    if (rx < 0 || rx >= width || sx < 0 || sx >= width) continue;

                    const rVal = ref[ry * width + rx];
                    const sVal = src[sy * width + sx];
                    corr += Math.abs(rVal - sVal);
                    count++;
                }
            }
            if (count > 0 && (maxCorr === -1 || corr < maxCorr)) {
                maxCorr = corr;
                bestX = dx;
                bestY = dy;
            }
        }
    }

    return { x: bestX, y: bestY };
}
