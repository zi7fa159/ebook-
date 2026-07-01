export class ImageViewer {
    constructor(canvas) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        this.image = null; // { width, height, data, channels }
        this.scale = 1.0;
        this.offsetX = 0;
        this.offsetY = 0;
        this.isDragging = false;
        this.lastMouseX = 0;
        this.lastMouseY = 0;

        this.initEvents();
    }

    setImage(image) {
        this.image = image;
        this.fitToScreen();
        this.render();
    }

    fitToScreen() {
        if (!this.image) return;
        const padding = 20;
        const availableWidth = this.canvas.clientWidth - padding;
        const availableHeight = this.canvas.clientHeight - padding;

        const scaleX = availableWidth / this.image.width;
        const scaleY = availableHeight / this.image.height;
        this.scale = Math.min(scaleX, scaleY, 1.0);

        this.offsetX = (this.canvas.width - this.image.width * this.scale) / 2;
        this.offsetY = (this.canvas.height - this.image.height * this.scale) / 2;
    }

    initEvents() {
        this.canvas.addEventListener('mousedown', (e) => {
            this.isDragging = true;
            this.lastMouseX = e.clientX;
            this.lastMouseY = e.clientY;
        });

        window.addEventListener('mousemove', (e) => {
            if (!this.isDragging) return;
            const dx = e.clientX - this.lastMouseX;
            const dy = e.clientY - this.lastMouseY;
            this.offsetX += dx;
            this.offsetY += dy;
            this.lastMouseX = e.clientX;
            this.lastMouseY = e.clientY;
            this.render();
        });

        window.addEventListener('mouseup', () => {
            this.isDragging = false;
        });

        this.canvas.addEventListener('wheel', (e) => {
            e.preventDefault();
            const zoomSpeed = 0.001;
            const delta = -e.deltaY;
            const factor = Math.pow(1.1, delta / 100);

            const mouseX = e.offsetX;
            const mouseY = e.offsetY;

            // Zoom towards mouse
            const worldX = (mouseX - this.offsetX) / this.scale;
            const worldY = (mouseY - this.offsetY) / this.scale;

            this.scale *= factor;
            this.offsetX = mouseX - worldX * this.scale;
            this.offsetY = mouseY - worldY * this.scale;

            this.render();
        }, { passive: false });

        window.addEventListener('resize', () => {
            this.resizeCanvas();
            this.render();
        });

        this.resizeCanvas();
    }

    resizeCanvas() {
        this.canvas.width = this.canvas.clientWidth;
        this.canvas.height = this.canvas.clientHeight;
    }

    render() {
        if (!this.image) return;

        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        // Simple conversion for display (8-bit)
        // In production, this would be a WebGL/WebGPU shader for real-time LUTs/Levels
        const tempCanvas = document.createElement('canvas');
        tempCanvas.width = this.image.width;
        tempCanvas.height = this.image.height;
        const tempCtx = tempCanvas.getContext('2d');
        const imageData = tempCtx.createImageData(this.image.width, this.image.height);

        const data = this.image.data;
        const channels = this.image.channels;

        for (let i = 0; i < this.image.width * this.image.height; i++) {
            const idx = i * 4;
            if (channels === 1) {
                const val = Math.floor(data[i] * 255);
                imageData.data[idx] = val;
                imageData.data[idx+1] = val;
                imageData.data[idx+2] = val;
                imageData.data[idx+3] = 255;
            } else if (channels === 3) {
                const sIdx = i * 3;
                imageData.data[idx] = Math.floor(data[sIdx] * 255);
                imageData.data[idx+1] = Math.floor(data[sIdx+1] * 255);
                imageData.data[idx+2] = Math.floor(data[sIdx+2] * 255);
                imageData.data[idx+3] = 255;
            }
        }

        tempCtx.putImageData(imageData, 0, 0);

        this.ctx.imageSmoothingEnabled = false;
        this.ctx.drawImage(
            tempCanvas,
            0, 0, this.image.width, this.image.height,
            this.offsetX, this.offsetY, this.image.width * this.scale, this.image.height * this.scale
        );
    }
}
