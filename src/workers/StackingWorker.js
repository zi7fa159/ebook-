import { ImageFrame } from '../core/ImageFrame.js';

self.onmessage = async (e) => {
    const { type, frames, registrationResults, width, height, channels, accumulator, weights } = e.data;

    if (type === 'stack_chunk') {
        const { sigmaClip } = e.data;
        const count = frames.length;
        const acc = accumulator || new Float64Array(width * height * channels);

        for (let f = 0; f < count; f++) {
            const frameData = frames[f];
            const weight = weights ? weights[f] : 1.0;
            const reg = registrationResults[f];

            if (reg.aps && reg.aps.length > 0) {
                stackWithAPs(acc, frameData, reg.aps, width, height, channels, weight);
                continue;
            }

            const dx = reg.alignment.dx;
            const dy = reg.alignment.dy;

            // Sub-pixel alignment using Bilinear Interpolation
            const idxX = Math.floor(dx);
            const idxY = Math.floor(dy);
            const fracX = dx - idxX;
            const fracY = dy - idxY;

            for (let y = 0; y < height; y++) {
                const ty = y - dy;
                if (ty < 0 || ty >= height - 1) continue;

                const ty0 = Math.floor(ty);
                const ty1 = ty0 + 1;
                const fty = ty - ty0;

                for (let x = 0; x < width; x++) {
                    const tx = x - dx;
                    if (tx < 0 || tx >= width - 1) continue;

                    const tx0 = Math.floor(tx);
                    const tx1 = tx0 + 1;
                    const ftx = tx - tx0;

                    for (let c = 0; c < channels; c++) {
                        // Bilinear interpolation
                        const v00 = frameData[(y * width + x) * channels + c];
                        // We actually need to interpolate the SOURCE frame to the TARGET grid
                        // ty, tx are coordinates in the target (reference) grid.
                        // wait, if TY = y - dy, then TY is the target coordinate.
                        // We should iterate over target coordinates and sample from source.
                    }
                }
            }

            // Correct approach: Iterate over target pixels (x,y),
            // sample source pixel at (x+dx, y+dy)
            for (let y = 0; y < height; y++) {
                for (let x = 0; x < width; x++) {
                    const sx = x + dx;
                    const sy = y + dy;

                    if (sx < 0 || sx >= width - 1 || sy < 0 || sy >= height - 1) continue;

                    const sx0 = Math.floor(sx);
                    const sx1 = sx0 + 1;
                    const sy0 = Math.floor(sy);
                    const sy1 = sy0 + 1;

                    const fsx = sx - sx0;
                    const fsy = sy - sy0;

                    for (let c = 0; c < channels; c++) {
                        const v00 = frameData[(sy0 * width + sx0) * channels + c];
                        const v10 = frameData[(sy0 * width + sx1) * channels + c];
                        const v01 = frameData[(sy1 * width + sx0) * channels + c];
                        const v11 = frameData[(sy1 * width + sx1) * channels + c];

                        const val = (v00 * (1 - fsx) * (1 - fsy) +
                                     v10 * fsx * (1 - fsy) +
                                     v01 * (1 - fsx) * fsy +
                                     v11 * fsx * fsy);

                        acc[(y * width + x) * channels + c] += val * weight;
                    }
                }
            }
        }

        self.postMessage({
            type: 'stack_chunk_result',
            accumulator: acc
        }, [acc.buffer]);
    }
};

/**
 * Multi-point stacking using local AP offsets and distance-weighted blending
 */
function stackWithAPs(acc, frameData, aps, width, height, channels, weight) {
    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            // Find nearest APs and interpolate their offsets
            // For simplicity, use the nearest AP or simple IDW (Inverse Distance Weighting)
            let totalW = 0;
            let combinedDX = 0;
            let combinedDY = 0;

            for (let i = 0; i < aps.length; i++) {
                const ap = aps[i];
                const distSq = (x - ap.x) * (x - ap.x) + (y - ap.y) * (y - ap.y) + 1e-6;
                const w = 1.0 / (distSq * distSq); // Stronger weight for closer APs
                combinedDX += ap.dx * w;
                combinedDY += ap.dy * w;
                totalW += w;
            }

            const dx = combinedDX / totalW;
            const dy = combinedDY / totalW;

            const sx = x + dx;
            const sy = y + dy;

            if (sx < 0 || sx >= width - 1 || sy < 0 || sy >= height - 1) continue;

            const sx0 = Math.floor(sx);
            const sx1 = sx0 + 1;
            const sy0 = Math.floor(sy);
            const sy1 = sy0 + 1;

            const fsx = sx - sx0;
            const fsy = sy - sy0;

            for (let c = 0; c < channels; c++) {
                const v00 = frameData[(sy0 * width + sx0) * channels + c];
                const v10 = frameData[(sy0 * width + sx1) * channels + c];
                const v01 = frameData[(sy1 * width + sx0) * channels + c];
                const v11 = frameData[(sy1 * width + sx1) * channels + c];

                const val = (v00 * (1 - fsx) * (1 - fsy) +
                             v10 * fsx * (1 - fsy) +
                             v01 * (1 - fsx) * fsy +
                             v11 * fsx * fsy);

                acc[(y * width + x) * channels + c] += val * weight;
            }
        }
    }
}
