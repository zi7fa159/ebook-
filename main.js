import { SERParser } from './src/io/ser-parser.js';
import { VideoExtractor } from './src/io/video-decoder.js';
import { ImageViewer } from './src/ui/image-viewer.js';
import { QualityManager } from './src/core/quality-manager.js';
import { Stacking } from './src/utils/stacking.js';
import { Exporter } from './src/io/exporter.js';
import { Registration } from './src/utils/registration.js';

class App {
    constructor() {
        this.viewer = null;
        this.currentProject = null;
        this.qualityManager = new QualityManager();
        this.init();
    }

    init() {
        const canvas = document.getElementById('main-viewer');
        this.viewer = new ImageViewer(canvas);

        this.setupDragAndDrop();
        this.setupEventListeners();
    }

    setupEventListeners() {
        document.getElementById('btn-analyze').addEventListener('click', () => this.runQualityAnalysis());
        document.getElementById('btn-stack').addEventListener('click', () => this.runStacking());
        document.getElementById('btn-export').addEventListener('click', () => this.exportResult());
    }

    setupDragAndDrop() {
        const workspace = document.getElementById('workspace');
        workspace.addEventListener('dragover', (e) => { e.preventDefault(); e.dataTransfer.dropEffect = 'copy'; });
        workspace.addEventListener('drop', async (e) => {
            e.preventDefault();
            const files = e.dataTransfer.files;
            if (files.length > 0) await this.handleFileImport(files[0]);
        });
    }

    async handleFileImport(file) {
        console.log('handleFileImport', file.name);
        this.updateStatus(`Importing ${file.name}...`);
        try {
            const ext = file.name.toLowerCase().split('.').pop();
            if (ext === 'ser') {
                const parser = new SERParser(file);
                const header = await parser.readHeader();
                const firstFrame = await parser.getFrame(0);
                this.viewer.setImage(firstFrame);
                this.currentProject = { file, type: 'ser', parser, header, qualityResults: null, stackedImage: null };
                this.onImportComplete();
            } else if (['mp4', 'mov', 'avi', 'mkv', 'webm'].includes(ext)) {
                this.updateStatus('Decoding video metadata...');
                this.currentProject = { file, type: 'video', frames: [] };
                this.runVideoImport(file);
            } else {
                throw new Error('Unsupported format');
            }
        } catch (err) {
            console.error(err);
            this.updateStatus(`Error: ${err.message}`);
        }
    }

    onImportComplete() {
        document.getElementById('file-info').textContent = `${this.currentProject.file.name} (${this.currentProject.header.frameCount} frames)`;
        document.getElementById('quality-section').style.display = 'block';
        this.updateStatus('Ready for analysis');
    }

    async runVideoImport(file) {
        const extractor = new VideoExtractor(file);
        this.showProgress(true);
        const frames = [];
        extractor.extractFrames(
            (frame) => {
                frames.push(frame);
                if (frames.length === 1) this.viewer.setImage(frame);
                this.updateStatus(`Importing video frames: ${frames.length}`);
            },
            () => {
                this.currentProject.frames = frames;
                this.currentProject.header = {
                    width: frames[0].width,
                    height: frames[0].height,
                    frameCount: frames.length
                };
                this.onImportComplete();
                this.showProgress(false);
            }
        );
    }

    async runQualityAnalysis() {
        if (!this.currentProject) return;
        this.updateStatus('Analyzing quality...');
        this.showProgress(true);

        let frames = [];
        if (this.currentProject.type === 'video') {
            frames = this.currentProject.frames;
        } else {
            for (let i = 0; i < this.currentProject.header.frameCount; i++) {
                frames.push(await this.currentProject.parser.getFrame(i));
                this.updateProgressBar((i + 1) / this.currentProject.header.frameCount * 0.5);
            }
        }

        this.qualityManager.onProgress = (p) => this.updateProgressBar(0.5 + p * 0.5);
        this.qualityManager.onComplete = (results) => {
            this.currentProject.qualityResults = results.sort((a, b) => b[1].score - a[1].score);
            this.updateStatus('Analysis complete');
            this.showProgress(false);
            document.getElementById('btn-stack').disabled = false;
            this.plotQuality();
        };

        this.qualityManager.analyzeFrames(frames);
    }

    async runStacking() {
        if (!this.currentProject || !this.currentProject.qualityResults) return;
        this.updateStatus('Aligning and Stacking...');
        this.showProgress(true);

        const percent = parseInt(document.getElementById('select-percent').value) / 100;
        const count = Math.max(1, Math.floor(this.currentProject.qualityResults.length * percent));
        const selectedResults = this.currentProject.qualityResults.slice(0, count);

        // Reference frame is the best one
        const refIndex = selectedResults[0][0];
        const refFrame = this.currentProject.type === 'video' ? this.currentProject.frames[refIndex] : await this.currentProject.parser.getFrame(refIndex);

        const framesToStack = [];
        for (let i = 0; i < selectedResults.length; i++) {
            const frameIndex = selectedResults[i][0];
            let frame = this.currentProject.type === 'video' ? this.currentProject.frames[frameIndex] : await this.currentProject.parser.getFrame(frameIndex);

            // Align to reference
            if (frameIndex !== refIndex) {
                const offset = Registration.phaseCorrelation(refFrame.data, frame.data, refFrame.width, refFrame.height);
                frame.dx = offset.dx;
                frame.dy = offset.dy;
            } else {
                frame.dx = 0;
                frame.dy = 0;
            }

            framesToStack.push(frame);
            this.updateProgressBar((i + 1) / selectedResults.length);
        }

        const weights = selectedResults.map(r => r[1].score);
        const stackedData = Stacking.weightedMean(
            framesToStack,
            weights,
            this.currentProject.header.width,
            this.currentProject.header.height
        );

        this.currentProject.stackedImage = {
            width: this.currentProject.header.width,
            height: this.currentProject.header.height,
            data: stackedData,
            channels: framesToStack[0].channels
        };

        this.viewer.setImage(this.currentProject.stackedImage);
        document.getElementById('post-processing').style.display = 'block';
        this.updateStatus('Stacking complete');
        this.showProgress(false);
    }

    async exportResult() {
        if (!this.currentProject.stackedImage) return;
        const blob = await Exporter.toPNG(this.currentProject.stackedImage);
        Exporter.download(blob, 'stacked_result.png');
    }

    plotQuality() {
        const canvas = document.getElementById('quality-plot');
        const ctx = canvas.getContext('2d');
        const results = this.currentProject.qualityResults;
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.strokeStyle = '#007acc';
        ctx.lineWidth = 2;
        ctx.beginPath();
        const scores = results.map(r => r[1].score);
        const maxScore = Math.max(...scores);
        const minScore = Math.min(...scores);
        const range = maxScore - minScore || 1;

        results.forEach((r, i) => {
            const x = (i / results.length) * canvas.width;
            const y = canvas.height - ((r[1].score - minScore) / range) * canvas.height;
            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        });
        ctx.stroke();
    }

    updateStatus(msg) { document.getElementById('status-bar').textContent = msg; }
    showProgress(show) { document.getElementById('progress-container').style.display = show ? 'block' : 'none'; }
    updateProgressBar(p) { document.getElementById('progress-bar').style.width = `${p * 100}%`; }
}

window.app = new App();
