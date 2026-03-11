package com.alsclone.astrostacker;

public class StackEngine {
    private final int width;
    private final int height;
    private float[] stackBuffer;
    private int frameCount = 0;

    public StackEngine(int width, int height) {
        this.width = width;
        this.height = height;
        // 50MP * 4 bytes (float) = 200MB. Realme 8i has 4GB/6GB RAM. largeHeap=true is needed.
        this.stackBuffer = new float[width * height];
    }

    public synchronized void addFrame(short[] rawData, int dx, int dy) {
        if (frameCount == 0) {
            for (int i = 0; i < width * height; i++) {
                stackBuffer[i] = rawData[i] & 0xFFFF;
            }
        } else {
            // Incremental average: (prev * n + next) / (n + 1)
            // To handle translation, we map rawData[y][x] to stackBuffer[y+dy][x+dx]
            // For simplicity in this 1:1 clone, we assume shift is small and we clip

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
        }
        frameCount++;
    }

    public synchronized void reset() {
        frameCount = 0;
        for (int i = 0; i < stackBuffer.length; i++) {
            stackBuffer[i] = 0;
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
