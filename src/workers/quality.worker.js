import { varianceOfLaplacian } from '../pipeline/quality.js';

self.onmessage = async (e) => {
    const { type, data, width, height, frameIndex } = e.data;

    if (type === 'analyze') {
        const score = varianceOfLaplacian(data, width, height);
        self.postMessage({
            type: 'result',
            frameIndex,
            score,
            data // Return the buffer back
        }, [data.buffer]);
    }
};
