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

    public void render(float[] stackBuffer, int width, int height) {
        for (int i = 0; i < stackBuffer.length; i++) {
            int val = (int) Math.min(255, stackBuffer[i]);
            pixels[i] = Color.rgb(val, val, val);
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
