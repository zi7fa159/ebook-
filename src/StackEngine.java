package com.alsclone.astrostacker;

public class StackEngine {
    private final int width;
    private final int height;
    private float[] stackBuffer;
    private float[] m2Buffer; // For Sigma Clipping (Welford's algorithm)
    private int frameCount = 0;
    private int stackMethod = 0; // 0: Mean, 1: Sum, 2: Sigma Clip

    public StackEngine(int width, int height) {
        this.width = width;
        this.height = height;
        // 50MP * 4 bytes (float) = 200MB. Realme 8i has 4GB/6GB RAM. largeHeap=true is needed.
        this.stackBuffer = new float[width * height];
    }

    public synchronized void setStackMethod(int method) {
        this.stackMethod = method;
        if (method == 2 && m2Buffer == null) {
            m2Buffer = new float[width * height];
        }
    }

    public synchronized void addFrame(short[] rawData, int dx, int dy) {
        if (frameCount == 0) {
            for (int i = 0; i < width * height; i++) {
                stackBuffer[i] = rawData[i] & 0xFFFF;
            }
        } else {
            if (stackMethod == 0) { // Mean
                float n = (float) frameCount;
                float nextN = n + 1.0f;
                for (int y = 0; y < height; y++) {
                    int sy = y + dy;
                    if (sy < 0 || sy >= height) continue;
                    int rowOffset = y * width;
                    int sRowOffset = sy * width;
                    for (int x = 0; x < width; x++) {
                        int sx = x + dx;
                        if (sx < 0 || sx >= width) continue;
                        float newVal = rawData[rowOffset + x] & 0xFFFF;
                        int sIdx = sRowOffset + sx;
                        stackBuffer[sIdx] = (stackBuffer[sIdx] * n + newVal) / nextN;
                    }
                }
            } else if (stackMethod == 1) { // Sum
                for (int y = 0; y < height; y++) {
                    int sy = y + dy;
                    if (sy < 0 || sy >= height) continue;
                    int rowOffset = y * width;
                    int sRowOffset = sy * width;
                    for (int x = 0; x < width; x++) {
                        int sx = x + dx;
                        if (sx < 0 || sx >= width) continue;
                        float newVal = rawData[rowOffset + x] & 0xFFFF;
                        int sIdx = sRowOffset + sx;
                        stackBuffer[sIdx] += newVal;
                    }
                }
            } else if (stackMethod == 2) { // Kappa-Sigma Clipping (High-Quality rejection)
                if (m2Buffer == null) m2Buffer = new float[width * height];
                float n = (float) frameCount;
                float nextN = n + 1.0f;

                // Kappa-Sigma: typically 2.0 to 3.0. Lower = more aggressive rejection of planes/satellites
                float kappa = 2.5f;

                for (int y = 0; y < height; y++) {
                    int sy = y + dy;
                    if (sy < 0 || sy >= height) continue;
                    int rowOffset = y * width;
                    int sRowOffset = sy * width;
                    for (int x = 0; x < width; x++) {
                        int sx = x + dx;
                        if (sx < 0 || sx >= width) continue;
                        float newVal = rawData[rowOffset + x] & 0xFFFF;
                        int sIdx = sRowOffset + sx;

                        float mean = stackBuffer[sIdx];
                        float m2 = m2Buffer[sIdx];

                        if (n > 4) {
                            float variance = m2 / n;
                            float std = (float) Math.sqrt(variance);

                            // Rejection logic: if value is too far from mean, don't include it in average
                            if (Math.abs(newVal - mean) > kappa * std) {
                                continue;
                            }
                        }

                        // Welford's algorithm for online mean and variance
                        float delta = newVal - mean;
                        float newMean = mean + delta / nextN;
                        float delta2 = newVal - newMean;

                        stackBuffer[sIdx] = newMean;
                        m2Buffer[sIdx] += delta * delta2;
                    }
                }
            }
        }
        frameCount++;
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
