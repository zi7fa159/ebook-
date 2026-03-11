package com.alsclone.astrostacker;

import android.graphics.Point;
import java.util.List;

public class FrameAligner {
    private List<Point> referenceStars;

    public void setReferenceStars(List<Point> stars) {
        this.referenceStars = stars;
    }

    public Point computeShift(List<Point> currentStars) {
        if (referenceStars == null || referenceStars.isEmpty() || currentStars == null || currentStars.isEmpty()) {
            return new Point(0, 0);
        }

        // Optimized displacement histogram approach
        // Max expected shift is say 200 pixels in downscaled coords
        int limit = 200;
        int size = limit * 2 + 1;
        int[] hist = new int[size * size];

        for (Point r : referenceStars) {
            for (Point c : currentStars) {
                int dx = r.x - c.x;
                int dy = r.y - c.y;

                if (Math.abs(dx) <= limit && Math.abs(dy) <= limit) {
                    hist[(dy + limit) * size + (dx + limit)]++;
                }
            }
        }

        int maxVotes = 0;
        int bestDx = 0;
        int bestDy = 0;

        // Find peak in histogram with a small 3x3 window to handle slight noise/centroiding jitter
        for (int y = 1; y < size - 1; y++) {
            for (int x = 1; x < size - 1; x++) {
                int votes = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        votes += hist[(y + dy) * size + (x + dx)];
                    }
                }
                if (votes > maxVotes) {
                    maxVotes = votes;
                    bestDx = x - limit;
                    bestDy = y - limit;
                }
            }
        }

        // If very few stars match, assume no movement to avoid jumping on noise
        if (maxVotes < 3) return new Point(0, 0);

        return new Point(bestDx, bestDy);
    }
}
