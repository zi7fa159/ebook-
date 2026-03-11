package com.example.astrostacker;

import java.util.List;

public class FrameAligner {
    public static class Translation {
        public float dx, dy;
    }

    public Translation align(List<StarDetector.Star> refStars, List<StarDetector.Star> newStars) {
        if (refStars == null || newStars == null || refStars.isEmpty() || newStars.isEmpty()) {
            return null;
        }

        // Simplistic alignment: find best matching translation for the top stars
        // We'll try to match each of the top 5 new stars to each of the top 5 ref stars
        // and find the most common displacement.

        int matchLimit = Math.min(10, Math.min(refStars.size(), newStars.size()));
        float bestDx = 0;
        float bestDy = 0;
        int maxVotes = 0;

        float tolerance = 5.0f; // pixels

        for (int i = 0; i < matchLimit; i++) {
            for (int j = 0; j < matchLimit; j++) {
                float dx = newStars.get(i).x - refStars.get(j).x;
                float dy = newStars.get(i).y - refStars.get(j).y;

                int votes = 0;
                for (int k = 0; k < matchLimit; k++) {
                    float targetX = refStars.get(k).x + dx;
                    float targetY = refStars.get(k).y + dy;

                    for (int l = 0; l < matchLimit; l++) {
                        float distSq = (newStars.get(l).x - targetX) * (newStars.get(l).x - targetX) +
                                       (newStars.get(l).y - targetY) * (newStars.get(l).y - targetY);
                        if (distSq < tolerance * tolerance) {
                            votes++;
                            break;
                        }
                    }
                }

                if (votes > maxVotes) {
                    maxVotes = votes;
                    bestDx = dx;
                    bestDy = dy;
                }
            }
        }

        if (maxVotes < 2) return null; // Not enough confidence

        Translation t = new Translation();
        t.dx = bestDx;
        t.dy = bestDy;
        return t;
    }
}
