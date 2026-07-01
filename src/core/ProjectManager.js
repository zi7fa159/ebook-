import { SERParser } from '../io/SERParser.js';
import { FITSParser } from '../io/FITSParser.js';
import { VideoParser } from '../io/VideoParser.js';
import { ImageFrame } from './ImageFrame.js';
import { APManager } from './APManager.js';
import { WorkerPool } from './WorkerPool.js';

export class ProjectManager {
    constructor() {
        this.currentProject = null;
        this.frames = [];
        this.metadata = null;
        this.analysisResults = [];
        this.registrationResults = [];
        this.aps = [];
        this.analysisPool = new WorkerPool('src/workers/AnalysisWorker.js');
        this.stackingPool = new WorkerPool('src/workers/StackingWorker.js');
        this.stackedFrame = null;
        this.bestFrameCache = null;
    }

    async loadFile(file) {
        const extension = file.name.split('.').pop().toLowerCase();
        if (extension === 'ser') {
            await this.loadSER(file);
        } else if (['fits', 'fit', 'fts'].includes(extension)) {
            await this.loadFITS(file);
        } else if (['mp4', 'mov', 'avi', 'webm'].includes(extension)) {
            await this.loadVideo(file);
        } else {
            throw new Error(`Unsupported file type: ${extension}`);
        }
    }

    async loadFITS(file) {
        const parser = new FITSParser(file);
        await parser.parse();
        const frame = await parser.getImageData();
        this.metadata = {
            imageWidth: frame.width,
            imageHeight: frame.height,
            frameCount: 1,
            colorID: 0
        };
        this.currentFrame = frame;
        this.bestFrameCache = frame;
        this.currentProject = { type: 'fits', file: file, parser: parser };
    }

    async loadVideo(file, maxFrames = Infinity) {
        const parser = new VideoParser(file);
        await parser.init();
        this.metadata = {
            ...parser.metadata,
            frameCount: Math.min(parser.metadata.frameCount, maxFrames),
            colorID: 100 // RGB
        };
        this.currentProject = {
            type: 'video',
            parser: parser,
            file: file
        };
        console.log("Video metadata:", this.metadata);
        // For preview, just extract the first frame
        await parser.getFrames((frame) => {
            this.currentFrame = frame;
        }, 1);
        console.log("Video first frame loaded");
    }

    async loadSER(file) {
        const parser = new SERParser(file);
        await parser.parseHeader();
        this.metadata = parser.metadata;

        this.currentProject = {
            type: 'ser',
            parser: parser,
            file: file
        };

        const rawFrame = await parser.getFrame(0);
        const frame = ImageFrame.fromSER(rawFrame, this.metadata);
        this.currentFrame = frame;
    }

    async analyzeFrames(onProgress) {
        const parser = this.currentProject.parser;
        const count = this.metadata.frameCount;
        console.log("Starting analysis, count:", count);

        if (this.currentProject.type === 'video') {
            this.analysisResults = [];
            let bestScore = -1;
            const res = await parser.getFrames(async (frame) => {
                console.log("Analyzing frame", frame.index);
                const result = await this.analysisPool.run({
                    type: 'analyze',
                    frameData: frame.data,
                    width: frame.width,
                    height: frame.height,
                    channels: frame.channels,
                    frameIndex: frame.index
                });
                this.analysisResults.push(result);
                if (result.score > bestScore) {
                    bestScore = result.score;
                    this.bestFrameCache = { ...frame, data: new Float32Array(frame.data) };
                }
                if (onProgress) onProgress(this.analysisResults.length / count);
            });
            console.log("Video analysis finished. Frames extracted:", res);
            return;
        }

        this.analysisResults = new Array(count);
        let bestScore = -1;
        const chunkSize = 20;
        for (let i = 0; i < count; i += chunkSize) {
            const promises = [];
            for (let j = 0; j < chunkSize && (i + j) < count; j++) {
                const idx = i + j;
                promises.push((async () => {
                    const rawFrame = await parser.getFrame(idx);
                    const frame = ImageFrame.fromSER(rawFrame, this.metadata);
                    const result = await this.analysisPool.run({
                        type: 'analyze',
                        frameData: frame.data,
                        width: frame.width,
                        height: frame.height,
                        channels: frame.channels,
                        frameIndex: idx
                    });
                    this.analysisResults[idx] = result;
                    if (result.score > bestScore) {
                        bestScore = result.score;
                        this.bestFrameCache = frame;
                    }
                    if (onProgress) onProgress((idx + 1) / count);
                })());
            }
            await Promise.all(promises);
        }
    }

