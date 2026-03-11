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
    private float[] sample = new float[10000];

    public Renderer(SurfaceHolder holder, int width, int height) {
        this.holder = holder;
        this.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.pixels = new int[width * height];
    }

    public void render(float[] stackY, float[] stackU, float[] stackV, int width, int height, boolean autoStretch, int frameCount, StackEngine.Mode mode) {
        float blackPoint = 0;
        float whitePoint = mode == StackEngine.Mode.AVERAGE ? 255 : 255 * frameCount;

        if (autoStretch && stackY.length > 0) {
            int step = Math.max(1, stackY.length / sample.length);
            for (int i = 0; i < sample.length; i++) {
                sample[i] = stackY[Math.min(i * step, stackY.length - 1)];
            }
            Arrays.sort(sample);
            blackPoint = sample[(int)(sample.length * 0.05)];
            whitePoint = sample[(int)(sample.length * 0.95)];
            if (whitePoint <= blackPoint) whitePoint = blackPoint + 1.0f;
        }

        float invRange = 255.0f / (whitePoint - blackPoint);

        int uvWidth = width / 2;
        for (int y = 0; y < height; y++) {
            int yRowOffset = y * width;
            int uvRowOffset = (y / 2) * uvWidth;
            for (int x = 0; x < width; x++) {
                int idx = yRowOffset + x;
                int uvIdx = uvRowOffset + (x / 2);

                float Y = (stackY[idx] - blackPoint) * invRange;
                float U = stackU[uvIdx] - 128f;
                float V = stackV[uvIdx] - 128f;

                // YUV to RGB
                int r = (int)(Y + 1.370705f * V);
                int g = (int)(Y - 0.337633f * U - 0.698001f * V);
                int b = (int)(Y + 1.732446f * U);

                if (r < 0) r = 0; else if (r > 255) r = 255;
                if (g < 0) g = 0; else if (g > 255) g = 255;
                if (b < 0) b = 0; else if (b > 255) b = 255;

                pixels[idx] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            try {
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
