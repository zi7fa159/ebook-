/**
 * Export utilities for images.
 */
export class Exporter {
    /**
     * Export image as PNG.
     */
    static async toPNG(image) {
        const canvas = document.createElement('canvas');
        canvas.width = image.width;
        canvas.height = image.height;
        const ctx = canvas.getContext('2d');
        const imageData = ctx.createImageData(image.width, image.height);

        const data = image.data;
        const channels = image.channels;

        for (let i = 0; i < image.width * image.height; i++) {
            const idx = i * 4;
            if (channels === 1) {
                const val = Math.floor(Math.min(Math.max(data[i], 0), 1) * 255);
                imageData.data[idx] = val;
                imageData.data[idx+1] = val;
                imageData.data[idx+2] = val;
                imageData.data[idx+3] = 255;
            } else {
                const sIdx = i * 3;
                imageData.data[idx] = Math.floor(Math.min(Math.max(data[sIdx], 0), 1) * 255);
                imageData.data[idx+1] = Math.floor(Math.min(Math.max(data[sIdx+1], 0), 1) * 255);
                imageData.data[idx+2] = Math.floor(Math.min(Math.max(data[sIdx+2], 0), 1) * 255);
                imageData.data[idx+3] = 255;
            }
        }

        ctx.putImageData(imageData, 0, 0);
        return new Promise(resolve => canvas.toBlob(resolve, 'image/png'));
    }

    /**
     * Download blob as file.
     */
    static download(blob, filename) {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }
}