    async registerFrames(onProgress) {
        const parser = this.currentProject.parser;
        // Generate APs on the best frame
        this.aps = APManager.generatePoints(this.bestFrameCache);
        const count = this.analysisResults.length;
        this.registrationResults = new Array(count);

        const refFrameData = {
            data: this.bestFrameCache.data,
            width: this.bestFrameCache.width,
            height: this.bestFrameCache.height,
            channels: this.bestFrameCache.channels
        };

        if (this.currentProject.type === 'video') {
            await parser.getFrames(async (frame) => {
                const result = await this.analysisPool.run({
                    type: 'register',
                    frameData: frame.data,
                    width: frame.width,
                    height: frame.height,
                    channels: frame.channels,
                    frameIndex: frame.index,
                    refFrame: refFrameData,
                    aps: this.aps
                });
                this.registrationResults[frame.index] = {
                    alignment: result.alignment,
                    aps: result.aps
                };
                if (onProgress) onProgress((frame.index + 1) / count);
            });
            return;
        }

        const chunkSize = 20;
        for (let i = 0; i < count; i += chunkSize) {
            const promises = [];
            for (let j = 0; j < chunkSize && (i + j) < count; j++) {
                const idx = i + j;
                promises.push((async () => {
                    const rawFrame = await parser.getFrame(idx);
                    const frame = ImageFrame.fromSER(rawFrame, this.metadata);
                    const result = await this.analysisPool.run({
                        type: 'register',
                        frameData: frame.data,
                        width: frame.width,
                        height: frame.height,
                        channels: frame.channels,
                        frameIndex: idx,
                        refFrame: refFrameData,
                        aps: this.aps
                    });
                    this.registrationResults[idx] = {
                        alignment: result.alignment,
                        aps: result.aps
                    };
                    if (onProgress) onProgress((idx + 1) / count);
                })());
            }
            await Promise.all(promises);
        }
    }

    async stackFrames(percentage, onProgress) {
        const parser = this.currentProject.parser;
        const count = this.analysisResults.length;
        const numToStack = Math.max(1, Math.floor(count * (percentage / 100)));

        const sortedIndices = this.analysisResults
            .map((r, i) => ({ index: i, score: r.score }))
            .sort((a, b) => b.score - a.score)
            .slice(0, numToStack)
            .map(r => r.index);

        const width = this.metadata.imageWidth;
        const height = this.metadata.imageHeight;
        let channels = this.metadata.colorID >= 100 ? 3 : 1;

        let accumulator = new Float64Array(width * height * channels);
        let totalWeight = 0;
        const maxScore = Math.max(...this.analysisResults.map(r => r.score));

        if (this.currentProject.type === 'video') {
            const sortedSet = new Set(sortedIndices);
            let processed = 0;
            await parser.getFrames(async (frame) => {
                if (sortedSet.has(frame.index)) {
                    const weight = this.analysisResults[frame.index].score / maxScore;
                    totalWeight += weight;
                    const result = await this.stackingPool.run({
                        type: 'stack_chunk',
                        frames: [frame.data],
                        registrationResults: [this.registrationResults[frame.index]],
                        weights: [weight],
                        width,
                        height,
                        channels,
                        accumulator: accumulator
                    });
                    accumulator = result.accumulator;
                    processed++;
                    if (onProgress) onProgress(processed / sortedIndices.length);
                }
            });
        } else {
            const chunkSize = 10;
            for (let i = 0; i < sortedIndices.length; i += chunkSize) {
                const chunkIndices = sortedIndices.slice(i, i + chunkSize);
                const framesToStack = [];
                const regs = [];
                const weights = [];

                for (const idx of chunkIndices) {
                    const raw = await parser.getFrame(idx);
                    const frame = ImageFrame.fromSER(raw, this.metadata);
                    framesToStack.push(frame.data);
                    regs.push(this.registrationResults[idx]);
                    const weight = this.analysisResults[idx].score / maxScore;
                    weights.push(weight);
                    totalWeight += weight;
                }

                const result = await this.stackingPool.run({
                    type: 'stack_chunk',
                    frames: framesToStack,
                    registrationResults: regs,
                    weights: weights,
                    width,
                    height,
                    channels,
                    accumulator: accumulator
                });
                accumulator = result.accumulator;
                if (onProgress) onProgress((i + chunkIndices.length) / sortedIndices.length);
            }
        }

        const finalData = new Float32Array(accumulator.length);
        for (let i = 0; i < accumulator.length; i++) {
            finalData[i] = accumulator[i] / totalWeight;
        }

        this.stackedFrame = {
            width,
            height,
            channels,
            data: finalData
        };
    }
}
