package com.alsclone.astrostacker;

import android.graphics.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StarDetector {
    private final int width;
    private final int height;
    private final int threshold;
    private byte[] blurred;

    public StarDetector(int width, int height) {
        this.width = width;
        this.height = height;
        this.threshold = 40; // Basic brightness threshold, can be adjusted
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public float calculateSharpness(byte[] grayData) {
        List<float[]> stars = detectStarsCentroid(grayData);
        if (stars.isEmpty()) return 0;

        float totalSharpness = 0;
        int count = 0;
        for (float[] star : stars) {
            int sx = Math.round(star[0]);
            int sy = Math.round(star[1]);
            if (sx < 2 || sy < 2 || sx >= width - 2 || sy >= height - 2) continue;

            // Local Laplacian (rough measure of high frequency/tightness)
            float center = grayData[sy * width + sx] & 0xFF;
            float neighbors = (grayData[sy * width + sx - 1] & 0xFF) +
                              (grayData[sy * width + sx + 1] & 0xFF) +
                              (grayData[(sy - 1) * width + sx] & 0xFF) +
                              (grayData[(sy + 1) * width + sx] & 0xFF);

            float laplacian = 4 * center - neighbors;
            if (laplacian > 0) {
                totalSharpness += laplacian;
                count++;
            }
        }
        return count > 0 ? totalSharpness / count : 0;
    }

    public List<float[]> detectStarsCentroid(byte[] grayData) {
        // 1. Simple 3x3 Blur (Box filter) to reduce noise
        if (blurred == null || blurred.length != width * height) {
            blurred = new byte[width * height];
        }
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int sum = (grayData[(y-1)*width + (x-1)] & 0xFF) + (grayData[(y-1)*width + x] & 0xFF) + (grayData[(y-1)*width + (x+1)] & 0xFF) +
                          (grayData[y*width + (x-1)] & 0xFF) + (grayData[y*width + x] & 0xFF) + (grayData[y*width + (x+1)] & 0xFF) +
                          ((y+1)*width + (x-1) < grayData.length ? (grayData[(y+1)*width + (x-1)] & 0xFF) : 0) +
                          ((y+1)*width + x < grayData.length ? (grayData[(y+1)*width + x] & 0xFF) : 0) +
                          ((y+1)*width + (x+1) < grayData.length ? (grayData[(y+1)*width + (x+1)] & 0xFF) : 0);
                blurred[y * width + x] = (byte)(sum / 9);
            }
        }

        List<StarCandidate> candidates = new ArrayList<>();

        // Very basic star detection:
        // 1. Grid search for local maxima above threshold
        // 2. Simple 3x3 or 5x5 check

        int step = 4; // Skip some pixels for speed during initial scan
        int radius = 5;
        for (int y = radius; y < height - radius; y += step) {
            for (int x = radius; x < width - radius; x += step) {
                int val = blurred[y * width + x] & 0xFF;
                if (val > threshold) {
                    if (isLocalMaximum(blurred, x, y, 5)) {
                        candidates.add(new StarCandidate(x, y, val));
                    }
                }
            }
        }

        // Sort by brightness and take top 50
        Collections.sort(candidates, (o1, o2) -> Integer.compare(o2.brightness, o1.brightness));

        List<float[]> stars = new ArrayList<>();
        int limit = Math.min(candidates.size(), 50);
        for (int i = 0; i < limit; i++) {
            StarCandidate c = candidates.get(i);
            // Calculate sub-pixel centroid
            float sumX = 0, sumY = 0, sumW = 0;
            int r = 3;
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int ix = c.x + dx;
                    int iy = c.y + dy;
                    if (ix >= 0 && ix < width && iy >= 0 && iy < height) {
                        float w = (grayData[iy * width + ix] & 0xFF);
                        sumX += ix * w;
                        sumY += iy * w;
                        sumW += w;
                    }
                }
            }
            if (sumW > 0) {
                stars.add(new float[]{sumX / sumW, sumY / sumW, (float)c.brightness});
            } else {
                stars.add(new float[]{(float)c.x, (float)c.y, (float)c.brightness});
            }
        }

        return stars;
    }

    public List<Point> detectStars(byte[] grayData) {
        List<float[]> centroids = detectStarsCentroid(grayData);
        List<Point> points = new ArrayList<>();
        for (float[] c : centroids) {
            points.add(new Point(Math.round(c[0]), Math.round(c[1])));
        }
        return points;
    }

    private boolean isLocalMaximum(byte[] data, int cx, int cy, int radius) {
        int val = data[cy * width + cx] & 0xFF;
        for (int y = cy - radius; y <= cy + radius; y++) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                if (x == cx && y == cy) continue;
                if ((data[y * width + x] & 0xFF) >= val) {
                    return false;
                }
            }
        }
        return true;
    }

    private static class StarCandidate {
        int x, y, brightness;
        StarCandidate(int x, int y, int brightness) {
            this.x = x;
            this.y = y;
            this.brightness = brightness;
        }
    }
}
