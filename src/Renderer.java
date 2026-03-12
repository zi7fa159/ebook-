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
    private final StretchParams currentParams = new StretchParams();

    public Renderer(TextureView textureView) {
        this.textureView = textureView;
    }

    public synchronized void setBlackLevel(int bl) { this.blackLevel = bl; }
    public synchronized void setWhiteLevel(float wl) { this.whiteLevel = wl; }
    public synchronized void setSensorOrientation(int orientation) { this.sensorOrientation = orientation; }
    public synchronized void setCfaPattern(int pattern) { this.cfaPattern = pattern; }

    public synchronized void setWbGains(float r, float g, float b) {
        this.redGain = r;
        this.greenGain = g;
        this.blueGain = b;
    }

    public synchronized void setStretch(float black, float mid, float white) {
        this.blackPoint = black;
        this.midPoint = Math.max(0.05f, Math.min(0.95f, mid));
        this.whitePoint = white;
        this.useAutoStretch = false;
        currentParams.useAuto = false;
    }

    public int[] getHistogram() {
        synchronized(histLock) {
            return histogram.clone();
        }
    }

    public synchronized void setAutoStretch(boolean auto) {
        this.useAutoStretch = auto;
        currentParams.useAuto = auto;
    }

    public StretchParams getCurrentParams() {
        StretchParams p = new StretchParams();
        synchronized(this) {
            p.scale = currentParams.scale;
            p.midFactor = currentParams.midFactor;
            p.blackOffset = currentParams.blackOffset;
            p.whiteClip = currentParams.whiteClip;
            p.useAuto = currentParams.useAuto;
        }
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
            currentParams.scale = scale;
            currentParams.midFactor = midFactor;
            currentParams.blackOffset = manualBlackOffset;
            currentParams.whiteClip = (whiteLevel - blackLevel) * 0.95f;
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

                r = (r - effectiveBlack - manualBlackOffset) * redGain;
                g = (g - effectiveBlack - manualBlackOffset) * greenGain;
                b = (b - effectiveBlack - manualBlackOffset) * blueGain;

                // Purple Highlight Fix: If value is near saturation, clamp to avoid tint
                float whiteClip = (whiteLevel - blackLevel) * 0.95f;
                if (r > whiteClip || g > whiteClip || b > whiteClip) {
                    float max = Math.max(r, Math.max(g, b));
                    if (max > whiteClip) {
                        r = g = b = max;
                    }
                }

                int ri, gi, bi;
                if (useAutoStretch) {
                    if (isFastLive) {
                        ri = (int) (r * scale);
                        gi = (int) (g * scale);
                        bi = (int) (b * scale);
                    } else {
                        ri = (int) (Math.sqrt(Math.max(0, r * scale) / 255.0) * 255.0);
                        gi = (int) (Math.sqrt(Math.max(0, g * scale) / 255.0) * 255.0);
                        bi = (int) (Math.sqrt(Math.max(0, b * scale) / 255.0) * 255.0);
                    }
                } else {
                    // Professional Midtones Stretch (Power Law)
                    float range = Math.max(1, (whitePoint - blackPoint) * effectiveWhite);
                    float normR = Math.max(0, Math.min(1.0f, r / range));
                    float normG = Math.max(0, Math.min(1.0f, g / range));
                    float normB = Math.max(0, Math.min(1.0f, b / range));

                    ri = (int) (Math.pow(normR, midFactor) * 255);
                    gi = (int) (Math.pow(normG, midFactor) * 255);
                    bi = (int) (Math.pow(normB, midFactor) * 255);
                }

                ri = Math.max(0, Math.min(255, ri));
                gi = Math.max(0, Math.min(255, gi));
                bi = Math.max(0, Math.min(255, bi));

                // Update histogram with average luminance
                int lum = (ri + gi + bi) / 3;
                synchronized(histLock) {
                    histogram[lum]++;
                }

                argbBuffer[y * sw + x] = 0xFF000000 | (ri << 16) | (gi << 8) | bi;
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
