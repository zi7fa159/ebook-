package com.example.astrostacker;

public class RawStackEngine {
    private float[] stackBuffer;
    private float[] darkFrame;
    private int width, height;
    private int frameCount = 0;

    public RawStackEngine(int width, int height) {
        this.width = width;
        this.height = height;
        this.stackBuffer = new float[width * height];
    }

    public void setDarkFrame(float[] dark) {
        this.darkFrame = dark;
    }

    public void addFrame(short[] rawData, float dx, float dy) {
        frameCount++;

        // Maintain Bayer phase by shifting by multiples of 2
        int shiftX = ((int)Math.round(dx) / 2) * 2;
        int shiftY = ((int)Math.round(dy) / 2) * 2;

        for (int y = 0; y < height; y++) {
            int srcY = y + shiftY;
            if (srcY < 0 || srcY >= height) continue;

            int destRowOffset = y * width;
            int srcRowOffset = srcY * width;

            for (int x = 0; x < width; x++) {
                int srcX = x + shiftX;
                if (srcX < 0 || srcX >= width) continue;

                int destIdx = destRowOffset + x;
                int srcIdx = srcRowOffset + srcX;
                float newVal = (float)(rawData[srcIdx] & 0xFFFF);

                if (darkFrame != null) {
                    newVal -= darkFrame[srcIdx];
                    if (newVal < 0) newVal = 0;
                }

                // Incremental average
                stackBuffer[destIdx] = (stackBuffer[destIdx] * (frameCount - 1) + newVal) / frameCount;
            }
        }
    }

    public void reset() {
        frameCount = 0;
        java.util.Arrays.fill(stackBuffer, 0.0f);
    }

    public float[] getStackBuffer() { return stackBuffer; }
    public int getFrameCount() { return frameCount; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
