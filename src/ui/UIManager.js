export class UIManager {
    constructor(app) {
        this.app = app;
        this.canvas = document.getElementById('main-canvas');
        this.ctx = this.canvas.getContext('2d');
        this.statusBar = document.getElementById('status-bar');
        this.resizerLeft = document.getElementById('resizer-left');
        this.resizerRight = document.getElementById('resizer-right');
        this.leftSidebar = document.getElementById('left-sidebar');
        this.rightSidebar = document.getElementById('right-sidebar');
    }

    init() {
        this.setupResizers();
        this.setupEventListeners();
        window.addEventListener('resize', () => this.handleResize());
        this.handleResize();
    }

    setupResizers() {
        let isResizingLeft = false;
        let isResizingRight = false;

        this.resizerLeft.addEventListener('mousedown', (e) => {
            isResizingLeft = true;
            document.body.style.cursor = 'col-resize';
        });

        this.resizerRight.addEventListener('mousedown', (e) => {
            isResizingRight = true;
            document.body.style.cursor = 'col-resize';
        });

        document.addEventListener('mousemove', (e) => {
            if (isResizingLeft) {
                const newWidth = e.clientX;
                if (newWidth > 100 && newWidth < 600) {
                    this.leftSidebar.style.width = `${newWidth}px`;
                }
            }
            if (isResizingRight) {
                const newWidth = window.innerWidth - e.clientX;
                if (newWidth > 100 && newWidth < 600) {
                    this.rightSidebar.style.width = `${newWidth}px`;
                }
            }
        });

        document.addEventListener('mouseup', () => {
            isResizingLeft = false;
            isResizingRight = false;
            document.body.style.cursor = 'default';
        });
    }

    setupEventListeners() {
        document.getElementById('btn-open-file').addEventListener('click', () => {
            this.handleOpenFile();
        });
        document.getElementById('btn-analyze').addEventListener('click', () => {
            this.handleAnalyze();
        });
        document.getElementById('btn-register').addEventListener('click', () => {
            this.handleRegister();
        });
        document.getElementById('btn-stack').addEventListener('click', () => {
            this.handleStack();
        });
        document.getElementById('btn-apply-wavelets').addEventListener('click', () => {
            this.handleWavelets();
        });
        document.getElementById('btn-apply-unsharp').addEventListener('click', () => {
            this.handleUnsharp();
        });
        document.getElementById('btn-export').addEventListener('click', () => {
            this.handleExport();
        });
        window.addEventListener('error', (e) => {
            this.setStatus(`Runtime Error: ${e.message}`);
            console.error(e);
        });
        window.addEventListener('unhandledrejection', (e) => {
            this.setStatus(`Promise Error: ${e.reason}`);
            console.error(e);
        });
    }

    async handleOpenFile() {
        try {
            const [fileHandle] = await window.showOpenFilePicker({
                types: [
                    {
                        description: 'Astronomical Data',
                        accept: {
                            'video/ser': ['.ser'],
                            'video/avi': ['.avi'],
                            'video/mp4': ['.mp4'],
                            'image/fits': ['.fits', '.fit', '.fts']
                        }
                    }
                ],
                multiple: false
            });
            const file = await fileHandle.getFile();
            this.setStatus(`Loading ${file.name}...`);
            await this.app.projectManager.loadFile(file);
            this.displayFrame(this.app.projectManager.currentFrame);
            this.setStatus(`Loaded ${file.name}`);
            document.getElementById('btn-analyze').disabled = false;
        } catch (err) {
            if (err.name !== 'AbortError') {
                console.error(err);
                this.setStatus(`Error: ${err.message}`);
            }
        }
    }

    setStatus(text) {
        this.statusBar.textContent = text;
    }

    handleResize() {
        // Handle canvas size etc
    }

    async handleAnalyze() {
        this.setStatus('Analyzing frames...');
        document.getElementById('btn-analyze').disabled = true;
        try {
            await this.app.projectManager.analyzeFrames((progress) => {
                this.setStatus(`Analyzing frames: ${Math.round(progress * 100)}%`);
            });
            this.setStatus('Analysis complete');
        } catch (err) {
            this.setStatus(`Analysis failed: ${err.message}`);
            document.getElementById('btn-analyze').disabled = false;
            throw err;
        }
        this.renderAnalysisResults();
        document.getElementById('btn-register').disabled = false;
    }

    async handleRegister() {
        this.setStatus('Registering frames...');
        document.getElementById('btn-register').disabled = true;
        await this.app.projectManager.registerFrames((progress) => {
            this.setStatus(`Registering frames: ${Math.round(progress * 100)}%`);
        });
        this.setStatus('Registration complete');
        document.getElementById('btn-stack').disabled = false;
    }

    async handleStack() {
        const percent = parseInt(document.getElementById('stack-percent').value);
        this.setStatus(`Stacking top ${percent}%...`);
        document.getElementById('btn-stack').disabled = true;
        await this.app.projectManager.stackFrames(percent, (progress) => {
            this.setStatus(`Stacking: ${Math.round(progress * 100)}%`);
        });
        this.setStatus('Stacking complete');
        this.displayFrame(this.app.projectManager.stackedFrame);
        document.getElementById('btn-apply-wavelets').disabled = false;
        document.getElementById('btn-apply-unsharp').disabled = false;
        document.getElementById('btn-export').disabled = false;
    }

    async handleWavelets() {
        import('../core/Wavelets.js').then(({ Wavelets }) => {
            const frame = this.app.projectManager.stackedFrame;
            if (!frame) return;

            const layers = Wavelets.atrous(frame, 5);
            const w1 = parseFloat(document.getElementById('wavelet-1').value);
            const w2 = parseFloat(document.getElementById('wavelet-2').value);

            const reconstructed = Wavelets.reconstruct(layers, [w1, w2, 1, 1, 1]);
            const processedFrame = {
                ...frame,
                data: reconstructed
            };
            this.displayFrame(processedFrame);
            this.setStatus('Wavelets applied');
        });
    }

    async handleUnsharp() {
        import('../core/PostProcessor.js').then(({ PostProcessor }) => {
            const frame = this.app.projectManager.stackedFrame;
            if (!frame) return;

            const amount = parseFloat(document.getElementById('unsharp-amount').value);
            const processedFrame = {
                ...frame,
                data: new Float32Array(frame.data)
            };
            PostProcessor.applyUnsharpMask(processedFrame, amount, 1.0);
            this.displayFrame(processedFrame);
            this.setStatus('Unsharp mask applied');
        });
    }

    displayFrame(frame) {
        if (!frame) return;

        this.canvas.width = frame.width;
        this.canvas.height = frame.height;
        const imageData = this.ctx.createImageData(frame.width, frame.height);
        const data = imageData.data;

        // Apply a simple auto-stretch for display if it's very dark
        const stats = this.getFrameStats(frame);
        const range = stats.max - stats.min;
        const stretch = range > 0 ? 1.0 / stats.max : 1.0;

        for (let i = 0; i < frame.width * frame.height; i++) {
            const idx = i * 4;
            const fIdx = i * frame.channels;

            let r, g, b;
            if (frame.channels === 3) {
                r = frame.data[fIdx];
                g = frame.data[fIdx + 1];
                b = frame.data[fIdx + 2];
            } else {
                r = g = b = frame.data[fIdx];
            }

            data[idx] = Math.min(255, Math.max(0, r * stretch * 255));
            data[idx + 1] = Math.min(255, Math.max(0, g * stretch * 255));
            data[idx + 2] = Math.min(255, Math.max(0, b * stretch * 255));
            data[idx + 3] = 255;
        }

        this.ctx.putImageData(imageData, 0, 0);
        document.getElementById('frame-info').textContent = `${frame.width}x${frame.height} | Channels: ${frame.channels}`;
    }

    getFrameStats(frame) {
        let min = 1, max = 0;
        for (let i = 0; i < frame.data.length; i++) {
            const v = frame.data[i];
            if (v < min) min = v;
            if (v > max) max = v;
        }
        return { min, max };
    }

    handleExport() {
        const link = document.createElement('a');
        link.download = 'stacked_image.png';
        link.href = this.canvas.toDataURL();
        link.click();
        this.setStatus('Exported image');
    }

    renderAnalysisResults() {
        const results = this.app.projectManager.analysisResults;
        const container = document.getElementById('analysis-chart');
        container.innerHTML = '';

        const canvas = document.createElement('canvas');
        canvas.width = container.clientWidth;
        canvas.height = 180;
        container.appendChild(canvas);

        const ctx = canvas.getContext('2d');
        const maxScore = Math.max(...results.map(r => r.score));

        ctx.strokeStyle = '#007acc';
        ctx.beginPath();
        results.forEach((r, i) => {
            const x = (i / results.length) * canvas.width;
            const y = canvas.height - (r.score / maxScore) * canvas.height;
            if (i === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        });
        ctx.stroke();
    }

    displayFrame(frame) {
        if (!frame) return;

        this.canvas.width = frame.width;
        this.canvas.height = frame.height;
        const imageData = this.ctx.createImageData(frame.width, frame.height);
        const data = imageData.data;

        for (let i = 0; i < frame.width * frame.height; i++) {
            const val = Math.min(255, Math.max(0, frame.data[i] * 255));
            const idx = i * 4;
            data[idx] = val;     // R
            data[idx + 1] = val; // G
            data[idx + 2] = val; // B
            data[idx + 3] = 255; // A
        }

        // Handle Color
        if (frame.channels === 3) {
            for (let i = 0; i < frame.width * frame.height; i++) {
                const idx = i * 4;
                const fIdx = i * 3;
                data[idx] = Math.min(255, Math.max(0, frame.data[fIdx] * 255));
                data[idx + 1] = Math.min(255, Math.max(0, frame.data[fIdx + 1] * 255));
                data[idx + 2] = Math.min(255, Math.max(0, frame.data[fIdx + 2] * 255));
            }
        }

        this.ctx.putImageData(imageData, 0, 0);
        document.getElementById('frame-info').textContent = `${frame.width}x${frame.height} | Channels: ${frame.channels}`;
    }
}
