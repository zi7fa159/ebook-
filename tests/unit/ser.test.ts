import { describe, it, expect, vi } from 'vitest';
import { SERParser } from '../../src/io/ser';
import { Endianness, PixelFormat } from '../../src/core/types';

describe('SERParser', () => {
  it('should correctly parse a mock SER header', async () => {
    const buffer = new ArrayBuffer(178);
    const view = new DataView(buffer);

    // FileID: LUCAM-RECORDER
    const fileID = 'LUCAM-RECORDER';
    for (let i = 0; i < fileID.length; i++) {
      view.setUint8(i, fileID.charCodeAt(i));
    }

    view.setUint32(14, 0, true); // luID
    view.setUint32(18, 0, true); // colorID (Mono)
    view.setUint32(22, 0, true); // littleEndian
    view.setUint32(26, 640, true); // width
    view.setUint32(30, 480, true); // height
    view.setUint32(34, 8, true); // bitDepth
    view.setUint32(38, 100, true); // frameCount

    const mockFile = new File([buffer], 'test.ser');
    const parser = new SERParser(mockFile);
    const header = await parser.parseHeader();

    expect(header.width).toBe(640);
    expect(header.height).toBe(480);
    expect(header.bitDepth).toBe(8);
    expect(header.frameCount).toBe(100);
    expect(header.pixelFormat).toBe(PixelFormat.Mono);
    expect(header.endianness).toBe(Endianness.Little);
  });
});
