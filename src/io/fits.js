import { PixelFormat, Endianness } from '../core/types.js';

export class FITSParser {
    constructor(file) {
        this.file = file;
        this.header = null;
    }

    async parseHeader() {
        const buffer = await this.file.slice(0, 28800).arrayBuffer(); // Read first 10 blocks
        const decoder = new TextDecoder();
        let headerStr = "";
        let pos = 0;

        while (pos < buffer.byteLength) {
            const block = decoder.decode(new Uint8Array(buffer.slice(pos, pos + 2880)));
            headerStr += block;
            if (block.includes("END     ")) break;
            pos += 2880;
        }

        const lines = headerStr.match(/.{80}/g);
        const headerMap = {};
        for (const line of lines) {
            const [key, valPart] = line.split("=");
            if (key && valPart) {
                const cleanKey = key.trim();
                const val = valPart.split("/")[0].trim().replace(/^'|'$/g, "");
                headerMap[cleanKey] = val;
            }
            if (line.startsWith("END     ")) break;
        }

        const bitpix = parseInt(headerMap["BITPIX"]);
        const naxis = parseInt(headerMap["NAXIS"]);
        const width = parseInt(headerMap["NAXIS1"] || 0);
        const height = parseInt(headerMap["NAXIS2"] || 0);
        const depth = parseInt(headerMap["NAXIS3"] || 1);

        this.header = {
            bitpix,
            naxis,
            width,
            height,
            depth,
            dataStart: pos + 2880 // Rough estimate, should find next block start
        };

        return this.header;
    }

    async getFrame(index) {
        if (!this.header) await this.parseHeader();
        const h = this.header;

        const bytesPerPixel = Math.abs(h.bitpix) / 8;
        const frameSize = h.width * h.height * bytesPerPixel;
        const offset = h.dataStart + index * frameSize;

        const buffer = await this.file.slice(offset, offset + frameSize).arrayBuffer();
        const view = new DataView(buffer);

        let data;
        if (h.bitpix === 8) {
            data = new Uint8Array(buffer);
        } else if (h.bitpix === 16) {
            data = new Uint16Array(h.width * h.height);
            for (let i = 0; i < data.length; i++) data[i] = view.getInt16(i * 2, false) + 32768; // Convert signed to unsigned
        } else if (h.bitpix === 32) {
            data = new Float32Array(h.width * h.height);
            for (let i = 0; i < data.length; i++) data[i] = view.getInt32(i * 4, false);
        } else if (h.bitpix === -32) {
            data = new Float32Array(h.width * h.height);
            for (let i = 0; i < data.length; i++) data[i] = view.getFloat32(i * 4, false);
        }

        return {
            data,
            metadata: {
                width: h.width,
                height: h.height,
                bitDepth: Math.abs(h.bitpix),
                pixelFormat: PixelFormat.Mono,
                endianness: Endianness.Big
            }
        };
    }
}
