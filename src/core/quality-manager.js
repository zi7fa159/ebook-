export class QualityManager {
    constructor(numWorkers = navigator.hardwareConcurrency || 4) {
        this.workers = [];
        this.numWorkers = numWorkers;
        this.queue = [];
        this.results = new Map();
        this.onComplete = null;
        this.onProgress = null;
        this.totalTasks = 0;
        this.finishedTasks = 0;

        for (let i = 0; i < numWorkers; i++) {
            const worker = new Worker(new URL('../workers/quality-worker.js', import.meta.url), { type: 'module' });
            worker.onmessage = this.handleWorkerMessage.bind(this);
            worker.busy = false;
            this.workers.push(worker);
        }
    }

    handleWorkerMessage(e) {
        const { type, id, score, details } = e.data;
        if (type === 'QUALITY_RESULT') {
            this.results.set(id, { score, details });
            this.finishedTasks++;

            if (this.onProgress) {
                this.onProgress(this.finishedTasks / this.totalTasks);
            }

            const worker = this.workers.find(w => w === e.target || (e.target && w === e.currentTarget));
            // Find the worker that sent the message
            const sourceWorker = this.workers.find(w => w === e.target);
            if (sourceWorker) sourceWorker.busy = false;

            this.processQueue();
        }
    }

    analyzeFrames(frames) {
        this.queue = frames.map((f, i) => ({ id: i, data: f.data, width: f.width, height: f.height }));
        this.totalTasks = this.queue.length;
        this.finishedTasks = 0;
        this.results.clear();
        this.processQueue();
    }

    processQueue() {
        if (this.finishedTasks === this.totalTasks && this.totalTasks > 0) {
            if (this.onComplete) this.onComplete(Array.from(this.results.entries()).sort((a, b) => a[0] - b[0]));
            return;
        }

        for (const worker of this.workers) {
            if (!worker.busy && this.queue.length > 0) {
                const task = this.queue.shift();
                worker.busy = true;
                worker.postMessage({
                    type: 'ANALYZE_QUALITY',
                    id: task.id,
                    frameData: task.data,
                    width: task.width,
                    height: task.height
                }, [task.data.buffer]); // Transfer buffer
            }
        }
    }
}
