import { SERParser } from './io/ser';
import { VideoFrameExtractor } from './io/video';
import { PixelFormat, Frame } from './core/types';
import { stackFramesSigmaClipping } from './pipeline/stacking';
import { applyWavelets } from './pipeline/postprocess';

const fileInput = document.getElementById('fileInput') as HTMLInputElement;
const stackBtn = document.getElementById('stackBtn') as HTMLButtonElement;
const frameRange = document.getElementById('frameRange') as HTMLInputElement;
const frameNum = document.getElementById('frameNum') as HTMLElement;
const fileInfo = document.getElementById('fileInfo') as HTMLElement;
const canvas = document.getElementById('mainCanvas') as HTMLCanvasElement;
const ctx = canvas.getContext('2d');

let parser: SERParser | null = null;
let videoExtractor: VideoFrameExtractor | null = null;
let frames: Frame[] = [];
const qualityScores: number[] = [];
let currentStacked: Float32Array | null = null;
let currentStackedWidth = 0;
let currentStackedHeight = 0;

const wl1 = document.getElementById('waveletLayer1') as HTMLInputElement;
const wl2 = document.getElementById('waveletLayer2') as HTMLInputElement;

[wl1, wl2].forEach(el => el.addEventListener('input', () => {
    if (currentStacked) {
        const layers = [parseFloat(wl1.value), parseFloat(wl2.value)];
        const processed = applyWavelets(currentStacked, currentStackedWidth, currentStackedHeight, layers);
        displayStacked(processed, currentStackedWidth, currentStackedHeight);
    }
}));

fileInput.addEventListener('change', async (e) => {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (!file) return;

    qualityScores.length = 0;
    frames = [];
    currentStacked = null;
    stackBtn.disabled = true;

    if (file.name.toLowerCase().endsWith('.ser')) {
        parser = new SERParser(file);
        try {
            const header = await parser.parseHeader();
            fileInfo.innerHTML = `
                <p>File: ${file.name}</p>
                <p>Resolution: ${header.width}x${header.height}</p>
                <p>Frames: ${header.frameCount}</p>
                <p>Format: SER (${header.pixelFormat})</p>
            `;
            frameRange.max = (header.frameCount - 1).toString();
            frameRange.disabled = false;
            renderFrame(0);
            analyzeQualitySER(header.frameCount);
        } catch (err) {
            console.error(err);
            fileInfo.innerHTML = `<p style="color: red">Error: ${err instanceof Error ? err.message : String(err)}</p>`;
        }
    } else if (file.name.toLowerCase().endsWith('.mp4') || file.name.toLowerCase().endsWith('.mov')) {
        videoExtractor = new VideoFrameExtractor();
        const url = URL.createObjectURL(file);
        try {
            const meta = await videoExtractor.load(url);
            fileInfo.innerHTML = `
                <p>File: ${file.name}</p>
                <p>Resolution: ${meta.width}x${meta.height}</p>
                <p>Duration: ${meta.duration.toFixed(2)}s</p>
                <p>Format: Video</p>
            `;
            const estimatedFrames = Math.floor(meta.duration * 30); // Assume 30fps for estimation
            frameRange.max = (estimatedFrames - 1).toString();
            frameRange.disabled = false;

            // Extract and analyze quality for video
            analyzeQualityVideo(meta.duration);
        } catch (err) {
            console.error(err);
            fileInfo.innerHTML = `<p style="color: red">Error: ${err instanceof Error ? err.message : String(err)}</p>`;
        }
    }
});

async function analyzeQualitySER(frameCount: number) {
    if (!parser) return;
    const stats = document.getElementById('stats') as HTMLElement;
    stats.textContent = `Analyzing quality... 0%`;

    const worker = new Worker(new URL('./workers/quality.worker.ts', import.meta.url), { type: 'module' });

    let processed = 0;
    qualityScores.length = frameCount;

    worker.onmessage = (e) => {
        const { frameIndex, score } = e.data;
        qualityScores[frameIndex] = score;
        processed++;
        stats.textContent = `Analyzing quality... ${Math.round((processed / frameCount) * 100)}%`;

        if (processed === frameCount) {
            const bestFrame = qualityScores.indexOf(Math.max(...qualityScores));
            stats.textContent = `Quality analysis complete. Best frame: ${bestFrame}`;
            worker.terminate();
            stackBtn.disabled = false;
        }
    };

    for (let i = 0; i < frameCount; i++) {
        const frame = await parser.getFrame(i);
        worker.postMessage({
            type: 'analyze',
            data: frame.data,
            width: frame.metadata.width,
            height: frame.metadata.height,
            frameIndex: i
        }, [frame.data.buffer]);
    }
}

