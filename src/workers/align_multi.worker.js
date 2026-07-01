import { getLocalShift } from '../pipeline/multipoint.js';

self.onmessage = async (e) => {
    const { type, refData, srcData, width, height, aps } = e.data;

    if (type === 'align_multi') {
        const shifts = aps.map((ap) => getLocalShift(refData, srcData, width, height, ap));
        self.postMessage({
            type: 'result',
            shifts,
            srcData // Return the buffer
        }, [srcData.buffer]);
    }
};
