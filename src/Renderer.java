package com.alsclone.astrostacker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.TextureView;

public class Renderer {
    private final TextureView textureView;
    private final Paint paint = new Paint();
    private Bitmap previewBitmap;
    private int[] argbBuffer;

    // Default color gains for Realme 8i sensor (approximate)
    private float redGain = 2.0f;
    private float blueGain = 1.8f;

    public Renderer(TextureView textureView) {
        this.textureView = textureView;
    }

    public void setWbGains(float r, float b) {
        this.redGain = r;
        this.blueGain = b;
    }

    public synchronized void updateStack(float[] stackBuffer, int width, int height) {
        // Downscale for preview performance (always 4x downscale)
        int sw = width / 2;
        int sh = height / 2;

        if (previewBitmap == null || previewBitmap.getWidth() != sw || previewBitmap.getHeight() != sh) {
            previewBitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
            argbBuffer = new int[sw * sh];
        }

        // Auto-brightness scaling for preview
        float maxVal = 1.0f;
        for (int i = 0; i < stackBuffer.length; i += 1000) {
            if (stackBuffer[i] > maxVal) maxVal = stackBuffer[i];
        }

        // Simple Debayering for live preview (assume RGGB)
        // Downscale while debayering for speed
        for (int y = 0; y < sh; y++) {
            int origY = y * 2;
            for (int x = 0; x < sw; x++) {
                int origX = x * 2;

                // RGGB
                float r = stackBuffer[origY * width + origX] * redGain;
                float g1 = stackBuffer[origY * width + (origX + 1)];
                float g2 = stackBuffer[(origY + 1) * width + origX];
                float b = stackBuffer[(origY + 1) * width + (origX + 1)] * blueGain;

                float g = (g1 + g2) / 2.0f;

                int ri = Math.min(255, (int) ((r / maxVal) * 255));
                int gi = Math.min(255, (int) ((g / maxVal) * 255));
                int bi = Math.min(255, (int) ((b / maxVal) * 255));

                argbBuffer[y * sw + x] = 0xFF000000 | (ri << 16) | (gi << 8) | bi;
            }
        }

        previewBitmap.setPixels(argbBuffer, 0, sw, 0, 0, sw, sh);
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
