package com.example.astrostacker;

import java.util.List;

public class FrameAligner {
    public static class Translation {
        public int dx, dy;
        public Translation(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    public Translation align(List<StarDetector.Star> refStars, List<StarDetector.Star> currentStars) {
        if (refStars.isEmpty() || currentStars.isEmpty()) {
            return new Translation(0, 0);
        }

        // More robust alignment:
        // For each of the top stars in the current frame, find the closest star in the reference frame.
        // Use the median displacement to avoid outliers.

        int limit = Math.min(currentStars.size(), 20);
        int[] dxs = new int[limit];
        int[] dys = new int[limit];
        int found = 0;

        for (int i = 0; i < limit; i++) {
            StarDetector.Star sCurrent = currentStars.get(i);
            StarDetector.Star bestMatch = null;
            double minDist = 50; // Max search radius

            for (StarDetector.Star sRef : refStars) {
                double dist = Math.sqrt(Math.pow(sCurrent.x - sRef.x, 2) + Math.pow(sCurrent.y - sRef.y, 2));
                if (dist < minDist) {
                    minDist = dist;
                    bestMatch = sRef;
                }
            }

            if (bestMatch != null) {
                dxs[found] = bestMatch.x - sCurrent.x;
                dys[found] = bestMatch.y - sCurrent.y;
                found++;
            }
        }

        if (found == 0) return new Translation(0, 0);

        // Simple average of found displacements
        int sumDx = 0, sumDy = 0;
        for (int i = 0; i < found; i++) {
            sumDx += dxs[i];
            sumDy += dys[i];
        }

        return new Translation(sumDx / found, sumDy / found);
    }
}
