/**
 * Video frame extractor using the <video> element and Canvas.
 */
export class VideoExtractor {
    constructor(file) {
        this.file = file;
        this.url = URL.createObjectURL(file);
    }

    async extractFrames(onFrame, onComplete, maxFrames = 100) {
        console.log('Extracting frames (max:', maxFrames, ') from:', this.file.name);
        const video = document.createElement('video');
        video.src = this.url;
        video.muted = true;
        video.playsInline = true;

        await new Promise((resolve, reject) => {
            video.onloadedmetadata = () => {
                console.log('Metadata loaded:', video.videoWidth, 'x', video.videoHeight, 'duration:', video.duration);
                resolve();
            };
            video.onerror = (e) => {
                console.error('Video error:', video.error);
                reject(new Error('Video load error: ' + (video.error ? video.error.message : 'Unknown')));
            };
        });

        const width = video.videoWidth;
        const height = video.videoHeight;
        const canvas = new OffscreenCanvas(width, height);
        const ctx = canvas.getContext('2d', { willReadFrequently: true });

        let frameCount = 0;
        const duration = video.duration;
        const fps = 30;

        video.onseeked = async () => {
            ctx.drawImage(video, 0, 0);
            const imageData = ctx.getImageData(0, 0, width, height);
            const floatData = new Float32Array(width * height);
            for (let i = 0; i < width * height; i++) {
                const r = imageData.data[i * 4] / 255;
                const g = imageData.data[i * 4 + 1] / 255;
                const b = imageData.data[i * 4 + 2] / 255;
                floatData[i] = 0.299 * r + 0.587 * g + 0.114 * b;
            }
            onFrame({ data: floatData, width, height, channels: 1, index: frameCount++ });

            const nextTime = frameCount / fps;
            if (nextTime < duration && frameCount < maxFrames) {
                video.currentTime = nextTime;
            } else {
                console.log('Extraction complete, frames:', frameCount);
                onComplete();
                URL.revokeObjectURL(this.url);
            }
        };

        video.currentTime = 0;
    }
}
