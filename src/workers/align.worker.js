import { alignFrames } from '../pipeline/registration.js';

self.onmessage = async (e) => {
    const { type, refData, srcData, width, height, frameIndex } = e.data;

    if (type === 'align') {
        const offset = alignFrames(refData, srcData, width, height);
        self.postMessage({
            type: 'result',
            frameIndex,
            offset,
            srcData // Return the buffer back
        }, [srcData.buffer]);
    }
};
