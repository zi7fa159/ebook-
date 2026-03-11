package com.example.astrostacker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import java.util.Arrays;

public class Renderer {
    private final SurfaceHolder holder;
    private Bitmap bitmap;
    private int[] pixels;
    private int[] lut = new int[256];

    public Renderer(SurfaceHolder holder, int width, int height) {
        this.holder = holder;
        this.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.pixels = new int[width * height];
    }

    /**
     * Renders the stack buffer to the surface with auto-stretch.
     */
    public void render(float[] stackBuffer, int width, int height, boolean autoStretch) {
        float blackPoint = 0;
        float whitePoint = 255;

        if (autoStretch && stackBuffer.length > 0) {
            // Percentile-based stretch (simplified for performance)
            // Use a small sample to find approximate percentiles
            float[] sample = new float[Math.min(stackBuffer.length, 10000)];
            int step = Math.max(1, stackBuffer.length / sample.length);
            for (int i = 0; i < sample.length; i++) {
                sample[i] = stackBuffer[Math.min(i * step, stackBuffer.length - 1)];
            }
            Arrays.sort(sample);

            blackPoint = sample[(int)(sample.length * 0.05)]; // 5th percentile
            whitePoint = sample[(int)(sample.length * 0.95)]; // 95th percentile

            if (whitePoint <= blackPoint) {
                whitePoint = blackPoint + 1.0f;
            }
        }

        float range = whitePoint - blackPoint;
        if (range <= 0) range = 1.0f;
        float invRange = 255.0f / range;

        for (int i = 0; i < stackBuffer.length; i++) {
            float val = (stackBuffer[i] - blackPoint) * invRange;
            int v = (int) val;
            if (v < 0) v = 0;
            else if (v > 255) v = 255;

            // Packed ARGB for faster bitmap update
            pixels[i] = 0xFF000000 | (v << 16) | (v << 8) | v;
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            try {
                // Keep aspect ratio
                int canvasW = canvas.getWidth();
                int canvasH = canvas.getHeight();
                float scale = Math.min((float)canvasW / width, (float)canvasH / height);
                int drawW = (int)(width * scale);
                int drawH = (int)(height * scale);
                int left = (canvasW - drawW) / 2;
                int top = (canvasH - drawH) / 2;

                Rect dest = new Rect(left, top, left + drawW, top + drawH);
                canvas.drawColor(Color.BLACK);
                canvas.drawBitmap(bitmap, null, dest, null);
            } finally {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }
}
