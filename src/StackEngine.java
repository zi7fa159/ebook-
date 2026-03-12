package com.alsclone.astrostacker;

public class StackEngine {
    private final int width;
    private final int height;
    private float[] stackBuffer; // Now stores RGB: [R, G, B, R, G, B, ...]
    private float[] m2Buffer;
    private int frameCount = 0;
    private int stackMethod = 0; // 0: Mean, 1: Sum, 2: Sigma Clip
    private int cfaPattern = 0;

    public StackEngine(int width, int height) {
        this.width = width;
        this.height = height;
        // RGB Stacking: 50MP * 3 channels * 4 bytes = 600MB
        this.stackBuffer = new float[width * height * 3];
    }

    public void setCfaPattern(int cfa) {
        this.cfaPattern = cfa;
    }

    public synchronized void setStackMethod(int method) {
        this.stackMethod = method;
        if (method == 2 && m2Buffer == null) {
            m2Buffer = new float[width * height * 3];
        }
    }

    public synchronized void addFrame(short[] rawData, FrameAligner.Alignment alignment) {
        if (frameCount == 0) {
            // Reference frame de-bayering
            for (int y = 0; y < height; y += 2) {
                for (int x = 0; x < width; x += 2) {
                    fillRgbFromBayer(rawData, x, y, x, y);
                }
            }
        } else {
            float angleRad = (float) Math.toRadians(alignment.angle);
            float cosA = (float) Math.cos(angleRad);
            float sinA = (float) Math.sin(angleRad);
            float cx = width / 2.0f;
            float cy = height / 2.0f;

            float n = (float) frameCount;
            float nextN = n + 1.0f;
            float kappa = 2.5f;

            for (int y = 0; y < height; y += 2) {
                for (int x = 0; x < width; x += 2) {
                    // Rotate and translate back to source coordinates
                    // Dest (x,y) -> Source (sx, sy)
                    float tx = x - cx;
                    float ty = y - cy;
                    float sx = tx * cosA + ty * sinA + cx - alignment.dx;
                    float sy = -tx * sinA + ty * cosA + cy - alignment.dy;

                    int isx = Math.round(sx / 2) * 2;
                    int isy = Math.round(sy / 2) * 2;

                    if (isx >= 0 && isx < width - 1 && isy >= 0 && isy < height - 1) {
                        updateRgb(rawData, isx, isy, x, y, n, nextN, kappa);
                    }
                }
            }
        }
        frameCount++;
    }

    private void fillRgbFromBayer(short[] raw, int sx, int sy, int dx, int dy) {
        int sIdx = sy * width + sx;
        float v00 = raw[sIdx] & 0xFFFF;
        float v01 = raw[sIdx + 1] & 0xFFFF;
        float v10 = raw[sIdx + width] & 0xFFFF;
        float v11 = raw[sIdx + width + 1] & 0xFFFF;

        float r, g, b;
        switch(cfaPattern) {
            case 1: r=v01; g=(v00+v11)/2f; b=v10; break;
            case 2: r=v10; g=(v00+v11)/2f; b=v01; break;
            case 3: r=v11; g=(v01+v10)/2f; b=v00; break;
            default: r=v00; g=(v01+v10)/2f; b=v11; break;
        }

        int dIdx = (dy * width + dx) * 3;
        stackBuffer[dIdx] = r;
        stackBuffer[dIdx+1] = g;
        stackBuffer[dIdx+2] = b;
        // Fill 2x2 block in stack buffer to maintain resolution
        copyRgb(dIdx, (dy * width + dx + 1) * 3);
        copyRgb(dIdx, ((dy + 1) * width + dx) * 3);
        copyRgb(dIdx, ((dy + 1) * width + dx + 1) * 3);
    }

    private void copyRgb(int src, int dst) {
        stackBuffer[dst] = stackBuffer[src];
        stackBuffer[dst+1] = stackBuffer[src+1];
        stackBuffer[dst+2] = stackBuffer[src+2];
    }

    private void updateRgb(short[] raw, int sx, int sy, int dx, int dy, float n, float nextN, float kappa) {
        int sIdx = sy * width + sx;
        float v00 = raw[sIdx] & 0xFFFF;
        float v01 = raw[sIdx + 1] & 0xFFFF;
        float v10 = raw[sIdx + width] & 0xFFFF;
        float v11 = raw[sIdx + width + 1] & 0xFFFF;

        float r, g, b;
        switch(cfaPattern) {
            case 1: r=v01; g=(v00+v11)/2f; b=v10; break;
            case 2: r=v10; g=(v00+v11)/2f; b=v01; break;
            case 3: r=v11; g=(v01+v10)/2f; b=v00; break;
            default: r=v00; g=(v01+v10)/2f; b=v11; break;
        }

        accumulate(dx, dy, r, g, b, n, nextN, kappa);
        accumulate(dx+1, dy, r, g, b, n, nextN, kappa);
        accumulate(dx, dy+1, r, g, b, n, nextN, kappa);
        accumulate(dx+1, dy+1, r, g, b, n, nextN, kappa);
    }

    private void accumulate(int x, int y, float r, float g, float b, float n, float nextN, float kappa) {
        int idx = (y * width + x) * 3;
        if (stackMethod == 0) { // Mean
            stackBuffer[idx] = (stackBuffer[idx] * n + r) / nextN;
            stackBuffer[idx+1] = (stackBuffer[idx+1] * n + g) / nextN;
            stackBuffer[idx+2] = (stackBuffer[idx+2] * n + b) / nextN;
        } else if (stackMethod == 1) { // Sum
            stackBuffer[idx] += r;
            stackBuffer[idx+1] += g;
            stackBuffer[idx+2] += b;
        } else if (stackMethod == 2) { // Sigma
            accumulateSigma(idx, r, n, nextN, kappa);
            accumulateSigma(idx+1, g, n, nextN, kappa);
            accumulateSigma(idx+2, b, n, nextN, kappa);
        }
    }

    private void accumulateSigma(int idx, float val, float n, float nextN, float kappa) {
        float mean = stackBuffer[idx];
        float m2 = m2Buffer[idx];
        if (n > 4) {
            float std = (float) Math.sqrt(m2 / n);
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
