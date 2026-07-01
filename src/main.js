import { SERParser } from './io/ser.js';
import { VideoFrameExtractor } from './io/video.js';
import { FITSParser } from './io/fits.js';
import { PixelFormat } from './core/types.js';
import { stackFramesSigmaClipping } from './pipeline/stacking.js';
import { stackFramesMultiPoint } from './pipeline/stacking_multi.js';
import { applyWavelets } from './pipeline/postprocess.js';
import { debayer } from './pipeline/debayer.js';
import { generateAPs } from './pipeline/multipoint.js';

const fileInput = document.getElementById('fileInput');
const stackBtn = document.getElementById('stackBtn');
const exportBtn = document.getElementById('exportBtn');
const frameRange = document.getElementById('frameRange');
const frameNum = document.getElementById('frameNum');
const fileInfo = document.getElementById('fileInfo');
const canvas = document.getElementById('mainCanvas');
const ctx = canvas.getContext('2d');

let parser = null;
let videoExtractor = null;
let fitsParser = null;
let videoDuration = 0;
let videoFPS = 30;
const qualityScores = [];
let currentStacked = null;
let currentStackedWidth = 0;
let currentStackedHeight = 0;

const wl1 = document.getElementById('waveletLayer1');
const wl2 = document.getElementById('waveletLayer2');

[wl1, wl2].forEach(el => el.addEventListener('input', () => {
    if (currentStacked) {
        const layers = [parseFloat(wl1.value), parseFloat(wl2.value)];
        const processed = applyWavelets(currentStacked, currentStackedWidth, currentStackedHeight, layers);
        displayStacked(processed, currentStackedWidth, currentStackedHeight);
    }
}));

fileInput.addEventListener('change', async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    qualityScores.length = 0;
    currentStacked = null;
    stackBtn.disabled = true;
    exportBtn.disabled = true;
    parser = null;
    videoExtractor = null;
    fitsParser = null;

    const name = file.name.toLowerCase();
    if (name.endsWith('.ser')) {
        parser = new SERParser(file);
        try {
            const header = await parser.parseHeader();
            fileInfo.innerHTML = `<p>File: ${file.name}</p><p>Resolution: ${header.width}x${header.height}</p><p>Frames: ${header.frameCount}</p><p>Format: SER</p>`;
            frameRange.max = (header.frameCount - 1).toString();
            frameRange.disabled = false;
            renderFrame(0);
            analyzeQualitySER(header.frameCount);
        } catch (err) {
            fileInfo.innerHTML = `<p style="color: red">Error: ${err.message}</p>`;
        }
    } else if (name.endsWith('.mp4') || name.endsWith('.mov')) {
        videoExtractor = new VideoFrameExtractor();
        const url = URL.createObjectURL(file);
        try {
            const meta = await videoExtractor.load(url);
            videoDuration = meta.duration;
            fileInfo.innerHTML = `<p>File: ${file.name}</p><p>Resolution: ${meta.width}x${meta.height}</p><p>Duration: ${meta.duration.toFixed(2)}s</p><p>Format: Video</p>`;
            videoFPS = meta.duration > 30 ? 10 : 30;
            const estimatedFrames = Math.floor(meta.duration * videoFPS);
            frameRange.max = (estimatedFrames - 1).toString();
            frameRange.disabled = false;
            analyzeQualityVideo(meta.duration, videoFPS);
        } catch (err) {
            fileInfo.innerHTML = `<p style="color: red">Error: ${err.message}</p>`;
        }
    } else if (name.endsWith('.fits') || name.endsWith('.fit')) {
        fitsParser = new FITSParser(file);
        try {
            const header = await fitsParser.parseHeader();
            fileInfo.innerHTML = `<p>File: ${file.name}</p><p>Resolution: ${header.width}x${header.height}</p><p>Frames: ${header.depth}</p><p>Format: FITS</p>`;
            frameRange.max = (header.depth - 1).toString();
            frameRange.disabled = false;
            renderFrame(0);
            analyzeQualityFITS(header.depth);
        } catch (err) {
            fileInfo.innerHTML = `<p style="color: red">Error: ${err.message}</p>`;
        }
    }
});

