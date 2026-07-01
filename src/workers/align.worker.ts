import { alignFrames } from '../pipeline/registration';

self.onmessage = async (e: MessageEvent) => {
    const { type, refData, srcData, width, height, frameIndex } = e.data;

    if (type === 'align') {
        const offset = alignFrames(refData, srcData, width, height);
        self.postMessage({
            type: 'result',
            frameIndex,
            offset
        }, [srcData.buffer]);
    }
};
