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
    private Bitmap rotatedBitmap;
    private int[] argbBuffer;

    private float blackPoint = 0.0f;
    private float whitePoint = 1.0f;
    private boolean useAutoStretch = true;

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

    public void setStretch(float black, float white) {
        this.blackPoint = black;
        this.whitePoint = white;
        this.useAutoStretch = false;
    }

    public void setAutoStretch(boolean auto) {
        this.useAutoStretch = auto;
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

        // Brightness scaling for preview
        float scale;
        if (useAutoStretch) {
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
            float robustMax = (maxObserved + avg * 10) / 11.0f;
            scale = 255.0f / (Math.max(1, robustMax - blackLevel));
        } else {
            scale = 255.0f / (Math.max(1, (whitePoint - blackPoint) * whiteLevel));
        }

        // Simple Debayering for live preview (assume RGGB)
        // Downscale while debayering for speed
        float manualBlackOffset = useAutoStretch ? 0 : blackPoint * whiteLevel;

        for (int y = 0; y < sh; y++) {
            int origY = y * 2;
            for (int x = 0; x < sw; x++) {
                int origX = x * 2;

                // RGGB
                float r, g1, g2, b;
                if (stackBuffer != null) {
                    r = (stackBuffer[origY * width + origX] - blackLevel - manualBlackOffset) * redGain;
                    g1 = (stackBuffer[origY * width + (origX + 1)] - blackLevel - manualBlackOffset) * greenGain;
                    g2 = (stackBuffer[(origY + 1) * width + origX] - blackLevel - manualBlackOffset) * greenGain;
                    b = (stackBuffer[(origY + 1) * width + (origX + 1)] - blackLevel - manualBlackOffset) * blueGain;
                } else {
                    r = ((rawBuffer[origY * width + origX] & 0xFFFF) - blackLevel - manualBlackOffset) * redGain;
                    g1 = ((rawBuffer[origY * width + (origX + 1)] & 0xFFFF) - blackLevel - manualBlackOffset) * greenGain;
                    g2 = ((rawBuffer[(origY + 1) * width + origX] & 0xFFFF) - blackLevel - manualBlackOffset) * greenGain;
                    b = ((rawBuffer[(origY + 1) * width + (origX + 1)] & 0xFFFF) - blackLevel - manualBlackOffset) * blueGain;
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
                // Rotate 90 degrees for portrait orientation
                if (rotatedBitmap == null || rotatedBitmap.getWidth() != previewBitmap.getHeight() || rotatedBitmap.getHeight() != previewBitmap.getWidth()) {
                    rotatedBitmap = Bitmap.createBitmap(previewBitmap.getHeight(), previewBitmap.getWidth(), Bitmap.Config.ARGB_8888);
                }

                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(90);

                Canvas rotatedCanvas = new Canvas(rotatedBitmap);
                rotatedCanvas.drawBitmap(previewBitmap, matrix, paint);

                // Draw rotated bitmap to fill screen while maintaining aspect ratio
                int canvasW = canvas.getWidth();
                int canvasH = canvas.getHeight();
                int bitW = rotatedBitmap.getWidth();
                int bitH = rotatedBitmap.getHeight();

                float scaleX = (float) canvasW / bitW;
                float scaleY = (float) canvasH / bitH;
                float scale = Math.max(scaleX, scaleY); // Fill screen

                int drawW = (int) (bitW * scale);
                int drawH = (int) (bitH * scale);
                int left = (canvasW - drawW) / 2;
                int top = (canvasH - drawH) / 2;

                Rect dst = new Rect(left, top, left + drawW, top + drawH);
                canvas.drawBitmap(rotatedBitmap, null, dst, paint);
            }
            textureView.unlockCanvasAndPost(canvas);
        }
    }
}
