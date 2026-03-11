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

    public StarDetector(int width, int height) {
        this.width = width;
        this.height = height;
        this.threshold = 40; // Basic brightness threshold, can be adjusted
    }

    public List<Point> detectStars(byte[] grayData) {
        // 1. Simple 3x3 Blur (Box filter) to reduce noise
        byte[] blurred = new byte[width * height];
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
        Collections.sort(candidates, new Comparator<StarCandidate>() {
            @Override
            public int compare(StarCandidate o1, StarCandidate o2) {
                return Integer.compare(o2.brightness, o1.brightness);
            }
        });

        List<Point> stars = new ArrayList<>();
        int limit = Math.min(candidates.size(), 50);
        for (int i = 0; i < limit; i++) {
            stars.add(new Point(candidates.get(i).x, candidates.get(i).y));
        }

        return stars;
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
