package com.alsclone.astrostacker;

public class StackEngine {
    private final int width;
    private final int height;
    private float[] stackBuffer; // running mean
    private float[] m2Buffer;    // running squared deviation
    private short[] nBuffer;     // per-pixel sample count
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
        if (method == 2) {
            if (m2Buffer == null) m2Buffer = new float[width * height];
            if (nBuffer == null) nBuffer = new short[width * height];
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
                if (nBuffer != null) nBuffer[i] = 1;
            }
        } else {
            float n_global = (float) frameCount;
            float nextN_global = n_global + 1.0f;

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
                        stackBuffer[idx] = (stackBuffer[idx] * n_global + val) / nextN_global;
                    } else if (stackMethod == 1) { // Sum
                        stackBuffer[idx] += val;
                    } else if (stackMethod == 2) { // Robust Sigma
                        robustSigmaClip(rawData, idx, sx, sy, width, val);
                    }
                }
            }
        }
        frameCount++;
    }

    private void robustSigmaClip(short[] rawData, int idx, int sx, int sy, int w, float val) {
        // Optional hot pixel pre-check (only if not at edges, using same Bayer phase neighbors)
        if (sx >= 2 && sx < w - 2 && sy >= 2 && sy < height - 2) {
            float m = getLocalMedianSamePhase(rawData, sx, sy, w);
            if (val > 5 * m && m > 0) val = m;
        }

        float mean = stackBuffer[idx];
        float m2 = m2Buffer[idx];
        int n = nBuffer[idx] & 0xFFFF;

        float kappa = 2.3f;
        float epsilon = 1e-6f;

        boolean accept = true;
        if (n >= 5) {
            float variance = m2 / (n - 1);
            float sigma = (float) Math.sqrt(variance);
            if (sigma < epsilon) sigma = epsilon;

            if (Math.abs(val - mean) > kappa * sigma) {
                accept = false;
            }
        }

        if (accept) {
            n++;
            nBuffer[idx] = (short) n;
            float delta = val - mean;
            mean += delta / n;
            float delta2 = val - mean;
            m2 += delta * delta2;

            stackBuffer[idx] = mean;
            m2Buffer[idx] = m2;
        }
    }

    private float getLocalMedianSamePhase(short[] data, int sx, int sy, int w) {
        // Neighbors 2 pixels away to maintain same Bayer color
        float[] p = new float[9];
        p[0] = data[(sy-2)*w + (sx-2)] & 0xFFFF;
        p[1] = data[(sy-2)*w + sx] & 0xFFFF;
        p[2] = data[(sy-2)*w + (sx+2)] & 0xFFFF;
        p[3] = data[sy*w + (sx-2)] & 0xFFFF;
        p[4] = data[sy*w + sx] & 0xFFFF;
        p[5] = data[sy*w + (sx+2)] & 0xFFFF;
        p[6] = data[(sy+2)*w + (sx-2)] & 0xFFFF;
        p[7] = data[(sy+2)*w + sx] & 0xFFFF;
        p[8] = data[(sy+2)*w + (sx+2)] & 0xFFFF;

        java.util.Arrays.sort(p);
        return p[4];
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
        if (nBuffer != null) {
            for (int i = 0; i < nBuffer.length; i++) {
                nBuffer[i] = 0;
            }
        }
    }

    public synchronized void clearMemory() {
        frameCount = 0;
        stackBuffer = null;
        m2Buffer = null;
        nBuffer = null;
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
