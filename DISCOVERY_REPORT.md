# Transient Discovery Report - Last 7 Days

**Investigation Period:** 2026-06-25 to 2026-07-02 (UTC)
**Source:** ALeRCE / ZTF Alert Stream
**Status:** 3 High-Confidence Candidates Identified

---

## Candidate 1: ZTF26abdvnkt
*   **Coordinates:** RA 275.98974, Dec -15.71914
*   **First Detection UTC:** 2026-06-25 08:02:08
*   **Survey ID:** ZTF26abdvnkt
*   **Light Curve Summary:**
    *   Rapid brightening: Rose from 18.95 to 17.30 mag in ~0.9 days.
    *   Last Detection: 2026-06-27 09:03:36 at 17.48 mag.
    *   Total Detections: 5
*   **Images Reviewed:** Science, Reference, and Difference FITS stamps verified. Clear difference image signal (S/N ~100).
*   **Cross-match Results:**
    *   SIMBAD: No match within 5".
    *   Gaia DR3: No source within 5".
    *   MPC: No known asteroid at this position/time.
*   **Evidence Supporting Novelty:** Fast rise time (>1 mag/day), high Deep-RB score (0.998), stationary coordinates (scatter < 0.1"), and no historical detections.
*   **Confidence Estimate:** 95%
*   **Recommended Follow-up:** Spectroscopic classification; likely a young Supernova.

## Candidate 2: ZTF26abdtuat
*   **Coordinates:** RA 197.10155, Dec 15.88213
*   **First Detection UTC:** 2026-06-25 05:10:18
*   **Survey ID:** ZTF26abdtuat
*   **Light Curve Summary:**
    *   Consistent rise: 20.00 to 19.34 mag over 5 days.
    *   Total Detections: 7
*   **Cross-match Results:** No SIMBAD/Gaia matches.
*   **Evidence Supporting Novelty:** High Deep-RB score (0.999), stationary, consistent brightening.
*   **Confidence Estimate:** 85%

## Candidate 3: ZTF26abekmjr
*   **Coordinates:** RA 190.56616, Dec 3.31254
*   **First Detection UTC:** 2026-06-25 05:01:42
*   **Survey ID:** ZTF26abekmjr
*   **Light Curve Summary:** 19.81 to 19.53 mag over 5 days.
*   **Cross-match Results:** No SIMBAD/Gaia matches.
*   **Evidence Supporting Novelty:** Stationary, high Deep-RB (0.996).
*   **Confidence Estimate:** 75%

---

## Search Strategy and Validation
- **Filtering:** Screened 40,000+ alerts from the last 7 days. Applied filters for `ndet > 1` (noise reduction), `ndethist < 20` (fresh transients only), and `stellar < 0.9`.
- **Validation:** Remaining candidates were cross-matched against SIMBAD, Gaia DR3, and the Minor Planet Center.
- **Stationarity:** Top candidates were checked for coordinate stability to rule out uncatalogued asteroids.
- **Visuals:** ZTF stamps were retrieved for the highest-confidence candidate (ZTF26abdvnkt) to confirm a real point-source detection in the difference image.

## False Positive Analysis
- **Moving Objects:** Several candidates showed arcsecond-scale motion between detections and were rejected as asteroids.
- **Image Artifacts:** Objects with low Real-Bogus (RB) scores or inconsistent light curves were rejected as potential subtraction residuals.
- **Known Variables:** Gaia cross-matches revealed most other brightening events were associated with known stars.
