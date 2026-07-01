import { describe, it, expect } from 'vitest';
import { FITSParser } from '../src/io/fits-parser.js';

function createMockFITS(width, height, bitpix) {
    const cards = [
        `SIMPLE  =                    T / fits standard`,
        `BITPIX  = ${bitpix.toString().padStart(20)} / bits per data value`,
        `NAXIS   =                    2 / number of data axes`,
        `NAXIS1  = ${width.toString().padStart(20)} / length of data axis 1`,
        `NAXIS2  = ${height.toString().padStart(20)} / length of data axis 2`,
        `END                                                                             `
    ];

    const header = new Uint8Array(2880);
    const encoder = new TextEncoder();
    for (let i = 0; i < cards.length; i++) {
        header.set(encoder.encode(cards[i].padEnd(80)), i * 80);
    }

    const numPixels = width * height;
    const bytesPerPixel = Math.abs(bitpix) / 8;
    const data = new Uint8Array(numPixels * bytesPerPixel);
    // Fill with some data if needed

    return new Blob([header, data]);
}

describe('FITSParser', () => {
    it('should parse mock FITS header', async () => {
        const blob = createMockFITS(100, 100, 16);
        const parser = new FITSParser(blob);
        const header = await parser.readHeader();

        expect(header['NAXIS1']).toBe('100');
        expect(header['NAXIS2']).toBe('100');
        expect(header['BITPIX']).toBe('16');
    });

    it('should extract image data', async () => {
        const blob = createMockFITS(10, 10, 8);
        const parser = new FITSParser(blob);
        const image = await parser.getImage();

        expect(image.width).toBe(10);
        expect(image.height).toBe(10);
        expect(image.data.length).toBe(100);
    });
});
