import { SERParser } from './io/ser-parser.js';
import { ImageViewer } from './ui/image-viewer.js';
import { QualityManager } from './core/quality-manager.js';
import { Stacking } from './utils/stacking.js';
import { Exporter } from './io/exporter.js';

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
        this.updateStatus(`Importing ${file.name}...`);
        try {
            let parser, header;
            if (file.name.toLowerCase().endsWith('.ser')) {
                parser = new SERParser(file);
                header = await parser.readHeader();
            } else {
                throw new Error('Unsupported format');
            }

            const firstFrame = await parser.getFrame(0);
            this.viewer.setImage(firstFrame);

            this.currentProject = { file, parser, header, qualityResults: null, stackedImage: null };
            document.getElementById('file-info').textContent = `${file.name} (${header.frameCount} frames)`;
            document.getElementById('quality-section').style.display = 'block';
            this.updateStatus('Ready for analysis');
        } catch (err) {
            this.updateStatus(`Error: ${err.message}`);
        }
    }

    async runQualityAnalysis() {
        if (!this.currentProject) return;
        this.updateStatus('Analyzing quality...');
        this.showProgress(true);

        const frames = [];
        for (let i = 0; i < this.currentProject.header.frameCount; i++) {
            frames.push(await this.currentProject.parser.getFrame(i));
            this.updateProgressBar((i + 1) / this.currentProject.header.frameCount * 0.5); // Load progress
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
        this.updateStatus('Stacking...');
        this.showProgress(true);

        const percent = parseInt(document.getElementById('select-percent').value) / 100;
        const count = Math.max(1, Math.floor(this.currentProject.qualityResults.length * percent));
        const selectedResults = this.currentProject.qualityResults.slice(0, count);

        const framesToStack = [];
        for (let i = 0; i < selectedResults.length; i++) {
            const frameIndex = selectedResults[i][0];
            framesToStack.push(await this.currentProject.parser.getFrame(frameIndex));
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
        ctx.beginPath();
        const maxScore = Math.max(...results.map(r => r[1].score));
        results.forEach((r, i) => {
            const x = (i / results.length) * canvas.width;
            const y = canvas.height - (r[1].score / maxScore) * canvas.height;
            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        });
        ctx.stroke();
    }

    updateStatus(msg) { document.getElementById('status-bar').textContent = msg; }
    showProgress(show) { document.getElementById('progress-container').style.display = show ? 'block' : 'none'; }
    updateProgressBar(p) { document.getElementById('progress-bar').style.width = `${p * 100}%`; }
}

window.app = new App();
