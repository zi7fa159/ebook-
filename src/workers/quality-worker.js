import { QualityEstimators } from '../utils/quality.js';

self.onmessage = async (e) => {
    const { type, frameData, width, height, id } = e.data;

    if (type === 'ANALYZE_QUALITY') {
        const laplacianVar = QualityEstimators.varianceOfLaplacian(frameData, width, height);
        const rms = QualityEstimators.rmsContrast(frameData);

        // Combine scores (simplified for now)
        const score = laplacianVar * rms;

        self.postMessage({
            type: 'QUALITY_RESULT',
            id,
            score,
            details: {
                laplacianVar,
                rms
            }
        });
    }
};
