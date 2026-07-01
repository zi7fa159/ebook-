import { describe, it, expect } from 'vitest';
import { stackFrames, stackFramesSigmaClipping } from '../../src/pipeline/stacking';
import { PixelFormat, Endianness } from '../../src/core/types';

describe('Stacking', () => {
    it('should stack two identical frames with no offset', () => {
        const frame1 = {
            data: new Uint8Array([10, 20, 30, 40]),
            metadata: { width: 2, height: 2, bitDepth: 8, pixelFormat: PixelFormat.Mono, endianness: Endianness.Little }
        };
        const frame2 = {
            data: new Uint8Array([10, 20, 30, 40]),
            metadata: { width: 2, height: 2, bitDepth: 8, pixelFormat: PixelFormat.Mono, endianness: Endianness.Little }
        };
        const offsets = [{ x: 0, y: 0 }, { x: 0, y: 0 }];
        const result = stackFrames([frame1, frame2], offsets);

        expect(result[0]).toBe(10);
        expect(result[1]).toBe(20);
        expect(result[2]).toBe(30);
        expect(result[3]).toBe(40);
    });

    it('should handle offsets correctly during stacking', () => {
        const frame1 = {
            data: new Uint8Array([
                1, 2,
                3, 4
            ]),
            metadata: { width: 2, height: 2, bitDepth: 8, pixelFormat: PixelFormat.Mono, endianness: Endianness.Little }
        };
        const frame2 = {
            data: new Uint8Array([
                0, 0,
                0, 5
            ]),
            metadata: { width: 2, height: 2, bitDepth: 8, pixelFormat: PixelFormat.Mono, endianness: Endianness.Little }
        };
        // frame2 is shifted such that its (1,1) is at frame1's (0,0)
        // No, my implementation: srcX = x + offset.x.
        // If offset.x = 1, then at x=0, srcX=1.
        // So frame1(0,0) += frame2(1,0)
        const offsets = [{ x: 0, y: 0 }, { x: 1, y: 1 }];
        const result = stackFrames([frame1, frame2], offsets);

        // result(0,0) = (frame1(0,0) + frame2(1,1)) / 2 = (1 + 5) / 2 = 3
        expect(result[0]).toBe(3);
    });
});
