# Astronomical Discovery Report - Last 7 Days

**Search Date:** 2025-02-22
**Search Period:** 2025-02-15 to 2025-02-22
**Broker:** ALeRCE (ZTF)

---

## High-Confidence Candidate: ZTF26abdvyey

### 1. Basic Information
- **Object ID:** ZTF26abdvyey
- **Survey & Alert IDs:** ZTF (Alert ID: 3462380422015015000)
- **Coordinates (J2000):** RA 21h 38m 37.28s, Dec +04° 26' 01.6" (324.655319, 4.433789)
- **First Detection (UTC):** 2025-02-15 09:07:49 (MJD 61216.3804)
- **Latest Detection (UTC):** 2025-02-21 09:03:21 (MJD 61222.3773)

### 2. Light Curve Summary
The object shows a consistent and monotonic rise in brightness across both g and r bands.
- **First Mag:** 20.16 ± 0.18 (g)
- **Last Mag:** 19.08 ± 0.18 (g) / 18.92 ± 0.18 (r)
- **Amplitude:** Δmag ≈ 1.2 over 6 days.
- **Behavior:** Smooth rising trend typical of a young supernova.

### 3. Image Review
- **Science Image:** Clear point source detected.
- **Template Image:** No discernible source at the position.
- **Difference Image:** High signal-to-noise point source detection, no significant dipole or subtraction artifacts.

### 4. Cross-match Results
- **SIMBAD:** No known objects within 30 arcseconds.
- **TNS:** No reported transients at these coordinates found in public searches.
- **MPC:** Query for known moving objects at this position and time returned no matches. The stationary nature over 6 days confirms it is not a Solar System object.
- **ALeRCE Classification:** Currently unclassified.

### 5. Evidence Supporting Novelty
- **Genuinely Overlooked:** Not yet reported to TNS or classified by major brokers.
- **Rising Light Curve:** The multi-day rising trend is strongly indicative of a real astronomical transient (e.g., SN).
- **Clean Subtraction:** The difference image stamps are excellent, ruling out common image artifacts or cosmic rays.

### 6. Evidence Against Novelty
- **Faintness:** The object is faint (mag ~19-20), which can sometimes lead to noisy detections, though the 9-epoch consistency mitigates this.
- **Unseen Host:** There is no obvious host galaxy in the ZTF template; while this is common for high-redshift SNe or those in the outskirts of galaxies, a very faint galactic variable cannot be 100% ruled out without deeper archival imaging.

### 8. Confidence Estimate & Uncertainties
- **Confidence:** 90% (High)
- **Uncertainties:** Faintness of the initial detections (mag > 20) introduces some scatter, but the multi-epoch detections (9 total) provide high reliability.

### 9. Recommended Follow-up
- **Spectroscopy:** Essential for classification (potential SN Type Ia or II).
- **Multi-band Photometry:** Continued monitoring to identify the peak and decline.

---

## Rejected Candidates (Top 10 Analysis)

| Object ID | Result | Reason |
|-----------|--------|--------|
| ZTF26abebhvt | Rejected | Known source (ATO J279.5289+07.6770) |
| ZTF26abeiasz | Rejected | Likely high-proper motion star or subtraction noise. |
| ZTF26abeccxr | Rejected | Sensor artifact (edge of chip/dead pixels). |
| ZTF26abdukrq | Rejected | Known galaxy Z 74-64; likely an AGN flare or known SN. |
| ZTF26abearye | Rejected | Match to faint source DES J135024.65+093853.5. |
| ZTF26abdxgto | Rejected | Variable source with poor subtraction quality. |
| ZTF26abecjxu | Rejected | Artifact/Variable (dipole in difference). |

---

## Search Strategy Improvements
The search successfully identified a fresh transient candidate. However, the following improvements are recommended:
1. **Automated Cross-matching:** Integrate TNS API directly (requires key) to speed up exclusion of known reports.
2. **Host Galaxy Detection:** Use deep survey data (Legacy Survey, Pan-STARRS) to check for faint host galaxies which may indicate the nature of the transient.
3. **Machine Learning Filtering:** Incorporate ALeRCE's internal scores (e.g., real-bogus) earlier in the pipeline to reduce manual stamp inspection.
