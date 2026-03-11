package com.alsclone.astrostacker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.Surface;
import android.view.TextureView;

public class Renderer {
    private final TextureView textureView;
    private final Paint paint = new Paint();
    private Bitmap previewBitmap;
    private int[] argbBuffer;

    public Renderer(TextureView textureView) {
        this.textureView = textureView;
    }

    public synchronized void updateStack(float[] stackBuffer, int width, int height) {
        if (previewBitmap == null || previewBitmap.getWidth() != width || previewBitmap.getHeight() != height) {
            previewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            argbBuffer = new int[width * height];
        }

        // Simple tone mapping / scaling for display
        // Find max value in buffer for normalization
        float maxVal = 1.0f;
        for (int i = 0; i < stackBuffer.length; i += 100) { // Sample to find max
            if (stackBuffer[i] > maxVal) maxVal = stackBuffer[i];
        }

        for (int i = 0; i < stackBuffer.length; i++) {
            int val = (int) ((stackBuffer[i] / maxVal) * 255);
            if (val > 255) val = 255;
            argbBuffer[i] = 0xFF000000 | (val << 16) | (val << 8) | val;
        }

        previewBitmap.setPixels(argbBuffer, 0, width, 0, 0, width, height);
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
