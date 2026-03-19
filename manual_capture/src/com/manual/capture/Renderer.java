package com.manual.capture;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.TextureView;

public class Renderer {
    private static final String TAG = "ManualRaw_Renderer";
    private final TextureView textureView;
    private final Paint paint = new Paint();
    private int sensorOrientation = 90;
    private int cfaPattern = 0; // 0=RGGB
    private Bitmap previewBitmap;
    private int[] argbBuffer;

    private ColorEngine colorEngine;
    private int blackLevel = 64;
    private float whiteLevel = 1023.0f;
    private ColorEngine.Params colorParams = new ColorEngine.Params();
    private String debugInfo = "";

    public Renderer(TextureView textureView) {
        this.textureView = textureView;
    }

    public synchronized void setColorEngine(ColorEngine ce) { this.colorEngine = ce; }
    public synchronized void setBlackLevel(int bl) { this.blackLevel = bl; }
    public synchronized void setWhiteLevel(float wl) { this.whiteLevel = wl; }
    public synchronized void setSensorOrientation(int orientation) { this.sensorOrientation = orientation; }

    public synchronized void updateLive(short[] rawBuffer, int width, int height) {
        if (width <= 0 || height <= 0) return;

        int step = (width > 6000) ? 12 : 6;
        int sw = (width / step) / 2 * 2;
        int sh = (height / step) / 2 * 2;

        synchronized(this) {
            if (previewBitmap == null || previewBitmap.getWidth() != sw || previewBitmap.getHeight() != sh) {
                if (previewBitmap != null) previewBitmap.recycle();
                previewBitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
                argbBuffer = new int[sw * sh];
            }
        }

        // Auto stretch for live view
        float maxObserved = 0;
        float sum = 0;
        int sampleCount = 0;
        for (int i = 0; i < rawBuffer.length; i += 20000) {
            float val = rawBuffer[i] & 0xFFFF;
            if (val > maxObserved) maxObserved = val;
            sum += val;
            sampleCount++;
        }
        float avg = sum / Math.max(1, sampleCount);
        float signalRange = Math.max(1, avg - blackLevel);
        colorParams.stretchScale = Math.min(30.0f, 40.0f / signalRange);
        colorParams.gamma = 1.0f;
        colorParams.manualBlackOffset = 0;
        colorParams.blackLevel = blackLevel;
        colorParams.whiteLevel = whiteLevel;

        for (int y = 0; y < sh; y += 2) {
            int oy = y * step;
            if (oy >= height - 1) continue;
            for (int x = 0; x < sw; x += 2) {
                int ox = x * step;
                if (ox >= width - 1) continue;

                int idx = oy * width + ox;
                float v00 = rawBuffer[idx] & 0xFFFF;
                float v01 = rawBuffer[idx + 1] & 0xFFFF;
                float v10 = rawBuffer[idx + width] & 0xFFFF;
                float v11 = rawBuffer[idx + width + 1] & 0xFFFF;

                float r, g, b;
                switch(cfaPattern) {
                    case 1: r=v01; g=(v00+v11)/2f; b=v10; break;
                    case 2: r=v10; g=(v00+v11)/2f; b=v01; break;
                    case 3: r=v11; g=(v01+v10)/2f; b=v00; break;
                    default: r=v00; g=(v01+v10)/2f; b=v11; break;
                }

                int argb = ColorEngine.processPixel(r, g, g, b, colorParams, blackLevel);

                argbBuffer[y * sw + x] = argb;
                argbBuffer[y * sw + (x + 1)] = argb;
                argbBuffer[(y + 1) * sw + x] = argb;
                argbBuffer[(y + 1) * sw + (x + 1)] = argb;
            }
        }

        previewBitmap.setPixels(argbBuffer, 0, sw, 0, 0, sw, sh);
        draw();
    }

    private void draw() {
        if (!textureView.isAvailable()) return;
        Canvas canvas = textureView.lockCanvas();
        if (canvas != null) {
            try {
                canvas.drawColor(0xFF000000);
                synchronized(this) {
                    if (previewBitmap != null) {
                        android.graphics.Matrix matrix = new android.graphics.Matrix();
                        matrix.postRotate(sensorOrientation, previewBitmap.getWidth() / 2.0f, previewBitmap.getHeight() / 2.0f);

                        float scale = Math.min((float)canvas.getWidth() / previewBitmap.getHeight(), (float)canvas.getHeight() / previewBitmap.getWidth());
                        matrix.postScale(scale, scale);
                        matrix.postTranslate((canvas.getWidth() - previewBitmap.getHeight()*scale)/2f, (canvas.getHeight() - previewBitmap.getWidth()*scale)/2f);
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
