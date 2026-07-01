/**
 * FITS file format reader.
 * Reference: https://fits.gsfc.nasa.gov/fits_standard.html
 */
export class FITSParser {
    constructor(file) {
        this.file = file;
        this.header = {};
        this.dataOffset = 0;
    }

    async readHeader() {
        let offset = 0;
        let finished = false;

        while (!finished) {
            const buffer = await this.file.slice(offset, offset + 2880).arrayBuffer();
            const view = new Uint8Array(buffer);

            for (let i = 0; i < 36; i++) {
                const card = new TextDecoder().decode(view.slice(i * 80, (i + 1) * 80));
                const key = card.slice(0, 8).trim();
                const valuePart = card.slice(10).split('/')[0].trim();

                if (key === 'END') {
                    finished = true;
                    break;
                }

                if (key) {
                    this.header[key] = valuePart.replace(/'/g, '').trim();
                }
            }
            offset += 2880;
        }

        this.dataOffset = offset;
        return this.header;
    }

    async getImage() {
        if (Object.keys(this.header).length === 0) await this.readHeader();

        const bitpix = parseInt(this.header['BITPIX']);
        const naxis1 = parseInt(this.header['NAXIS1']);
        const naxis2 = parseInt(this.header['NAXIS2']);
        const naxis = parseInt(this.header['NAXIS']);

        if (naxis < 2) throw new Error('FITS must have at least 2 dimensions');

        const numPixels = naxis1 * naxis2;
        let bytesPerPixel = Math.abs(bitpix) / 8;
        const dataSize = numPixels * bytesPerPixel;

        const buffer = await this.file.slice(this.dataOffset, this.dataOffset + dataSize).arrayBuffer();
        const view = new DataView(buffer);
        const data = new Float32Array(numPixels);

        for (let i = 0; i < numPixels; i++) {
            let val;
            if (bitpix === 8) val = view.getUint8(i);
            else if (bitpix === 16) val = view.getInt16(i * 2, false); // Big-endian
            else if (bitpix === 32) val = view.getInt32(i * 4, false);
            else if (bitpix === -32) val = view.getFloat32(i * 4, false);
            else if (bitpix === -64) val = view.getFloat64(i * 8, false);

            // Basic normalization (depends on BZERO/BSCALE in production)
            const bscale = parseFloat(this.header['BSCALE'] || 1.0);
            const bzero = parseFloat(this.header['BZERO'] || 0.0);
            data[i] = val * bscale + bzero;
        }

        // Normalize to 0..1 for internal pipeline
        let min = Infinity, max = -Infinity;
        for(let i=0; i<data.length; i++) {
            if(data[i] < min) min = data[i];
            if(data[i] > max) max = data[i];
        }
        const range = max - min;
        if(range > 0) {
            for(let i=0; i<data.length; i++) data[i] = (data[i] - min) / range;
        }

        return {
            width: naxis1,
            height: naxis2,
            data,
            channels: 1
        };
    }
}
