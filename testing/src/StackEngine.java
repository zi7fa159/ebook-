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
        float dx = alignment.dx;
        float dy = alignment.dy;

        if (frameCount == 0) {
            for (int i = 0; i < width * height; i++) {
                stackBuffer[i] = rawData[i] & 0xFFFF;
            }
        } else {
            float n = (float) frameCount;
            float nextN = n + 1.0f;
            float kappa = 2.5f;

            // Precompute bilinear weights
            // We interpolate in 2x2 steps to stay in the same Bayer phase
            float fx = (dx >= 0) ? (dx % 2) / 2.0f : (2 + (dx % 2)) / 2.0f;
            float fy = (dy >= 0) ? (dy % 2) / 2.0f : (2 + (dy % 2)) / 2.0f;
            if (fx >= 1.0f) fx = 0.999f;
            if (fy >= 1.0f) fy = 0.999f;

            float w00 = (1 - fx) * (1 - fy);
            float w10 = fx * (1 - fy);
            float w01 = (1 - fx) * fy;
            float w11 = fx * fy;

            int sdx = (int) Math.floor(dx / 2) * 2;
            int sdy = (int) Math.floor(dy / 2) * 2;

            for (int y = 2; y < height - 2; y++) {
                int sy = y - sdy;
                if (sy < 2 || sy >= height - 2) continue;

                int rowOff = y * width;
                int sRowOff = sy * width;

                for (int x = 2; x < width - 2; x++) {
                    int sx = x - sdx;
                    if (sx < 2 || sx >= width - 2) continue;

                    // Bilinear interpolation from 4 Bayer-phase-matched neighbors
                    float v00 = rawData[sRowOff + sx] & 0xFFFF;
                    float v10 = rawData[sRowOff + sx - 2] & 0xFFFF; // dx is shift of frame relative to ref
                    float v01 = rawData[(sy - 2) * width + sx] & 0xFFFF;
                    float v11 = rawData[(sy - 2) * width + sx - 2] & 0xFFFF;

                    // Wait, if dx is positive, the frame moved RIGHT. To align with ref, we look LEFT.
                    // So neighbors are at sx, sx-2, sy, sy-2. Correct.

                    float val = v00 * w00 + v10 * w10 + v01 * w01 + v11 * w11;
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
        for (int i = 0; i < stackBuffer.length; i++) {
            stackBuffer[i] = 0;
        }
        if (m2Buffer != null) {
            for (int i = 0; i < m2Buffer.length; i++) {
                m2Buffer[i] = 0;
            }
        }
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