async function analyzeQualityVideo(duration: number, fps: number = 30) {
    if (!videoExtractor) return;
    const stats = document.getElementById('stats') as HTMLElement;
    stats.textContent = `Extracting and analyzing video...`;

    const worker = new Worker(new URL('./workers/quality.worker.ts', import.meta.url), { type: 'module' });

    let frameIndex = 0;
    for (let t = 0; t < duration; t += 1/fps) {
        const frame = await videoExtractor.getFrame(t);
        frames.push(frame);

        worker.postMessage({
            type: 'analyze',
            data: frame.data,
            width: frame.metadata.width,
            height: frame.metadata.height,
            frameIndex: frameIndex
        }, [frame.data.buffer]);

        frameIndex++;
        stats.textContent = `Extracted ${frameIndex} frames...`;
    }

    qualityScores.length = frameIndex;
    let processed = 0;
    worker.onmessage = (e) => {
        const { frameIndex, score } = e.data;
        qualityScores[frameIndex] = score;
        processed++;
        if (processed === qualityScores.length) {
            stats.textContent = `Video analysis complete. ${processed} frames.`;
            worker.terminate();
            stackBtn.disabled = false;
        }
    };
}

frameRange.addEventListener('input', async () => {
    const index = parseInt(frameRange.value);
    frameNum.textContent = index.toString();
    if (parser) {
        renderFrame(index);
    } else if (frames[index]) {
        displayFrame(frames[index]);
    }
});

async function renderFrame(index: number) {
    if (!parser) return;
    const frame = await parser.getFrame(index);
    displayFrame(frame);
}

function displayFrame(frame: Frame) {
    if (!ctx) return;
    const { width, height } = frame.metadata;
    if (canvas.width !== width || canvas.height !== height) {
        canvas.width = width;
        canvas.height = height;
    }
    const imageData = ctx.createImageData(width, height);
    const data = imageData.data;

    if (frame.metadata.pixelFormat === PixelFormat.Mono) {
        for (let i = 0; i < width * height; i++) {
            const val = frame.data[i];
            const displayVal = frame.metadata.bitDepth > 8 ? (val >> (frame.metadata.bitDepth - 8)) : val;
            data[i * 4] = displayVal;
            data[i * 4 + 1] = displayVal;
            data[i * 4 + 2] = displayVal;
            data[i * 4 + 3] = 255;
        }
    }
    ctx.putImageData(imageData, 0, 0);
}

stackBtn.addEventListener('click', async () => {
    const stats = document.getElementById('stats') as HTMLElement;

    const indexedScores = qualityScores.map((score, index) => ({ score, index }));
    indexedScores.sort((a, b) => b.score - a.score);
    const frameCount = qualityScores.length;
    const topFramesCount = Math.max(1, Math.floor(frameCount * 0.25));
    const selectedIndices = indexedScores.slice(0, topFramesCount).map(f => f.index);

    stats.textContent = `Aligning ${topFramesCount} frames...`;

    const bestFrameIdx = selectedIndices[0];
    const refFrame = parser ? await parser.getFrame(bestFrameIdx) : frames[bestFrameIdx];

    const alignWorker = new Worker(new URL('./workers/align.worker.ts', import.meta.url), { type: 'module' });

    let alignedCount = 0;
    const results: { index: number, offset: { x: number, y: number }, frame: Frame }[] = [];

    alignWorker.onmessage = (e) => {
        const { frameIndex, offset } = e.data;
        const res = results.find(r => r.index === frameIndex);
        if (res) res.offset = offset;

        alignedCount++;
        stats.textContent = `Aligning frames... ${Math.round((alignedCount / topFramesCount) * 100)}%`;

        if (alignedCount === topFramesCount) {
            alignWorker.terminate();
            stats.textContent = `Stacking...`;

            results.sort((a, b) => selectedIndices.indexOf(a.index) - selectedIndices.indexOf(b.index));
            const sortedFrames = results.map(r => r.frame);
            const sortedOffsets = results.map(r => r.offset);

            const stackedData = stackFramesSigmaClipping(sortedFrames, sortedOffsets, 3);
            currentStacked = stackedData;
            currentStackedWidth = refFrame.metadata.width;
            currentStackedHeight = refFrame.metadata.height;
            displayStacked(stackedData, currentStackedWidth, currentStackedHeight);
            stats.textContent = `Stacking complete.`;
        }
    };

    for (const idx of selectedIndices) {
        const frame = parser ? await parser.getFrame(idx) : frames[idx];
        results.push({ index: idx, offset: { x: 0, y: 0 }, frame });
        alignWorker.postMessage({
            type: 'align',
            refData: refFrame.data,
            srcData: frame.data,
            width: refFrame.metadata.width,
            height: refFrame.metadata.height,
            frameIndex: idx
        }, [frame.data.buffer]);
    }
});

function displayStacked(data: Float32Array, width: number, height: number) {
    if (!ctx) return;
    canvas.width = width;
    canvas.height = height;
    const imageData = ctx.createImageData(width, height);

    let min = Infinity;
    let max = -Infinity;
    for (let i = 0; i < data.length; i++) {
        if (data[i] < min) min = data[i];
        if (data[i] > max) max = data[i];
    }
    const range = max - min || 1;

    for (let i = 0; i < data.length; i++) {
        const val = ((data[i] - min) / range) * 255;
        imageData.data[i * 4] = val;
        imageData.data[i * 4 + 1] = val;
        imageData.data[i * 4 + 2] = val;
        imageData.data[i * 4 + 3] = 255;
    }
    ctx.putImageData(imageData, 0, 0);
}
