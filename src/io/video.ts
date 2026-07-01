import { Frame, PixelFormat, Endianness } from '../core/types';

export class VideoFrameExtractor {
    private video: HTMLVideoElement;
    private canvas: OffscreenCanvas;
    private ctx: OffscreenCanvasRenderingContext2D;

    constructor() {
        this.video = document.createElement('video');
        this.canvas = new OffscreenCanvas(1, 1);
        const ctx = this.canvas.getContext('2d', { willReadFrequently: true });
        if (!ctx) throw new Error('Could not get OffscreenCanvas context');
        this.ctx = ctx;
    }

    async load(url: string): Promise<{ width: number, height: number, duration: number }> {
        return new Promise((resolve, reject) => {
            this.video.src = url;
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

    async getFrame(time: number): Promise<Frame> {
        return new Promise((resolve) => {
            this.video.currentTime = time;
            this.video.onseeked = () => {
                this.ctx.drawImage(this.video, 0, 0);
                const imageData = this.ctx.getImageData(0, 0, this.canvas.width, this.canvas.height);
                // Convert RGBA to Grayscale for initial processing (or keep RGB)
                const data = new Uint8Array(this.canvas.width * this.canvas.height);
                for (let i = 0; i < data.length; i++) {
                    data[i] = imageData.data[i * 4]; // R channel as mono for quality analysis
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
        });
    }

    async extractAll(fps: number = 30): Promise<Frame[]> {
        const frames: Frame[] = [];
        const duration = this.video.duration;
        for (let t = 0; t < duration; t += 1/fps) {
            frames.push(await this.getFrame(t));
        }
        return frames;
    }
}
