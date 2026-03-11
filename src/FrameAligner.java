package com.example.astrostacker;

import java.util.List;

public class FrameAligner {
    public static class Translation {
        public float dx, dy;
        public int confidence;
    }

    private static final float MATCH_TOLERANCE_SQ = 3.0f * 3.0f;
    private static final int MIN_STARS_FOR_ALIGN = 8;
    private static final float MAX_ALLOWED_DRIFT = 50.0f;

    public Translation align(List<StarDetector.Star> refStars, List<StarDetector.Star> newStars) {
        if (refStars == null || newStars == null ||
            refStars.size() < MIN_STARS_FOR_ALIGN || newStars.size() < MIN_STARS_FOR_ALIGN) {
            return null;
        }

        // Use top stars to find candidate translations
        int searchLimit = Math.min(15, Math.min(refStars.size(), newStars.size()));

        float bestDx = 0;
        float bestDy = 0;
        int maxVotes = 0;

        for (int i = 0; i < searchLimit; i++) {
            for (int j = 0; j < searchLimit; j++) {
                // Candidate translation assuming newStars[i] matches refStars[j]
                float dx = newStars.get(i).x - refStars.get(j).x;
                float dy = newStars.get(i).y - refStars.get(j).y;

                if (Math.abs(dx) > MAX_ALLOWED_DRIFT || Math.abs(dy) > MAX_ALLOWED_DRIFT) {
                    continue;
                }

                int votes = 0;
                // Verify this translation against all stars
                for (StarDetector.Star rs : refStars) {
                    float tx = rs.x + dx;
                    float ty = rs.y + dy;

                    for (StarDetector.Star ns : newStars) {
                        float dSq = (ns.x - tx) * (ns.x - tx) + (ns.y - ty) * (ns.y - ty);
                        if (dSq < MATCH_TOLERANCE_SQ) {
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

        // Confidence threshold: at least 50% of stars or 6 stars must match
        if (maxVotes < Math.max(6, refStars.size() / 2)) {
            return null;
        }

        Translation t = new Translation();
        t.dx = bestDx;
        t.dy = bestDy;
        t.confidence = maxVotes;
        return t;
    }
}
