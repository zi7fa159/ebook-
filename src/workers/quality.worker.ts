import { varianceOfLaplacian } from '../pipeline/quality';

self.onmessage = async (e: MessageEvent) => {
    const { type, data, width, height, frameIndex } = e.data;

    if (type === 'analyze') {
        const score = varianceOfLaplacian(data, width, height);
        self.postMessage({
            type: 'result',
            frameIndex,
            score
        }, [data.buffer]);
    }
};
