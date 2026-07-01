import { Registration } from '../core/Registration.js';
import { QualityEstimators } from '../core/QualityEstimators.js';

self.onmessage = async (e) => {
    const { type, frameData, width, height, channels, frameIndex, refFrame } = e.data;

    if (type === 'analyze') {
        const frame = { data: frameData, width, height, channels };
        const score = QualityEstimators.varianceOfLaplacian(frame);
        const contrast = QualityEstimators.rmsContrast(frame);
        self.postMessage({ type: 'result', frameIndex, score, contrast });
    } else if (type === 'register') {
        const frame = { data: frameData, width, height, channels };
        const reference = { data: refFrame.data, width: refFrame.width, height: refFrame.height, channels: refFrame.channels };

        // Global alignment
        const globalAlignment = Registration.findTranslationFFT(reference, frame);

        // AP-based local alignment
        const apResults = [];
        if (e.data.aps) {
            for (const ap of e.data.aps) {
                // Find translation for this specific patch
                const patchRef = getPatch(reference, ap.x, ap.y, ap.size);
                const patchTarget = getPatch(frame, ap.x + globalAlignment.dx, ap.y + globalAlignment.dy, ap.size);

                const localShift = Registration.findTranslation(patchRef, patchTarget, 10);
                apResults.push({
                    x: ap.x,
                    y: ap.y,
                    dx: globalAlignment.dx + localShift.dx,
                    dy: globalAlignment.dy + localShift.dy
                });
            }
        }

        self.postMessage({ type: 'registration_result', frameIndex, alignment: globalAlignment, aps: apResults });
    }
};

function getPatch(frame, cx, cy, size) {
    const r = Math.floor(size / 2);
    const data = new Float32Array(size * size * frame.channels);
    for (let y = 0; y < size; y++) {
        for (let x = 0; x < size; x++) {
            const sx = Math.min(frame.width - 1, Math.max(0, cx - r + x));
            const sy = Math.min(frame.height - 1, Math.max(0, cy - r + y));
            for (let c = 0; c < frame.channels; c++) {
                data[(y * size + x) * frame.channels + c] = frame.data[(sy * frame.width + sx) * frame.channels + c];
            }
        }
    }
    return { data, width: size, height: size, channels: frame.channels };
}
