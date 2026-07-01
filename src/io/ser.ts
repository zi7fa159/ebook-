import { PixelFormat, Endianness, ImageMetadata, Frame } from '../core/types';

export class SERParser {
  private file: File;
  private header: SERHeader | null = null;

  constructor(file: File) {
    this.file = file;
  }

  async parseHeader(): Promise<SERHeader> {
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

  private readNullTerminatedString(bytes: Uint8Array): string {
    const end = bytes.indexOf(0);
    return new TextDecoder().decode(bytes.slice(0, end === -1 ? undefined : end));
  }

  private mapColorID(colorID: number): PixelFormat {
    switch (colorID) {
      case 0: return PixelFormat.Mono;
      case 8: return PixelFormat.BayerRGGB;
      case 9: return PixelFormat.BayerGRBG;
      case 10: return PixelFormat.BayerGBRG;
      case 11: return PixelFormat.BayerBGGR;
      case 100: return PixelFormat.RGB;
      case 101: return PixelFormat.RGB; // BGR -> convert or handle?
      default: return PixelFormat.Mono;
    }
  }

  async getFrame(index: number): Promise<Frame> {
    if (!this.header) await this.parseHeader();
    const h = this.header!;

    if (index < 0 || index >= h.frameCount) {
      throw new Error(`Frame index ${index} out of bounds (0-${h.frameCount - 1})`);
    }

    const bytesPerPixel = h.bitDepth > 8 ? 2 : 1;
    const channels = h.pixelFormat === PixelFormat.RGB ? 3 : 1;
    const frameSize = h.width * h.height * bytesPerPixel * channels;
    const offset = 178 + index * frameSize;

    const buffer = await this.file.slice(offset, offset + frameSize).arrayBuffer();

    let data: Uint8Array | Uint16Array;
    if (h.bitDepth <= 8) {
        data = new Uint8Array(buffer);
    } else {
        data = new Uint16Array(buffer);
        if (h.endianness === Endianness.Big) {
            // Swap bytes
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

export interface SERHeader {
  luID: number;
  colorID: number;
  endianness: Endianness;
  width: number;
  height: number;
  bitDepth: number;
  frameCount: number;
  observer: string;
  instrument: string;
  telescope: string;
  dateUTC: bigint;
  dateLocal: bigint;
  pixelFormat: PixelFormat;
}
