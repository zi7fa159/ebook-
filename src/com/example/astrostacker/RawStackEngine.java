package com.example.astrostacker;

public class RawStackEngine {
    private float[] stackBuffer;
    private int width, height;
    private int frameCount = 0;

    public RawStackEngine(int width, int height) {
        this.width = width;
        this.height = height;
        this.stackBuffer = new float[width * height];
    }

    public void addFrame(short[] rawData, float dx, float dy) {
        frameCount++;
        // Maintain Bayer phase: shift must be even
        int sx = ((int)Math.round(dx) / 2) * 2;
        int sy = ((int)Math.round(dy) / 2) * 2;

        for (int y = 0; y < height; y++) {
            int srcY = y + sy;
            if (srcY < 0 || srcY >= height) continue;
            int destOff = y * width;
            int srcOff = srcY * width;
            for (int x = 0; x < width; x++) {
                int srcX = x + sx;
                if (srcX < 0 || srcX >= width) continue;
                float val = (float)(rawData[srcOff + srcX] & 0xFFFF);
                stackBuffer[destOff + x] = (stackBuffer[destOff + x] * (frameCount - 1) + val) / frameCount;
            }
        }
    }

    public void reset() {
        frameCount = 0;
        java.util.Arrays.fill(stackBuffer, 0);
    }

    public float[] getStack() { return stackBuffer; }
    public int getFrameCount() { return frameCount; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
