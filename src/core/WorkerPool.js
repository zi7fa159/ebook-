export class WorkerPool {
    constructor(workerPath, poolSize = navigator.hardwareConcurrency || 4) {
        this.workerPath = workerPath;
        this.poolSize = poolSize;
        this.workers = [];
        this.idleWorkers = [];
        this.queue = [];

        for (let i = 0; i < this.poolSize; i++) {
            const worker = new Worker(this.workerPath, { type: 'module' });
            this.workers.push(worker);
            this.idleWorkers.push(worker);
        }
    }

    run(task) {
        return new Promise((resolve, reject) => {
            const job = { task, resolve, reject };
            this.queue.push(job);
            this.processQueue();
        });
    }

    processQueue() {
        if (this.queue.length === 0 || this.idleWorkers.length === 0) return;

        const worker = this.idleWorkers.pop();
        const job = this.queue.shift();

        const handler = (e) => {
            worker.removeEventListener('message', handler);
            worker.removeEventListener('error', errorHandler);
            this.idleWorkers.push(worker);
            job.resolve(e.data);
            this.processQueue();
        };

        const errorHandler = (e) => {
            worker.removeEventListener('message', handler);
            worker.removeEventListener('error', errorHandler);
            this.idleWorkers.push(worker);
            job.reject(e);
            this.processQueue();
        };

        worker.addEventListener('message', handler);
        worker.addEventListener('error', errorHandler);
        worker.postMessage(job.task);
    }
}
