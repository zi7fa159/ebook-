import { PixelFormat, Endianness } from '../core/types.js';

export class SERParser {
  constructor(file) {
    this.file = file;
    this.header = null;
  }

  async parseHeader() {
    const buffer = await this.file.slice(0, 178).arrayBuffer();
    const view = new DataView(buffer);

    const fileID = String.fromCharCode(...new Uint8Array(buffer.slice(0, 14)));
    if (fileID !== 'LUCAM-RECORDER') {
      throw new Error('Invalid SER file: Missing LUCAM-RECORDER header');
    }

    const luID = view.getUint32(14, true);
    const colorID = view.getUint32(18, true);
    const littleEndian = view.getUint32(22, true) === 0;
    const width = view.getUint32(26, true);
    const height = view.getUint32(30, true);
    const bitDepth = view.getUint32(34, true);
    const frameCount = view.getUint32(38, true);

    const observer = this.readNullTerminatedString(new Uint8Array(buffer.slice(42, 82)));
    const instrument = this.readNullTerminatedString(new Uint8Array(buffer.slice(82, 122)));
    const telescope = this.readNullTerminatedString(new Uint8Array(buffer.slice(122, 162)));

    const dateUTC = view.getBigUint64(162, true);
    const dateLocal = view.getBigUint64(170, true);

    this.header = {
      luID,
      colorID,
      endianness: littleEndian ? Endianness.Little : Endianness.Big,
      width,
      height,
      bitDepth,
      frameCount,
      observer,
      instrument,
      telescope,
      dateUTC,
      dateLocal,
      pixelFormat: this.mapColorID(colorID),
    };

    return this.header;
  }

  readNullTerminatedString(bytes) {
    const end = bytes.indexOf(0);
    return new TextDecoder().decode(bytes.slice(0, end === -1 ? undefined : end));
  }

  mapColorID(colorID) {
    switch (colorID) {
      case 0: return PixelFormat.Mono;
      case 8: return PixelFormat.BayerRGGB;
      case 9: return PixelFormat.BayerGRBG;
      case 10: return PixelFormat.BayerGBRG;
      case 11: return PixelFormat.BayerBGGR;
      case 100: return PixelFormat.RGB;
      case 101: return PixelFormat.RGB;
      default: return PixelFormat.Mono;
    }
  }

  async getFrame(index) {
    if (!this.header) await this.parseHeader();
    const h = this.header;

    if (index < 0 || index >= h.frameCount) {
      throw new Error(`Frame index ${index} out of bounds`);
    }

    const bytesPerPixel = h.bitDepth > 8 ? 2 : 1;
    const channels = h.pixelFormat === PixelFormat.RGB ? 3 : 1;
    const frameSize = h.width * h.height * bytesPerPixel * channels;
    const offset = 178 + index * frameSize;

    const buffer = await this.file.slice(offset, offset + frameSize).arrayBuffer();

    let data;
    if (h.bitDepth <= 8) {
        data = new Uint8Array(buffer);
    } else {
        data = new Uint16Array(buffer);
        if (h.endianness === Endianness.Big) {
            const view = new DataView(buffer);
            for (let i = 0; i < data.length; i++) {
                data[i] = view.getUint16(i * 2, false);
            }
        }
    }

    return {
      data,
      metadata: {
        width: h.width,
        height: h.height,
        bitDepth: h.bitDepth,
        pixelFormat: h.pixelFormat,
        endianness: h.endianness,
      }
    };
  }
}
