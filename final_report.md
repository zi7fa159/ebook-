# Transient Discovery Report (Last 7 Days)

**No discovery candidate survived rigorous validation.**

## Summary of Investigation
Over the last 7 days, a total of 1,000 recent transient alerts from the ZTF survey were retrieved via the ALeRCE Python client and analyzed. The search targeted events whose first detection occurred within the last 7 days (MJD 61215.94 to 61222.94).

## Explanation of Results

### Why each candidate failed:
1.  **High-Confidence Supernova Candidates**: Several candidates with high ALeRCE SN probabilities (e.g., **ZTF26abdtryj**, **ZTF26abdtsbc**, **ZTF26abdukrq**) were already cataloged in the Transient Name Server (TNS) under designations such as **SN 2026qbu** and **AT 2026qcg**. These events were correctly identified but are not "overlooked."
2.  **Stellar Variables and Known Stars**: Promising candidate **ZTF26abecvzp** (RA: 321.9488, Dec: 7.1824), which had an 86.8% SN probability, was found to coincide exactly with a known star in Gaia DR2 (Mag_G = 19.82). The detection was likely due to a slight brightening of a faint star rather than a new transient event.
3.  **Solar System Objects**: A significant number of candidates with multiple detections (e.g., **ZTF26abdtpas**, **ZTF26abdtqfr**) were cross-matched against the Minor Planet Center (MPC) database using the MPChecker tool. All these candidates were confirmed as known asteroids.
4.  **Single-Detection Artifacts**: Candidates with only one detection in the last 7 days and no prior history were overwhelmingly classified as asteroids or image artifacts (bogus) by ALeRCE classifiers.

### False Positive Sources:
-   **Known Asteroids**: Most alerts that lacked a SIMBAD/Gaia match but showed consistent motion were confirmed as known minor planets.
-   **Catalog Latency**: A small lag between ZTF alert generation and TNS reporting means that the most recent real transients have often already been flagged by other brokers or survey pipelines (like Fink or ALeRCE's own reporting bot).
-   **Faint Stellar Fluctuations**: Variability in very faint stars (near the detection limit of Gaia/SDSS) can occasionally mimic the start of a transient light curve.

## Improvements for Future Searches
-   **Automated TNS API Integration**: Implementing direct TNS API cross-matching would allow for real-time exclusion of reported events, enabling a deeper search into lower-probability candidates.
-   **Rubin/LSST Alerts**: As LSST data becomes more available, integrating it as a primary source will provide deeper limiting magnitudes, potentially revealing transients in extremely faint host galaxies that ZTF misses.
-   **Host Galaxy Offset Analysis**: Prioritizing transients with significant offsets from known galaxy centers may help distinguish SNe from nuclear activity (AGN) and CVs more effectively.
