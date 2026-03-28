package com.astroapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;

public class PreviewProcessor {
    private static final String TAG = "PreviewProcessor";

    /**
     * Extracts a Bitmap from a JPEG image for display.
     * User requested no stretching, just show the raws (previews).
     */
    public static Bitmap processJpeg(Image image) {
        if (image == null) return null;

        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode JPEG", e);
            return null;
        } finally {
            image.close();
        }
    }

    /**
     * Stretches a bitmap if needed (legacy, not used now as requested)
     */
    public static Bitmap stretchBitmap(Bitmap src, float gamma, float contrast) {
        if (src == null) return null;
        // Logic removed or simplified
        return src;
    }
}
