import { describe, it, expect } from 'vitest';
import { SERParser, COLOR_ID } from '../../src/io/ser-parser.js';

function createMockSER(width, height, frameCount, pixelDepth, colorId) {
    const headerSize = 178;
    const bytesPerPixel = pixelDepth <= 8 ? 1 : 2;
    const planes = (colorId === 100 || colorId === 101) ? 3 : 1;
    const frameSize = width * height * bytesPerPixel * planes;
    const totalSize = headerSize + (frameCount * frameSize);
    const buffer = new ArrayBuffer(totalSize);
    const view = new DataView(buffer);

    // Header ID
    const encoder = new TextEncoder();
    const id = encoder.encode('LUCAM-RECORDER');
    new Uint8Array(buffer, 0, 14).set(id);

    view.setInt32(14, 0, true); // luId
    view.setInt32(18, colorId, true); // colorId
    view.setInt32(22, 1, true); // littleEndian
    view.setInt32(26, width, true);
    view.setInt32(30, height, true);
    view.setInt32(34, pixelDepth, true);
    view.setInt32(38, frameCount, true);

    // Fill some pixel data
    const maxVal = (1 << pixelDepth) - 1;
    if (bytesPerPixel === 1) {
        const pixels = new Uint8Array(buffer, headerSize);
        pixels.fill(128);
    } else {
        const pixels = new Uint16Array(buffer, headerSize);
        pixels.fill(maxVal / 2);
    }

    return new Blob([buffer]);
}

describe('SERParser', () => {
    it('should correctly parse a mock 8-bit mono SER file header', async () => {
        const blob = createMockSER(640, 480, 10, 8, COLOR_ID.MONO);
        const parser = new SERParser(blob);
        const header = await parser.readHeader();

        expect(header.width).toBe(640);
        expect(header.height).toBe(480);
        expect(header.frameCount).toBe(10);
        expect(header.pixelDepth).toBe(8);
        expect(header.colorId).toBe(COLOR_ID.MONO);
    });

    it('should extract normalized frame data', async () => {
        const blob = createMockSER(100, 100, 2, 16, COLOR_ID.MONO);
        const parser = new SERParser(blob);
        const frame = await parser.getFrame(0);

        expect(frame.width).toBe(100);
        expect(frame.height).toBe(100);
        expect(frame.data.length).toBe(10000);
        // 32767 / 65535 approx 0.499992
        expect(frame.data[0]).toBeCloseTo(0.499992, 5);
    });
});
