package com.example.astrostacker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.SurfaceHolder;

public class Renderer {
    private final SurfaceHolder holder;
    private Bitmap bitmap;
    private int[] pixels;

    public Renderer(SurfaceHolder holder, int width, int height) {
        this.holder = holder;
        this.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.pixels = new int[width * height];
    }

    public void render(float[] stackBuffer, int width, int height, boolean autoStretch) {
        float min = 255;
        float max = 0;

        if (autoStretch) {
            for (float v : stackBuffer) {
                if (v < min) min = v;
                if (v > max) max = v;
            }
        } else {
            min = 0;
            max = 255;
        }

        float range = max - min;
        if (range < 1.0f) range = 1.0f;

        for (int i = 0; i < stackBuffer.length; i++) {
            float val = (stackBuffer[i] - min) * 255.0f / range;
            int v = (int) Math.max(0, Math.min(255, val));
            pixels[i] = Color.rgb(v, v, v);
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            Rect dest = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
            canvas.drawBitmap(bitmap, null, dest, null);
            holder.unlockCanvasAndPost(canvas);
        }
    }
}
