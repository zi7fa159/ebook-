package com.alsclone.astrostacker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.TextureView;

public class Renderer {
    private static final String TAG = "A2LS_Renderer";
    private final TextureView textureView;
    private final Paint paint = new Paint();
    private int sensorOrientation = 90;
    private int cfaPattern = 0; // 0=RGGB, 1=GRBG, 2=GBRG, 3=BGGR
    private Bitmap previewBitmap;
    private Bitmap rotatedBitmap;
    private int[] argbBuffer;

    private float blackPoint = 0.0f;
    private float midPoint = 0.5f;
    private float whitePoint = 1.0f;
    private boolean useAutoStretch = true;

    // Default color gains for Realme 8i sensor (approximate)
    private float redGain = 1.6f;
    private float greenGain = 1.0f;
    private float blueGain = 2.1f;
    private int blackLevel = 64;
    private float whiteLevel = 1023.0f;
    private int frameCount = 0;
    private int currentStackFrames = 1;
    private boolean isSumStacking = false;
    private final int[] histogram = new int[256];
    private final Object histLock = new Object();
    private final ColorEngine.Params colorParams = new ColorEngine.Params();

    public Renderer(TextureView textureView) {
        this.textureView = textureView;
    }

    public synchronized void setBlackLevel(int bl) { this.blackLevel = bl; }
    public synchronized void setWhiteLevel(float wl) { this.whiteLevel = wl; }
    public synchronized void setSensorOrientation(int orientation) { this.sensorOrientation = orientation; }
    public synchronized void setCfaPattern(int pattern) { this.cfaPattern = pattern; }

    public synchronized void setWbGains(float r, float g, float b) {
        colorParams.rGain = r;
        colorParams.gGain = g;
        colorParams.bGain = b;
        this.redGain = r;
        this.greenGain = g;
        this.blueGain = b;
    }

    public synchronized void setStretch(float black, float mid, float white) {
        this.blackPoint = black;
        this.midPoint = Math.max(0.05f, Math.min(0.95f, mid));
        this.whitePoint = white;
        this.useAutoStretch = false;
        colorParams.useAutoStretch = false;
        colorParams.gamma = (float)(Math.log(0.5)/Math.log(midPoint));
    }

    public int[] getHistogram() {
        synchronized(histLock) {
            return histogram.clone();
        }
    }

    public synchronized void setAutoStretch(boolean auto) {
        this.useAutoStretch = auto;
        colorParams.useAutoStretch = auto;
    }

    public synchronized ColorEngine.Params getColorParams() {
        ColorEngine.Params p = new ColorEngine.Params();
        p.rGain = colorParams.rGain;
        p.gGain = colorParams.gGain;
        p.bGain = colorParams.bGain;
        p.blackLevel = colorParams.blackLevel;
        p.whiteLevel = colorParams.whiteLevel;
        p.stretchScale = colorParams.stretchScale;
        p.gamma = colorParams.gamma;
        p.manualBlackOffset = colorParams.manualBlackOffset;
        p.useAutoStretch = colorParams.useAutoStretch;
        return p;
    }

    private String debugInfo = "";

    public void setDebugInfo(String info) {
        this.debugInfo = info;
    }

    public synchronized void updateLive(short[] rawBuffer, int width, int height) {
        this.currentStackFrames = 1;
        this.isSumStacking = false;
        processAndDraw(null, rawBuffer, width, height);
    }

    public synchronized void updateStack(float[] stackBuffer, int width, int height, int n, boolean isSum) {
        this.currentStackFrames = n;
        this.isSumStacking = isSum;
        processAndDraw(stackBuffer, null, width, height);
    }

