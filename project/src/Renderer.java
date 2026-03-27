package com.example.astrostacker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;

public class Renderer {
    private SurfaceHolder holder;
    private Paint paint = new Paint();
    private int[] pixelBuffer;
    private Bitmap bitmap;

    public Renderer(SurfaceHolder holder) {
        this.holder = holder;
    }

    public void render(StackEngine stackEngine) {
        float[] buffer = stackEngine.getStackBuffer();
        int width = stackEngine.getWidth();
        int height = stackEngine.getHeight();

        if (pixelBuffer == null || pixelBuffer.length != buffer.length) {
            pixelBuffer = new int[buffer.length];
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }

        for (int i = 0; i < buffer.length; i++) {
            int val = (int) buffer[i];
            if (val > 255) val = 255;
            pixelBuffer[i] = 0xFF000000 | (val << 16) | (val << 8) | val;
        }

        bitmap.setPixels(pixelBuffer, 0, width, 0, 0, width, height);

        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            Rect dest = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
            canvas.drawBitmap(bitmap, null, dest, paint);
            holder.unlockCanvasAndPost(canvas);
        }
    }
}
