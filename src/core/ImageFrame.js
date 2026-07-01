/**
 * Internal Image representation for processing.
 * We use Float32Array for all internal operations to maintain precision.
 */
export class ImageFrame {
    constructor(width, height, channels = 1) {
        this.width = width;
        this.height = height;
        this.channels = channels;
        this.data = new Float32Array(width * height * channels);
    }

    static fromSER(rawData, metadata) {
        const { imageWidth, imageHeight, pixelDepthPerPlane, colorID } = metadata;
        let channels = 1;
        if (colorID >= 100) channels = 3;

        const frame = new ImageFrame(imageWidth, imageHeight, channels);
        const maxVal = (1 << pixelDepthPerPlane) - 1;

        for (let i = 0; i < rawData.length; i++) {
            frame.data[i] = rawData[i] / maxVal;
        }

        return frame;
    }
}