async function analyzeQualitySER(frameCount) {
    if (!parser) return;
    const stats = document.getElementById('stats');
    const worker = new Worker(new URL('./workers/quality.worker.js', import.meta.url), { type: 'module' });
    let processed = 0;
    qualityScores.length = frameCount;
    worker.onmessage = (e) => {
        const { frameIndex, score } = e.data;
        qualityScores[frameIndex] = score;
        processed++;
        stats.textContent = `Analyzing quality... ${Math.round((processed / frameCount) * 100)}%`;
        if (processed === frameCount) {
            worker.terminate();
            stackBtn.disabled = false;
        }
    };
    for (let i = 0; i < frameCount; i++) {
        const frame = await parser.getFrame(i);
        worker.postMessage({ type: 'analyze', data: frame.data, width: frame.metadata.width, height: frame.metadata.height, frameIndex: i }, [frame.data.buffer]);
    }
}

async function analyzeQualityFITS(frameCount) {
    if (!fitsParser) return;
    const stats = document.getElementById('stats');
    const worker = new Worker(new URL('./workers/quality.worker.js', import.meta.url), { type: 'module' });
    let processed = 0;
    qualityScores.length = frameCount;
    worker.onmessage = (e) => {
        const { frameIndex, score } = e.data;
        qualityScores[frameIndex] = score;
        processed++;
        stats.textContent = `Analyzing quality... ${Math.round((processed / frameCount) * 100)}%`;
        if (processed === frameCount) {
            worker.terminate();
            stackBtn.disabled = false;
        }
    };
    for (let i = 0; i < frameCount; i++) {
        const frame = await fitsParser.getFrame(i);
        worker.postMessage({ type: 'analyze', data: frame.data, width: frame.metadata.width, height: frame.metadata.height, frameIndex: i }, [frame.data.buffer]);
    }
}

async function analyzeQualityVideo(duration, fps) {
    if (!videoExtractor) return;
    const stats = document.getElementById('stats');
    const worker = new Worker(new URL('./workers/quality.worker.js', import.meta.url), { type: 'module' });
    const totalFrames = Math.floor(duration * fps);
    qualityScores.length = totalFrames;
    const framesCache = [];
    let processed = 0;
    worker.onmessage = (e) => {
        const { frameIndex, score, data } = e.data;
        qualityScores[frameIndex] = score;
        if (framesCache[frameIndex]) framesCache[frameIndex].data = data;
        processed++;
        stats.textContent = `Analyzed ${processed}/${totalFrames} frames...`;
        if (processed === totalFrames) {
            worker.terminate();
            stackBtn.disabled = false;
        }
    };
    for (let i = 0; i < totalFrames; i++) {
        const frame = await videoExtractor.getFrame(i / fps);
        framesCache.push(frame);
        worker.postMessage({ type: 'analyze', data: frame.data, width: frame.metadata.width, height: frame.metadata.height, frameIndex: i }, [frame.data.buffer]);
    }
    window.framesCache = framesCache;
}

frameRange.addEventListener('input', async () => {
    const index = parseInt(frameRange.value);
    frameNum.textContent = index.toString();
    if (parser) {
        renderFrame(index);
    } else if (fitsParser) {
        const frame = await fitsParser.getFrame(index);
        displayFrame(frame);
    } else if (videoExtractor) {
        const frame = await videoExtractor.getFrame(index / videoFPS);
        displayFrame(frame);
    }
});

async function renderFrame(index) {
    const frame = parser ? await parser.getFrame(index) : null;
    if (frame) displayFrame(frame);
}

