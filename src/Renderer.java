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

        // Crude Debayering for live preview (assuming RGGB pattern)
        // R G
        // G B
        for (int y = 0; y < height; y += 2) {
            for (int x = 0; x < width; x += 2) {
                // RGGB positions
                float r = stackBuffer[y * width + x];
                float g1 = stackBuffer[y * width + (x + 1)];
                float g2 = stackBuffer[(y + 1) * width + x];
                float b = stackBuffer[(y + 1) * width + (x + 1)];

                float g = (g1 + g2) / 2.0f;

                int ri = Math.min(255, (int) ((r / maxVal) * 255));
                int gi = Math.min(255, (int) ((g / maxVal) * 255));
                int bi = Math.min(255, (int) ((b / maxVal) * 255));

                int color = 0xFF000000 | (ri << 16) | (gi << 8) | bi;

                // Set 2x2 block to the same color for speed in preview
                argbBuffer[y * width + x] = color;
                argbBuffer[y * width + (x + 1)] = color;
                argbBuffer[(y + 1) * width + x] = color;
                argbBuffer[(y + 1) * width + (x + 1)] = color;
            }
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
