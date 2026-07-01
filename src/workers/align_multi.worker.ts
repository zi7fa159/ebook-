import { getLocalShift } from '../pipeline/multipoint';

self.onmessage = async (e: MessageEvent) => {
    const { type, refData, srcData, width, height, aps } = e.data;

    if (type === 'align_multi') {
        const shifts = aps.map((ap: any) => getLocalShift(refData, srcData, width, height, ap));
        self.postMessage({
            type: 'result',
            shifts
        });
    }
};
