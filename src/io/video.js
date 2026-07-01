import { PixelFormat, Endianness } from '../core/types.js';

export class VideoFrameExtractor {
    constructor() {
        this.video = document.createElement('video');
        this.canvas = new OffscreenCanvas(1, 1);
        this.ctx = this.canvas.getContext('2d', { willReadFrequently: true });
    }

    async load(url) {
        return new Promise((resolve, reject) => {
            this.video.src = url;
            this.video.muted = true;
            this.video.preload = 'auto';
            this.video.onloadedmetadata = () => {
                this.canvas.width = this.video.videoWidth;
                this.canvas.height = this.video.videoHeight;
                resolve({
                    width: this.video.videoWidth,
                    height: this.video.videoHeight,
                    duration: this.video.duration
                });
            };
            this.video.onerror = reject;
        });
    }

    async getFrame(time) {
        return new Promise((resolve) => {
            const onSeeked = () => {
                this.video.removeEventListener('seeked', onSeeked);
                this.ctx.drawImage(this.video, 0, 0);
                const imageData = this.ctx.getImageData(0, 0, this.canvas.width, this.canvas.height);
                const data = new Uint8Array(this.canvas.width * this.canvas.height);
                for (let i = 0; i < data.length; i++) {
                    const r = imageData.data[i * 4];
                    const g = imageData.data[i * 4 + 1];
                    const b = imageData.data[i * 4 + 2];
                    data[i] = 0.299 * r + 0.587 * g + 0.114 * b;
                }
                resolve({
                    data,
                    metadata: {
                        width: this.canvas.width,
                        height: this.canvas.height,
                        bitDepth: 8,
                        pixelFormat: PixelFormat.Mono,
                        endianness: Endianness.Little
                    }
                });
            };
            this.video.addEventListener('seeked', onSeeked);
            this.video.currentTime = time;
        });
    }
}
