package com.alsclone.astrostacker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.TextureView;

public class Renderer {
    private final TextureView textureView;
    private final Paint paint = new Paint();
    private Bitmap previewBitmap;
    private int[] argbBuffer;

    // Default color gains for Realme 8i sensor (approximate)
    private float redGain = 1.6f;
    private float greenGain = 1.0f;
    private float blueGain = 2.1f;
    private int blackLevel = 64; // Common black level for many sensors
    private float whiteLevel = 1023.0f; // 10-bit typical, update from logs if different

    public Renderer(TextureView textureView) {
        this.textureView = textureView;
    }

    public void setBlackLevel(int bl) { this.blackLevel = bl; }
    public void setWhiteLevel(float wl) { this.whiteLevel = wl; }

    public void setWbGains(float r, float g, float b) {
        this.redGain = r;
        this.greenGain = g;
        this.blueGain = b;
    }

    public synchronized void updateLive(short[] rawBuffer, int width, int height) {
        processAndDraw(null, rawBuffer, width, height);
    }

    public synchronized void updateStack(float[] stackBuffer, int width, int height) {
        processAndDraw(stackBuffer, null, width, height);
    }

    private void processAndDraw(float[] stackBuffer, short[] rawBuffer, int width, int height) {
        // Downscale for preview performance (always 4x downscale)
        int sw = width / 2;
        int sh = height / 2;

        if (previewBitmap == null || previewBitmap.getWidth() != sw || previewBitmap.getHeight() != sh) {
            previewBitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
            argbBuffer = new int[sw * sh];
        }

        // Auto-brightness scaling for preview - use 99th percentile approx
        float maxObserved = 0;
        int sampleCount = 0;
        float sum = 0;
        int len = (stackBuffer != null) ? stackBuffer.length : rawBuffer.length;
        for (int i = 0; i < len; i += 2000) {
            float val = (stackBuffer != null) ? stackBuffer[i] : (rawBuffer[i] & 0xFFFF);
            if (val > maxObserved) maxObserved = val;
            sum += val;
            sampleCount++;
        }
        float avg = sum / sampleCount;
        // Robust max: use a mix of avg and max to avoid hot pixel dominance
        float robustMax = (maxObserved + avg * 10) / 11.0f;
        float scale = 255.0f / (Math.max(1, robustMax - blackLevel));

        // Simple Debayering for live preview (assume RGGB)
        // Downscale while debayering for speed
        for (int y = 0; y < sh; y++) {
            int origY = y * 2;
            for (int x = 0; x < sw; x++) {
                int origX = x * 2;

                // RGGB
                float r, g1, g2, b;
                if (stackBuffer != null) {
                    r = (stackBuffer[origY * width + origX] - blackLevel) * redGain;
                    g1 = (stackBuffer[origY * width + (origX + 1)] - blackLevel) * greenGain;
                    g2 = (stackBuffer[(origY + 1) * width + origX] - blackLevel) * greenGain;
                    b = (stackBuffer[(origY + 1) * width + (origX + 1)] - blackLevel) * blueGain;
                } else {
                    r = ((rawBuffer[origY * width + origX] & 0xFFFF) - blackLevel) * redGain;
                    g1 = ((rawBuffer[origY * width + (origX + 1)] & 0xFFFF) - blackLevel) * greenGain;
                    g2 = ((rawBuffer[(origY + 1) * width + origX] & 0xFFFF) - blackLevel) * greenGain;
                    b = ((rawBuffer[(origY + 1) * width + (origX + 1)] & 0xFFFF) - blackLevel) * blueGain;
                }

                float g = (g1 + g2) / 2.0f;

                // Apply a simple sqrt stretch for better visibility of faint stars (ALS style)
                int ri = (int) (Math.sqrt(Math.max(0, r * scale) / 255.0) * 255.0);
                int gi = (int) (Math.sqrt(Math.max(0, g * scale) / 255.0) * 255.0);
                int bi = (int) (Math.sqrt(Math.max(0, b * scale) / 255.0) * 255.0);

                ri = Math.min(255, ri);
                gi = Math.min(255, gi);
                bi = Math.min(255, bi);

                argbBuffer[y * sw + x] = 0xFF000000 | (ri << 16) | (gi << 8) | bi;
            }
        }

        previewBitmap.setPixels(argbBuffer, 0, sw, 0, 0, sw, sh);
        draw();
    }

    private void draw() {
        Canvas canvas = textureView.lockCanvas();
        if (canvas != null) {
            if (previewBitmap != null) {
                Rect src = new Rect(0, 0, previewBitmap.getWidth(), previewBitmap.getHeight());
                Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
                canvas.drawBitmap(previewBitmap, src, dst, paint);
            }
            textureView.unlockCanvasAndPost(canvas);
        }
    }
}
