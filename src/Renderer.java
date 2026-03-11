package com.alsclone.astrostacker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.TextureView;

public class Renderer {
    private final TextureView textureView;
    private final Paint paint = new Paint();
    private int sensorOrientation = 90;
    private int cfaPattern = 0; // 0=RGGB, 1=GRBG, 2=GBRG, 3=BGGR
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
    private int blackLevel = 64;
    private float whiteLevel = 1023.0f;

    public Renderer(TextureView textureView) {
        this.textureView = textureView;
    }

    public void setBlackLevel(int bl) { this.blackLevel = bl; }
    public void setWhiteLevel(float wl) { this.whiteLevel = wl; }
    public void setSensorOrientation(int orientation) { this.sensorOrientation = orientation; }
    public void setCfaPattern(int pattern) { this.cfaPattern = pattern; }

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
        // Increased downscale for 50MP performance (4x = 16x area reduction)
        int step = (width > 6000) ? 4 : 2;
        int sw = width / step;
        int sh = height / step;

        if (previewBitmap == null || previewBitmap.getWidth() != sw || previewBitmap.getHeight() != sh) {
            previewBitmap = Bitmap.createBitmap(sw, sw, Bitmap.Config.ARGB_8888); // Square allocation for rotation safety
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

        // Simple Debayering for live preview (pattern aware)
        float manualBlackOffset = useAutoStretch ? 0 : blackPoint * whiteLevel;
        boolean isFastLive = (stackBuffer == null);

        for (int y = 0; y < sh; y++) {
            int oy = y * step;
            for (int x = 0; x < sw; x++) {
                int ox = x * step;

                float r, g, b;
                if (stackBuffer != null) {
                    // Pattern: 0=RGGB, 1=GRBG, 2=GBRG, 3=BGGR
                    float v00 = stackBuffer[oy * width + ox];
                    float v01 = stackBuffer[oy * width + (ox + 1)];
                    float v10 = stackBuffer[(oy + 1) * width + ox];
                    float v11 = stackBuffer[(oy + 1) * width + (ox + 1)];

                    if (cfaPattern == 0) { r = v00; g = (v01+v10)/2f; b = v11; }
                    else if (cfaPattern == 1) { r = v01; g = (v00+v11)/2f; b = v10; }
                    else if (cfaPattern == 2) { r = v10; g = (v00+v11)/2f; b = v01; }
                    else { r = v11; g = (v01+v10)/2f; b = v00; }
                } else {
                    float v00 = (rawBuffer[oy * width + ox] & 0xFFFF);
                    float v01 = (rawBuffer[oy * width + (ox + 1)] & 0xFFFF);
                    float v10 = (rawBuffer[(oy + 1) * width + ox] & 0xFFFF);
                    float v11 = (rawBuffer[(oy + 1) * width + (ox + 1)] & 0xFFFF);

                    if (cfaPattern == 0) { r = v00; g = (v01+v10)/2f; b = v11; }
                    else if (cfaPattern == 1) { r = v01; g = (v00+v11)/2f; b = v10; }
                    else if (cfaPattern == 2) { r = v10; g = (v00+v11)/2f; b = v01; }
                    else { r = v11; g = (v01+v10)/2f; b = v00; }
                }

                r = (r - blackLevel - manualBlackOffset) * redGain;
                g = (g - blackLevel - manualBlackOffset) * greenGain;
                b = (b - blackLevel - manualBlackOffset) * blueGain;

                int ri, gi, bi;
                if (isFastLive) {
                    // Faster linear mapping for live view
                    ri = (int) (r * scale);
                    gi = (int) (g * scale);
                    bi = (int) (b * scale);
                } else {
                    // ALS style sqrt stretch for stacked result only
                    ri = (int) (Math.sqrt(Math.max(0, r * scale) / 255.0) * 255.0);
                    gi = (int) (Math.sqrt(Math.max(0, g * scale) / 255.0) * 255.0);
                    bi = (int) (Math.sqrt(Math.max(0, b * scale) / 255.0) * 255.0);
                }

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
                // Handle dynamic rotation
                int targetWidth = (sensorOrientation % 180 == 0) ? previewBitmap.getWidth() : previewBitmap.getHeight();
                int targetHeight = (sensorOrientation % 180 == 0) ? previewBitmap.getHeight() : previewBitmap.getWidth();

                if (rotatedBitmap == null || rotatedBitmap.getWidth() != targetWidth || rotatedBitmap.getHeight() != targetHeight) {
                    rotatedBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
                }

                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(sensorOrientation);

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
