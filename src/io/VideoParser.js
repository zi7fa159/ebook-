/**
 * Video Parser using hidden video element
 */
export class VideoParser {
    constructor(file) {
        this.file = file;
        this.metadata = null;
        this.url = URL.createObjectURL(file);
    }

    async init() {
        return new Promise((resolve, reject) => {
            const video = document.createElement('video');
            video.src = this.url;
            video.onloadedmetadata = () => {
                this.metadata = {
                    imageWidth: video.videoWidth,
                    imageHeight: video.videoHeight,
                    frameCount: Math.floor(video.duration * 30) || 100,
                    duration: video.duration
                };
                resolve();
            };
            video.onerror = reject;
        });
    }

    async getFrames(onFrame, maxFrames = Infinity) {
        if (window.MAX_TEST_FRAMES) maxFrames = Math.min(maxFrames, window.MAX_TEST_FRAMES);
        const video = document.createElement('video');
        video.src = this.url;
        video.muted = true;

        await new Promise((resolve) => {
            video.oncanplaythrough = resolve;
        });

        const canvas = new OffscreenCanvas(this.metadata.imageWidth, this.metadata.imageHeight);
        const ctx = canvas.getContext('2d');
        const fps = 30;
        let frameIdx = 0;

        return new Promise(async (resolve) => {
            if (maxFrames === 1) {
                video.currentTime = 0;
                await new Promise(r => video.onseeked = r);
                ctx.drawImage(video, 0, 0);
                const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                const data = this.toInternalData(imageData);
                onFrame({ data, width: canvas.width, height: canvas.height, channels: 3, index: 0 });
                resolve(1);
                return;
            }

            await video.play();

            const extract = async () => {
                if (video.ended || frameIdx >= maxFrames) {
                    video.pause();
                    video.src = "";
                    video.load();
                    resolve(frameIdx);
                    return;
                }

                ctx.drawImage(video, 0, 0);
                const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                const data = this.toInternalData(imageData);

                await onFrame({
                    data,
                    width: canvas.width,
                    height: canvas.height,
                    channels: 3,
                    index: frameIdx++
                });

                if (video.requestVideoFrameCallback) {
                    video.requestVideoFrameCallback(extract);
                } else {
                    requestAnimationFrame(extract);
                }
            };

            if (video.requestVideoFrameCallback) {
                video.requestVideoFrameCallback(extract);
            } else {
                extract();
            }
        });
    }

    toInternalData(imageData) {
        const data = new Float32Array(imageData.width * imageData.height * 3);
        for (let i = 0; i < imageData.data.length / 4; i++) {
            data[i * 3] = imageData.data[i * 4] / 255;
            data[i * 3 + 1] = imageData.data[i * 4 + 1] / 255;
            data[i * 3 + 2] = imageData.data[i * 4 + 2] / 255;
        }
        return data;
    }

    destroy() {
        URL.revokeObjectURL(this.url);
    }
}
