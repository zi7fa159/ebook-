/**
 * Basic FITS (Flexible Image Transport System) Reader
 */
export class FITSParser {
    constructor(file) {
        this.file = file;
        this.header = {};
        this.dataOffset = 0;
    }

    async parse() {
        let offset = 0;
        let readingHeader = true;

        while (readingHeader) {
            const buffer = await this.file.slice(offset, offset + 2880).arrayBuffer();
            const view = new Uint8Array(buffer);

            for (let i = 0; i < 2880; i += 80) {
                const line = new TextDecoder().decode(view.slice(i, i + 80));
                const key = line.slice(0, 8).trim();
                const value = line.slice(10).split('/')[0].trim();

                if (key === 'END') {
                    readingHeader = false;
                    break;
                }
                if (key) {
                    this.header[key] = value;
                }
            }
            offset += 2880;
        }

        this.dataOffset = offset;
        this.width = parseInt(this.header['NAXIS1']);
        this.height = parseInt(this.header['NAXIS2']);
        this.bitpix = parseInt(this.header['BITPIX']);
    }

    async getImageData() {
        const size = this.width * this.height;
        const bytesPerPixel = Math.abs(this.bitpix) / 8;
        const buffer = await this.file.slice(this.dataOffset, this.dataOffset + size * bytesPerPixel).arrayBuffer();
        const view = new DataView(buffer);
        const data = new Float32Array(size);

        if (this.bitpix === 16) {
            for (let i = 0; i < size; i++) {
                data[i] = view.getInt16(i * 2, false) / 32768 + 1; // FITS is Big Endian
            }
        } else if (this.bitpix === -32) {
            for (let i = 0; i < size; i++) {
                data[i] = view.getFloat32(i * 4, false);
            }
        } else if (this.bitpix === 8) {
            const u8 = new Uint8Array(buffer);
            for (let i = 0; i < size; i++) {
                data[i] = u8[i] / 255;
            }
        }

        return {
            width: this.width,
            height: this.height,
            channels: 1,
            data: data
        };
    }
}
