package com.example.astrostacker;

public class StackEngine {
    private float[] stackBuffer;
    private int width, height;
    private int frameCount = 0;

    public StackEngine(int width, int height) {
        this.width = width;
        this.height = height;
        this.stackBuffer = new float[width * height];
    }

    public void addFrame(byte[] yData, int rowStride, float dx, float dy) {
        frameCount++;
        int idxDx = Math.round(dx);
        int idxDy = Math.round(dy);

        for (int y = 0; y < height; y++) {
            int srcY = y + idxDy;
            if (srcY < 0 || srcY >= height) continue;

            for (int x = 0; x < width; x++) {
                int srcX = x + idxDx;
                if (srcX < 0 || srcX >= width) continue;

                int srcIdx = srcY * rowStride + srcX;
                int destIdx = y * width + x;

                float newVal = (yData[srcIdx] & 0xFF);
                stackBuffer[destIdx] = (stackBuffer[destIdx] * (frameCount - 1) + newVal) / frameCount;
            }
        }
    }

    public void reset() {
        frameCount = 0;
        for (int i = 0; i < stackBuffer.length; i++) stackBuffer[i] = 0;
    }

    public float[] getStackBuffer() {
        return stackBuffer;
    }

    public int getFrameCount() {
        return frameCount;
    }
}
