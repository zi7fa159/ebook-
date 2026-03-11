package com.example.astrostacker;

public class StackEngine {
    public enum Mode { AVERAGE, ADDITIVE }

    private float[] stackY, stackU, stackV;
    private int width, height;
    private int uvWidth, uvHeight;
    private int frameCount = 0;
    private Mode mode = Mode.AVERAGE;

    public StackEngine(int width, int height) {
        this.width = width;
        this.height = height;
        this.uvWidth = width / 2;
        this.uvHeight = height / 2;
        this.stackY = new float[width * height];
        this.stackU = new float[uvWidth * uvHeight];
        this.stackV = new float[uvWidth * uvHeight];
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void addFrame(byte[] yData, int yStride, byte[] uData, int uStride, byte[] vData, int vStride, float dx, float dy) {
        frameCount++;

        int shiftX = Math.round(dx);
        int shiftY = Math.round(dy);

        // Stack Y plane
        stackPlane(stackY, yData, width, height, yStride, shiftX, shiftY);

        // Stack U and V planes (subsampled)
        stackPlane(stackU, uData, uvWidth, uvHeight, uStride, shiftX / 2, shiftY / 2);
        stackPlane(stackV, vData, uvWidth, uvHeight, vStride, shiftX / 2, shiftY / 2);
    }

    private void stackPlane(float[] stack, byte[] data, int w, int h, int stride, int sx, int sy) {
        for (int y = 0; y < h; y++) {
            int srcY = y + sy;
            if (srcY < 0 || srcY >= h) continue;

            int destRowOffset = y * w;
            int srcRowOffset = srcY * stride;

            for (int x = 0; x < w; x++) {
                int srcX = x + sx;
                if (srcX < 0 || srcX >= w) continue;

                int destIdx = destRowOffset + x;
                float newVal = (float)(data[srcRowOffset + srcX] & 0xFF);

                if (mode == Mode.AVERAGE) {
                    stack[destIdx] = (stack[destIdx] * (frameCount - 1) + newVal) / frameCount;
                } else {
                    stack[destIdx] += newVal;
                }
            }
        }
    }

    public void reset() {
        frameCount = 0;
        java.util.Arrays.fill(stackY, 0.0f);
        java.util.Arrays.fill(stackU, 0.0f);
        java.util.Arrays.fill(stackV, 0.0f);
    }

    public float[] getStackY() { return stackY; }
    public float[] getStackU() { return stackU; }
    public float[] getStackV() { return stackV; }
    public int getFrameCount() { return frameCount; }
    public Mode getMode() { return mode; }
}
