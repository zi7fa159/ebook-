/**
 * SER File Format Parser
 *
 * SER files are a common format in planetary imaging.
 * Format spec: http://www.grischa-hahn.de/astro/ser/SER%20Doc%20v3.pdf
 */

export class SERParser {
    constructor(file) {
        this.file = file;
        this.metadata = null;
        this.headerSize = 178;
    }

    async parseHeader() {
        const buffer = await this.file.slice(0, this.headerSize).arrayBuffer();
        const view = new DataView(buffer);

        // Header is Little Endian
        const fileID = new TextDecoder().decode(buffer.slice(0, 14)).trim();
        if (fileID !== 'LUCAM-RECORDER') {
            throw new Error('Invalid SER file header');
        }

        const luID = view.getUint32(14, true);
        const colorID = view.getUint32(18, true);
        const littleEndian = view.getUint32(22, true) === 0; // 0 = Little Endian, 1 = Big Endian
        const imageWidth = view.getUint32(26, true);
        const imageHeight = view.getUint32(30, true);
        const pixelDepthPerPlane = view.getUint32(34, true);
        const frameCount = view.getUint32(38, true);

        const observer = new TextDecoder().decode(buffer.slice(42, 82)).trim();
        const instrument = new TextDecoder().decode(buffer.slice(82, 122)).trim();
        const telescope = new TextDecoder().decode(buffer.slice(122, 162)).trim();

        const dateTime = view.getBigUint64(162, true);
        const dateTime_UTC = view.getBigUint64(170, true);

        this.metadata = {
            luID,
            colorID,
            littleEndian,
            imageWidth,
            imageHeight,
            pixelDepthPerPlane,
            frameCount,
            observer,
            instrument,
            telescope,
            dateTime,
            dateTime_UTC
        };

        this.validate();
    }

    validate() {
        const m = this.metadata;
        const bytesPerPixel = Math.ceil(m.pixelDepthPerPlane / 8);
        let planes = 1;

        // ColorID: 0=Mono, 8=Bayer RGGB, 9=GRBG, 10=GBRG, 11=BGGR, 100=RGB, 101=BGR
        if (m.colorID >= 100) {
            planes = 3;
        }

        this.bytesPerFrame = m.imageWidth * m.imageHeight * bytesPerPixel * planes;
        this.totalFramesSize = this.bytesPerFrame * m.frameCount;

        // Trailer starts after frames
        this.trailerOffset = this.headerSize + this.totalFramesSize;
    }

    async getFrame(frameIndex) {
        if (frameIndex < 0 || frameIndex >= this.metadata.frameCount) {
            throw new Error(`Frame index ${frameIndex} out of bounds`);
        }

        const start = this.headerSize + (frameIndex * this.bytesPerFrame);
        const end = start + this.bytesPerFrame;
        const buffer = await this.file.slice(start, end).arrayBuffer();

        return this.processFrameBuffer(buffer);
    }

    processFrameBuffer(buffer) {
        const m = this.metadata;
        const pixelDepth = m.pixelDepthPerPlane;
        const littleEndian = m.littleEndian;

        if (pixelDepth <= 8) {
            return new Uint8Array(buffer);
        } else {
            // SER 16-bit is usually stored as 16-bit but can be 10, 12, 14 bit
            // We return Uint16Array
            const u16 = new Uint16Array(buffer.byteLength / 2);
            const view = new DataView(buffer);
            for (let i = 0; i < u16.length; i++) {
                u16[i] = view.getUint16(i * 2, littleEndian);
            }
            return u16;
        }
    }
}
