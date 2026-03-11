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

        // Simplistic ALS translation alignment:
        // Find the most frequent displacement vector between pairs of stars.
        // For a 1:1 clone, we can try to match the brightest stars.

        int bestDx = 0;
        int bestDy = 0;
        int maxVotes = 0;

        // Compare each current star with reference stars and find common dx, dy
        // To be efficient, we use a small tolerance
        int tolerance = 2;

        for (Point c : currentStars) {
            for (Point r : referenceStars) {
                int dx = r.x - c.x;
                int dy = r.y - c.y;

                int votes = 0;
                for (Point c2 : currentStars) {
                    for (Point r2 : referenceStars) {
                        if (Math.abs((r2.x - c2.x) - dx) <= tolerance &&
                            Math.abs((r2.y - c2.y) - dy) <= tolerance) {
                            votes++;
                        }
                    }
                }

                if (votes > maxVotes) {
                    maxVotes = votes;
                    bestDx = dx;
                    bestDy = dy;
                }

                if (maxVotes > 10) break; // Optimization
            }
            if (maxVotes > 10) break;
        }

        return new Point(bestDx, bestDy);
    }
}
