package com.example.astrostacker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StarDetector {
    public static class Star implements Comparable<Star> {
        public float x, y, brightness;
        public Star(float x, float y, float brightness) {
            this.x = x;
            this.y = y;
            this.brightness = brightness;
        }
        @Override
        public int compareTo(Star o) {
            return Float.compare(o.brightness, this.brightness);
        }
    }

    public List<Star> detectStars(byte[] yData, int width, int height, int rowStride) {
        // Simple 3x3 box blur and local maxima detection
        // Downsample by 2 for detection
        int ds = 2;
        int sw = width / ds;
        int sh = height / ds;
        float[] blurred = new float[sw * sh];

        // Downsample and Blur
        for (int y = 1; y < sh - 1; y++) {
            for (int x = 1; x < sw - 1; x++) {
                float sum = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int iy = (y * ds + dy * ds);
                        int ix = (x * ds + dx * ds);
                        sum += (yData[iy * rowStride + ix] & 0xFF);
                    }
                }
                blurred[y * sw + x] = sum / 9.0f;
            }
        }

        // Threshold: find max brightness
        float maxB = 0;
        float avgB = 0;
        for (float b : blurred) {
            if (b > maxB) maxB = b;
            avgB += b;
        }
        avgB /= blurred.length;
        float threshold = avgB + (maxB - avgB) * 0.5f; // Simple threshold

        List<Star> stars = new ArrayList<>();
        for (int y = 1; y < sh - 1; y++) {
            for (int x = 1; x < sw - 1; x++) {
                float val = blurred[y * sw + x];
                if (val > threshold) {
                    // Local maxima
                    if (val >= blurred[(y - 1) * sw + x] && val >= blurred[(y + 1) * sw + x] &&
                        val >= blurred[y * sw + (x - 1)] && val >= blurred[y * sw + (x + 1)]) {
                        stars.add(new Star(x * ds, y * ds, val));
                    }
                }
            }
        }

        Collections.sort(stars);
        if (stars.size() > 50) {
            return stars.subList(0, 50);
        }
        return stars;
    }
}
