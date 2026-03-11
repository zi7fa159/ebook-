package com.example.astrostacker;

public class StackEngine {
    public enum Mode { AVERAGE, ADDITIVE }

    private float[] stackBuffer;
    private int width, height;
    private int frameCount = 0;
    private Mode mode = Mode.AVERAGE;

    public StackEngine(int width, int height) {
        this.width = width;
        this.height = height;
        this.stackBuffer = new float[width * height];
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * Adds a frame to the stack.
     * @param yData Y-plane byte data
     * @param rowStride row stride of the input data
     * @param dx horizontal displacement of the incoming frame relative to reference
     * @param dy vertical displacement of the incoming frame relative to reference
     */
    public void addFrame(byte[] yData, int rowStride, float dx, float dy) {
        frameCount++;

        // Integer shift for speed (as requested in optimizations)
        int shiftX = Math.round(dx);
        int shiftY = Math.round(dy);

        for (int y = 0; y < height; y++) {
            int srcY = y + shiftY;
            if (srcY < 0 || srcY >= height) continue;

            int destRowOffset = y * width;
            int srcRowOffset = srcY * rowStride;

            for (int x = 0; x < width; x++) {
                int srcX = x + shiftX;
                if (srcX < 0 || srcX >= width) continue;

                int destIdx = destRowOffset + x;
                float newVal = (float)(yData[srcRowOffset + srcX] & 0xFF);

                if (mode == Mode.AVERAGE) {
                    // Incremental averaging: S_n = (S_{n-1} * (n-1) + x_n) / n
                    stackBuffer[destIdx] = (stackBuffer[destIdx] * (frameCount - 1) + newVal) / frameCount;
                } else {
                    // Additive stacking
                    stackBuffer[destIdx] += newVal;
                }
            }
        }
    }

    public void reset() {
        frameCount = 0;
        if (stackBuffer != null) {
            java.util.Arrays.fill(stackBuffer, 0.0f);
        }
    }

    public float[] getStackBuffer() {
        return stackBuffer;
    }

    public int getFrameCount() {
        return frameCount;
    }
}