function displayFrame(frame) {
    if (!ctx) return;
    const { width, height, pixelFormat } = frame.metadata;
    if (canvas.width !== width || canvas.height !== height) {
        canvas.width = width;
        canvas.height = height;
    }
    if (pixelFormat !== PixelFormat.Mono && pixelFormat !== PixelFormat.RGB) {
        const rgba = debayer(frame.data, width, height, pixelFormat);
        ctx.putImageData(new ImageData(rgba, width, height), 0, 0);
    } else {
        const imageData = ctx.createImageData(width, height);
        const data = imageData.data;
        for (let i = 0; i < width * height; i++) {
            const val = frame.data[i];
            const displayVal = frame.metadata.bitDepth > 8 ? (val >> (frame.metadata.bitDepth - 8)) : val;
            data[i * 4] = data[i * 4 + 1] = data[i * 4 + 2] = displayVal;
            data[i * 4 + 3] = 255;
        }
        ctx.putImageData(imageData, 0, 0);
    }
}

stackBtn.addEventListener('click', async () => {
    const stats = document.getElementById('stats');
    const indexedScores = qualityScores.map((score, index) => ({ score, index }));
    indexedScores.sort((a, b) => b.score - a.score);
    const frameCount = qualityScores.length;
    const topFramesCount = Math.min(50, Math.max(1, Math.floor(frameCount * 0.1)));
    const selectedIndices = indexedScores.slice(0, topFramesCount).map(f => f.index);

    const bestFrameIdx = selectedIndices[0];
    const refFrame = parser ? await parser.getFrame(bestFrameIdx) : (fitsParser ? await fitsParser.getFrame(bestFrameIdx) : window.framesCache[bestFrameIdx]);
    const { width, height } = refFrame.metadata;

    // Multi-point alignment
    const aps = generateAPs(width, height, Math.floor(width/4), Math.floor(width/8));
    stats.textContent = `Aligning ${topFramesCount} frames with ${aps.length} APs...`;

    const alignWorker = new Worker(new URL('./workers/align_multi.worker.js', import.meta.url), { type: 'module' });
    let alignedCount = 0;
    const results = [];

    alignWorker.onmessage = (e) => {
        const { frameIndex, shifts, srcData } = e.data;
        const res = results.find(r => r.index === frameIndex);
        if (res) {
            res.offsets = shifts;
            res.frame.data = srcData;
        }
        alignedCount++;
        stats.textContent = `Aligning frames... ${Math.round((alignedCount / topFramesCount) * 100)}%`;
        if (alignedCount === topFramesCount) {
            alignWorker.terminate();
            stats.textContent = `Stacking...`;
            results.sort((a, b) => selectedIndices.indexOf(a.index) - selectedIndices.indexOf(b.index));
            const stackedData = stackFramesMultiPoint(results.map(r => r.frame), results.map(r => r.offsets), width, height, aps, 3);
            currentStacked = stackedData;
            currentStackedWidth = width;
            currentStackedHeight = height;
            displayStacked(stackedData, width, height);
            stats.textContent = `Stacking complete. Used ${topFramesCount} frames.`;
            exportBtn.disabled = false;
        }
    };

    for (const idx of selectedIndices) {
        const frame = parser ? await parser.getFrame(idx) : (fitsParser ? await fitsParser.getFrame(idx) : window.framesCache[idx]);
        results.push({ index: idx, offsets: [], frame });
        alignWorker.postMessage({ type: 'align_multi', refData: refFrame.data, srcData: frame.data, width, height, aps }, [frame.data.buffer]);
    }
});

function displayStacked(data, width, height) {
    if (!ctx) return;
    canvas.width = width;
    canvas.height = height;
    const imageData = ctx.createImageData(width, height);
    let min = Infinity, max = -Infinity;
    for (let i = 0; i < data.length; i++) {
        if (data[i] < min) min = data[i];
        if (data[i] > max) max = data[i];
    }
    const range = (max - min) || 1;
    for (let i = 0; i < data.length; i++) {
        const val = ((data[i] - min) / range) * 255;
        imageData.data[i * 4] = imageData.data[i * 4 + 1] = imageData.data[i * 4 + 2] = val;
        imageData.data[i * 4 + 3] = 255;
    }
    ctx.putImageData(imageData, 0, 0);
}

exportBtn.addEventListener('click', () => {
    const link = document.createElement('a');
    link.download = 'stacked_result.png';
    link.href = canvas.toDataURL('image/png');
    link.click();
});
