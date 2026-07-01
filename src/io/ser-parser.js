/**
 * SER file format parser for Lucky Imaging.
 * Reference: http://www.grischa-hahn.de.vu/astro/ser/SER%20Doc%20V3.pdf
 */

export const COLOR_ID = {
    MONO: 0,
    BAYER_RGGB: 8,
    BAYER_GRBG: 9,
    BAYER_GBRG: 10,
    BAYER_BGGR: 11,
    RGB: 100,
    BGR: 101
};

export class SERParser {
    constructor(file) {
        this.file = file;
        this.header = null;
        this.frameSize = 0;
        this.headerSize = 178;
    }

    async readHeader() {
        const buffer = await this.file.slice(0, this.headerSize).arrayBuffer();
        const view = new DataView(buffer);

        const fileId = new TextDecoder().decode(buffer.slice(0, 14));
        if (!fileId.startsWith('LUCAM-RECORDER')) {
            throw new Error('Invalid SER file: Header ID mismatch');
        }

        this.header = {
            fileId,
            luId: view.getInt32(14, true),
            colorId: view.getInt32(18, true),
            littleEndian: view.getInt32(22, true),
            width: view.getInt32(26, true),
            height: view.getInt32(30, true),
            pixelDepth: view.getInt32(34, true),
            frameCount: view.getInt32(38, true),
            observer: new TextDecoder().decode(buffer.slice(42, 82)).replace(/\0/g, '').trim(),
            instrument: new TextDecoder().decode(buffer.slice(82, 122)).replace(/\0/g, '').trim(),
            device: new TextDecoder().decode(buffer.slice(122, 162)).replace(/\0/g, '').trim(),
            date: view.getBigInt64(162, true),
            dateUT: view.getBigInt64(170, true)
        };

        const bytesPerPixel = this.header.pixelDepth <= 8 ? 1 : 2;
        const planes = (this.header.colorId === COLOR_ID.RGB || this.header.colorId === COLOR_ID.BGR) ? 3 : 1;
        this.frameSize = this.header.width * this.header.height * bytesPerPixel * planes;

        return this.header;
    }

    async getFrame(frameIndex) {
        if (!this.header) await this.readHeader();
        if (frameIndex < 0 || frameIndex >= this.header.frameCount) {
            throw new Error(`Frame index ${frameIndex} out of bounds (0-${this.header.frameCount - 1})`);
        }

        const offset = this.headerSize + (frameIndex * this.frameSize);
        const buffer = await this.file.slice(offset, offset + this.frameSize).arrayBuffer();

        return this.processPixelData(buffer);
    }

    processPixelData(buffer) {
        const { width, height, pixelDepth, colorId, littleEndian } = this.header;
        const bytesPerPixel = pixelDepth <= 8 ? 1 : 2;
        const planes = (colorId === COLOR_ID.RGB || colorId === COLOR_ID.BGR) ? 3 : 1;
        const totalPixels = width * height * planes;

        let data;
        if (bytesPerPixel === 1) {
            data = new Uint8Array(buffer);
        } else {
            // SER 16-bit can be big or little endian
            const isLittleEndian = littleEndian === 1;
            data = new Uint16Array(totalPixels);
            const view = new DataView(buffer);
            for (let i = 0; i < totalPixels; i++) {
                data[i] = view.getUint16(i * 2, isLittleEndian);
            }
        }

        // Convert to normalized Float32Array for internal pipeline
        const floatData = new Float32Array(totalPixels);
        const maxVal = (1 << pixelDepth) - 1;
        for (let i = 0; i < totalPixels; i++) {
            floatData[i] = data[i] / maxVal;
        }

        return {
            width,
            height,
            channels: planes,
            data: floatData,
            pixelDepth,
            colorId
        };
    }
}
