package com.alsclone.astrostacker;

public class StackEngine {
    private final int width;
    private final int height;
    private float[] stackBuffer; // Bayer float buffer
    private float[] m2Buffer;
    private int frameCount = 0;
    private int stackMethod = 0; // 0: Mean, 1: Sum, 2: Sigma Clip

    public StackEngine(int width, int height) {
        this.width = width;
        this.height = height;
        // Bayer Stacking: 50MP * 4 bytes = 200MB (STABLE)
        this.stackBuffer = new float[width * height];
    }

    public synchronized void setStackMethod(int method) {
        this.stackMethod = method;
        if (method == 2 && m2Buffer == null) {
            m2Buffer = new float[width * height];
        }
    }

    public synchronized void addFrame(short[] rawData, FrameAligner.Alignment alignment) {
        // ALS uses translation only for stability on 50MP
        int dx = Math.round(alignment.dx / 2) * 2;
        int dy = Math.round(alignment.dy / 2) * 2;

        if (stackBuffer == null) {
            stackBuffer = new float[width * height];
        }

        if (frameCount == 0) {
            for (int i = 0; i < width * height; i++) {
                stackBuffer[i] = rawData[i] & 0xFFFF;
            }
        } else {
            float n = (float) frameCount;
            float nextN = n + 1.0f;
            float kappa = 2.5f;

            for (int y = 0; y < height; y++) {
                int sy = y - dy;
                if (sy < 0 || sy >= height) continue;
                int rowOff = y * width;
                int sRowOff = sy * width;
                for (int x = 0; x < width; x++) {
                    int sx = x - dx;
                    if (sx < 0 || sx >= width) continue;
                    float val = rawData[sRowOff + sx] & 0xFFFF;
                    int idx = rowOff + x;

                    if (stackMethod == 0) { // Mean
                        stackBuffer[idx] = (stackBuffer[idx] * n + val) / nextN;
                    } else if (stackMethod == 1) { // Sum
                        stackBuffer[idx] += val;
                    } else if (stackMethod == 2) { // Sigma
                        accumulateSigma(idx, val, n, nextN, kappa);
                    }
                }
            }
        }
        frameCount++;
    }

    private void accumulateSigma(int idx, float val, float n, float nextN, float kappa) {
        float mean = stackBuffer[idx];
        float m2 = m2Buffer[idx];
        if (n > 4) {
            float std = (float) Math.sqrt(Math.max(0, m2 / n));
            if (Math.abs(val - mean) > kappa * std) return;
        }
        float delta = val - mean;
        float newMean = mean + delta / nextN;
        stackBuffer[idx] = newMean;
        m2Buffer[idx] += delta * (val - newMean);
    }

    public synchronized void reset() {
        frameCount = 0;
        if (stackBuffer != null) {
            for (int i = 0; i < stackBuffer.length; i++) {
                stackBuffer[i] = 0;
            }
        }
        if (m2Buffer != null) {
            for (int i = 0; i < m2Buffer.length; i++) {
                m2Buffer[i] = 0;
            }
        }
    }

    public synchronized void clearMemory() {
        frameCount = 0;
        stackBuffer = null;
        m2Buffer = null;
        System.gc();
    }

    public int getFrameCount() {
        return frameCount;
    }

    public float[] getStackBuffer() {
        return stackBuffer;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
