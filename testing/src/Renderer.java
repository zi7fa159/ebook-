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
    private boolean isZoomed = false;
    private float sharpenAggression = 0.0f; // 0.0 to 1.0

    public Renderer(TextureView textureView) {
        this.textureView = textureView;
    }

    public android.content.Context getContext() {
        return textureView.getContext();
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

    public synchronized void setTempTint(float temp, float tint) {
        colorParams.temperature = temp;
        colorParams.tint = tint;
        float[] gains = ColorEngine.getGainsFromTempTint(temp, tint);
        setWbGains(gains[0], gains[1], gains[2]);
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
        p.temperature = colorParams.temperature;
        p.tint = colorParams.tint;
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

    public synchronized void setZoom(boolean zoom) {
        this.isZoomed = zoom;
    }

    public synchronized void setSharpen(float sharpen) {
        this.sharpenAggression = sharpen;
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
        if (width <= 0 || height <= 0) return;
        // Increased downscale for 50MP performance (4x = 16x area reduction)
        int step = (width > 6000) ? 4 : 2;
        int sw = (width / step) / 2 * 2; // Ensure even
        int sh = (height / step) / 2 * 2;
        if (sw <= 0 || sh <= 0) return;

        synchronized(this) {
            if (previewBitmap == null || previewBitmap.getWidth() != sw || previewBitmap.getHeight() != sh) {
                if (previewBitmap != null) previewBitmap.recycle();
                previewBitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
                argbBuffer = new int[sw * sh];
            }
        }

        // Effective black level for summed stacks
        float effectiveBlack = isSumStacking ? (blackLevel * currentStackFrames) : blackLevel;
        float effectiveWhite = isSumStacking ? (whiteLevel * currentStackFrames) : whiteLevel;

        // Sky background normalization (median background estimation)
        float backgroundMedian = 0;
        if (stackBuffer != null) {
            float sum = 0;
            int count = 0;
            int len = stackBuffer.length;
            for (int i = 0; i < len; i += 10000) {
                sum += stackBuffer[i];
                count++;
            }
            if (count > 0) backgroundMedian = (sum / count) - effectiveBlack;
        }

        // Brightness scaling for preview
        float scale;
        float midFactor;
        float manualBlackOffset;

        if (useAutoStretch) {
            float maxObserved = 0;
            int sampleCount = 0;
            float sum = 0;
            int len = (stackBuffer != null) ? stackBuffer.length : rawBuffer.length;
            for (int i = 0; i < len; i += 8000) {
                float val = (stackBuffer != null) ? stackBuffer[i] : (rawBuffer[i] & 0xFFFF);
                if (val > maxObserved) maxObserved = val;
                sum += val;
                sampleCount++;
            }
            float avg = sum / Math.max(1, sampleCount);
            float signalRange = Math.max(1, avg - effectiveBlack);
            scale = 40.0f / signalRange; // Slightly dimmer auto-stretch
            if (scale > 30.0f) scale = 30.0f;
            midFactor = 1.0f;
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
            colorParams.blackLevel = blackLevel; // Original black level
            colorParams.whiteLevel = whiteLevel;
            colorParams.useAutoStretch = useAutoStretch;
        }

        if (stackBuffer == null && rawBuffer != null) {
            int mid = rawBuffer.length / 2;
            debugInfo = "RawSample: " + (rawBuffer[mid] & 0xFFFF) + ", Scale: " + String.format("%.2f", scale);
        }
        frameCount++;

        synchronized(histLock) {
            for (int i = 0; i < 256; i++) histogram[i] = 0;
        }

        int startX = 0;
        int startY = 0;
        int endX = width;
        int endY = height;
        int currentStep = step;

        if (isZoomed) {
            // 400x400 center crop at full resolution
            int zoomSize = 800; // Increased a bit to fill preview better
            startX = (width - zoomSize) / 2;
            startY = (height - zoomSize) / 2;
            endX = startX + zoomSize;
            endY = startY + zoomSize;
            currentStep = 1; // Full resolution for zoom

            // Re-calc sw/sh for zoom
            sw = zoomSize;
            sh = zoomSize;

            synchronized(this) {
                if (previewBitmap == null || previewBitmap.getWidth() != sw || previewBitmap.getHeight() != sh) {
                    if (previewBitmap != null) previewBitmap.recycle();
                    previewBitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
                    argbBuffer = new int[sw * sh];
                }
            }
        }

        for (int y = 0; y < sh; y += 2) {
            int oy = startY + y * currentStep;
            if (oy >= endY - 1 || oy >= height - 1) continue;
            for (int x = 0; x < sw; x += 2) {
                int ox = startX + x * currentStep;
                if (ox >= endX - 1 || ox >= width - 1) continue;

                int idx = oy * width + ox;
                float v00 = getPixel(stackBuffer, rawBuffer, idx, width, height);
                float v01 = getPixel(stackBuffer, rawBuffer, idx + 1, width, height);
                float v10 = getPixel(stackBuffer, rawBuffer, idx + width, width, height);
                float v11 = getPixel(stackBuffer, rawBuffer, idx + width + 1, width, height);

                float r, g, b;
                switch(cfaPattern) {
                    case 1: r=v01; g=(v00+v11)/2f; b=v10; break;
                    case 2: r=v10; g=(v00+v11)/2f; b=v01; break;
                    case 3: r=v11; g=(v01+v10)/2f; b=v00; break;
                    default: r=v00; g=(v01+v10)/2f; b=v11; break;
                }

                int argb = ColorEngine.processPixel(r, g, g, b, colorParams, effectiveBlack);

                // Set 2x2 block
                argbBuffer[y * sw + x] = argb;
                argbBuffer[y * sw + (x + 1)] = argb;
                argbBuffer[(y + 1) * sw + x] = argb;
                argbBuffer[(y + 1) * sw + (x + 1)] = argb;

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

    private float getPixel(float[] stack, short[] raw, int idx, int w, int h) {
        float val = (stack != null) ? stack[idx] : (raw[idx] & 0xFFFF);
        if (sharpenAggression <= 0) return val;

        // Simple Laplacian-lite sharpening for Bayer domain
        // We look at neighbors 2 pixels away to match color phase
        if (idx < 2 * w || idx >= (h - 2) * w || idx % w < 2 || idx % w >= w - 2) return val;

        float n1 = (stack != null) ? stack[idx - 2] : (raw[idx - 2] & 0xFFFF);
        float n2 = (stack != null) ? stack[idx + 2] : (raw[idx + 2] & 0xFFFF);
        float n3 = (stack != null) ? stack[idx - 2 * w] : (raw[idx - 2 * w] & 0xFFFF);
        float n4 = (stack != null) ? stack[idx + 2 * w] : (raw[idx + 2 * w] & 0xFFFF);

        float laplacian = 4 * val - (n1 + n2 + n3 + n4);
        return val + laplacian * sharpenAggression * 0.5f;
    }

    private void draw() {
        if (!textureView.isAvailable()) return;
        Canvas canvas = textureView.lockCanvas();
        if (canvas != null) {
            try {
                canvas.drawColor(0xFF000000);

                synchronized(this) {
                    if (previewBitmap != null && !previewBitmap.isRecycled()) {
                        int bw = previewBitmap.getWidth();
                        int bh = previewBitmap.getHeight();

                        android.graphics.Matrix matrix = new android.graphics.Matrix();
                        matrix.postRotate(sensorOrientation, bw / 2.0f, bh / 2.0f);

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

                        matrix.postTranslate(-minX, -minY);

                        float scaleX = (float) canvas.getWidth() / rotatedW;
                        float scaleY = (float) canvas.getHeight() / rotatedH;
                        float scale = Math.min(scaleX, scaleY);
                        matrix.postScale(scale, scale);

                        float finalW = rotatedW * scale;
                        float finalH = rotatedH * scale;
                        matrix.postTranslate((canvas.getWidth() - finalW) / 2.0f, (canvas.getHeight() - finalH) / 2.0f);

                        canvas.drawBitmap(previewBitmap, matrix, paint);
                    }
                }

                paint.setColor(0xFFFFFFFF);
                paint.setTextSize(30);
                canvas.drawText(debugInfo, 50, 40, paint);
            } finally {
                textureView.unlockCanvasAndPost(canvas);
            }
        }
    }
}
