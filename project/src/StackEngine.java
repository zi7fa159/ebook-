package com.example.astrostacker;

public class StackEngine {
    private float[] stackBuffer;
    private int width;
    private int height;
    private int frameCount;

    public StackEngine(int width, int height) {
        this.width = width;
        this.height = height;
        this.stackBuffer = new float[width * height];
        this.frameCount = 0;
    }

    public synchronized void addFrame(byte[] grayData, int dx, int dy) {
        if (frameCount == 0) {
            for (int i = 0; i < grayData.length; i++) {
                stackBuffer[i] = (float) (grayData[i] & 0xFF);
            }
        } else {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int srcX = x + dx;
                    int srcY = y + dy;
                    if (srcX >= 0 && srcX < width && srcY >= 0 && srcY < height) {
                        float newValue = (float) (grayData[srcY * width + srcX] & 0xFF);
                        int idx = y * width + x;
                        stackBuffer[idx] = (stackBuffer[idx] * frameCount + newValue) / (frameCount + 1);
                    }
                }
            }
        }
        frameCount++;
    }

    public synchronized void reset() {
        for (int i = 0; i < stackBuffer.length; i++) {
            stackBuffer[i] = 0;
        }
        frameCount = 0;
    }

    public synchronized int getFrameCount() {
        return frameCount;
    }

    public synchronized float[] getStackBuffer() {
        return stackBuffer;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