    private void processAndDraw(float[] stackBuffer, short[] rawBuffer, int width, int height) {
        // Increased downscale for 50MP performance (4x = 16x area reduction)
        int step = (width > 6000) ? 4 : 2;
        int sw = width / step;
        int sh = height / step;

        if (previewBitmap == null || previewBitmap.getWidth() != sw || previewBitmap.getHeight() != sh) {
            if (previewBitmap != null) previewBitmap.recycle();
            previewBitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
            argbBuffer = new int[sw * sh];
        }

        // Effective black level for summed stacks
        float effectiveBlack = isSumStacking ? (blackLevel * currentStackFrames) : blackLevel;
        float effectiveWhite = isSumStacking ? (whiteLevel * currentStackFrames) : whiteLevel;

        // Brightness scaling for preview
        float scale;
        float midFactor;
        float manualBlackOffset;

        if (useAutoStretch) {
            float maxObserved = 0;
            int sampleCount = 0;
            float sum = 0;
            int len = (stackBuffer != null) ? stackBuffer.length : rawBuffer.length;
            for (int i = 0; i < len; i += 4000) {
                float val = (stackBuffer != null) ? stackBuffer[i] : (rawBuffer[i] & 0xFFFF);
                if (val > maxObserved) maxObserved = val;
                sum += val;
                sampleCount++;
            }
            float avg = sum / sampleCount;
            float signalRange = Math.max(1, avg - effectiveBlack);
            scale = 50.0f / signalRange; // Map background to 50/255
            if (scale > 20.0f) scale = 20.0f;
            midFactor = 1.0f; // Neutral
            manualBlackOffset = 0;
        } else {
            manualBlackOffset = blackPoint * effectiveWhite;
            scale = 255.0f / Math.max(1.0f, (whitePoint * effectiveWhite) - manualBlackOffset);
            midFactor = (float) (Math.log(0.5) / Math.log(midPoint));
        }

        // Sync params for export
        synchronized(this) {
            colorParams.stretchScale = scale;
            colorParams.gamma = midFactor;
            colorParams.manualBlackOffset = manualBlackOffset;
            colorParams.blackLevel = (int)effectiveBlack;
            colorParams.whiteLevel = whiteLevel;
        }

        if (stackBuffer == null && rawBuffer != null) {
            // Sample a few pixels to see if they are non-zero
            int mid = rawBuffer.length / 2;
            debugInfo = "RawSample: " + (rawBuffer[mid] & 0xFFFF) + ", Scale: " + String.format("%.2f", scale);
        }
        frameCount++;

        // Simple Debayering for live preview (pattern aware)
        boolean isFastLive = (stackBuffer == null);

        synchronized(histLock) {
            for (int i = 0; i < 256; i++) histogram[i] = 0;
        }

        for (int y = 0; y < sh; y++) {
            int oy = y * step;
            for (int x = 0; x < sw; x++) {
                int ox = x * step;

                float v00, v01, v10, v11;
                if (stackBuffer != null) {
                    v00 = stackBuffer[oy * width + ox];
                    v01 = stackBuffer[oy * width + (ox + 1)];
                    v10 = stackBuffer[(oy + 1) * width + ox];
                    v11 = stackBuffer[(oy + 1) * width + (ox + 1)];
                } else {
                    v00 = (rawBuffer[oy * width + ox] & 0xFFFF);
                    v01 = (rawBuffer[oy * width + (ox + 1)] & 0xFFFF);
                    v10 = (rawBuffer[(oy + 1) * width + ox] & 0xFFFF);
                    v11 = (rawBuffer[(oy + 1) * width + (ox + 1)] & 0xFFFF);
                }

                float r, b;
                if (cfaPattern == 0) { r = v00; b = v11; }
                else if (cfaPattern == 1) { r = v01; b = v10; }
                else if (cfaPattern == 2) { r = v10; b = v01; }
                else { r = v11; b = v00; }

                // Optimized color processing using ColorEngine
                float whiteClip = (whiteLevel - blackLevel) * 0.95f;
                int argb = ColorEngine.processPixel(r, v01, v10, b, colorParams, effectiveBlack, whiteClip);

                // Extract 8-bit for histogram
                int ri = (argb >> 16) & 0xFF;
                int gi = (argb >> 8) & 0xFF;
                int bi = argb & 0xFF;

                // Update histogram
                synchronized(histLock) {
                    histogram[(ri + gi + bi) / 3]++;
                }

                argbBuffer[y * sw + x] = argb;
            }
        }

        previewBitmap.setPixels(argbBuffer, 0, sw, 0, 0, sw, sh);
        draw();
    }

    private void draw() {
        if (!textureView.isAvailable()) return;
        Canvas canvas = textureView.lockCanvas();
        if (canvas != null) {
            canvas.drawColor(0xFF222222); // Dark gray to distinguish from pure black background

            // Draw a debug indicator (green dot) to show rendering is alive
            paint.setColor(0xFF00FF00);
            canvas.drawCircle(30, 30, 15, paint);

            if (previewBitmap != null) {
                int bw = previewBitmap.getWidth();
                int bh = previewBitmap.getHeight();

                android.graphics.Matrix matrix = new android.graphics.Matrix();

                // 1. Rotate around center of source bitmap
                matrix.postRotate(sensorOrientation, bw / 2.0f, bh / 2.0f);

                // 2. Translate so it's centered at (0,0) after rotation?
                // Actually, let's just use a simpler approach.

                // Better approach: rotate then find bounds, then scale to fit canvas.
                float[] pts = {0, 0, bw, 0, bw, bh, 0, bh};
                matrix.mapPoints(pts);
                float minX = pts[0], minY = pts[1], maxX = pts[0], maxY = pts[1];
                for (int i = 2; i < 8; i += 2) {
                    minX = Math.min(minX, pts[i]);
                    minY = Math.min(minY, pts[i+1]);
                    maxX = Math.max(maxX, pts[i]);
                    maxY = Math.max(maxY, pts[i+1]);
                }
                float rotatedW = maxX - minX;
                float rotatedH = maxY - minY;

                // Translate to bring minX, minY to 0,0
                matrix.postTranslate(-minX, -minY);

                // Scale to fill canvas
                float scaleX = (float) canvas.getWidth() / rotatedW;
                float scaleY = (float) canvas.getHeight() / rotatedH;
                float scale = Math.max(scaleX, scaleY);
                matrix.postScale(scale, scale);

                // Center in canvas
                float finalW = rotatedW * scale;
                float finalH = rotatedH * scale;
                matrix.postTranslate((canvas.getWidth() - finalW) / 2.0f, (canvas.getHeight() - finalH) / 2.0f);

                canvas.drawBitmap(previewBitmap, matrix, paint);
            }

            // Draw debug text
            paint.setColor(0xFFFFFFFF);
            paint.setTextSize(30);
            canvas.drawText(debugInfo, 50, 40, paint);

            textureView.unlockCanvasAndPost(canvas);
        }
    }
}
