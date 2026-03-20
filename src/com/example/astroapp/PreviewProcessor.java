package com.example.astroapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import java.nio.ByteBuffer;

public class PreviewProcessor {
    private ImageView imageView;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Bitmap reusableBitmap;
    private Canvas reusableCanvas;
    private Paint stretchPaint;

    public PreviewProcessor(ImageView imageView) {
        this.imageView = imageView;
        stretchPaint = new Paint();
        ColorMatrix cm = new ColorMatrix(new float[] {
                1.5f, 0, 0, 0, 10,
                0, 1.5f, 0, 0, 10,
                0, 0, 1.5f, 0, 10,
                0, 0, 0, 1, 0
        });
        stretchPaint.setColorFilter(new ColorMatrixColorFilter(cm));
    }

    public void processJpeg(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        if (bitmap != null) {
            Bitmap stretched = applyFastStretch(bitmap);
            mainHandler.post(() -> {
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageBitmap(stretched);
            });
        }
    }

    private synchronized Bitmap applyFastStretch(Bitmap src) {
        if (reusableBitmap == null || reusableBitmap.getWidth() != src.getWidth() || reusableBitmap.getHeight() != src.getHeight()) {
            reusableBitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
            reusableCanvas = new Canvas(reusableBitmap);
        }

        reusableCanvas.drawBitmap(src, 0, 0, stretchPaint);
        src.recycle(); // Free the decoded mutable bitmap
        return reusableBitmap;
    }
}
