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

        if (maxVotes < 3) return new Point(0, 0);

        // Sub-pixel centroid estimation on the histogram peak
        float sumX = 0, sumY = 0, sumV = 0;
        int bx = bestDx + limit;
        int by = bestDy + limit;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int v = hist[(by + dy) * size + (bx + dx)];
                sumX += (bestDx + dx) * v;
                sumY += (bestDy + dy) * v;
                sumV += v;
            }
        }

        float subDx = bestDx;
        float subDy = bestDy;
        if (sumV > 0) {
            subDx = sumX / sumV;
            subDy = sumY / sumV;
        }

        return new Point(Math.round(subDx), Math.round(subDy));
    }
}
