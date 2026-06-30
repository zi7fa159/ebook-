# Scientific Report: Discovery of Uncatalogued Faint High-Proper-Motion Objects in Gaia DR3

## Objectives
The primary objective of this study was to identify previously unknown high-proper-motion (HPM) objects in the solar neighborhood. We focused on faint sources ($G > 18$) in Gaia DR3 that lack identification in the SIMBAD astronomical database. These objects are prime candidates for nearby ultracool dwarfs or cool white dwarfs that have been overlooked due to their faintness.

## Datasets Analyzed
- **Gaia DR3**: Primary catalog for astrometry (proper motion, parallax) and photometry ($G, BP, RP$).
- **AllWISE**: Infrared photometry ($W1, W2$) used for color characterization and validation.
- **2MASS**: Near-infrared photometry ($J, H, Ks$) for multi-wavelength consistency checks.
- **SIMBAD**: Used for cross-identification and filtering of previously known objects.

## Methodology
1. **Initial Selection**: Queried Gaia DR3 for sources with $\mu > 200$ mas/yr, $G > 18$, RUWE $< 1.4$, and Galactic latitude $|b| > 30^\circ$. A sample of 500 candidates was selected, ordered by $G$ magnitude (faintest first).
2. **Cross-identification Filtering**: Each candidate was cross-matched against SIMBAD. Sources with existing identifiers (beyond Gaia and TIC) or those with any SIMBAD object within 1 arcminute were rejected.
3. **Multi-wavelength Retrieval**: For uncatalogued candidates, AllWISE and 2MASS data were retrieved via VizieR.
4. **Final Scientific Review**: Candidates were evaluated based on:
   - Presence of infrared counterparts.
   - Cleanliness of WISE flags (`cc_f`).
   - Color indices ($G-W2 > 3.0$ or $BP-RP > 2.0$) consistent with cool stellar or substellar objects.
   - Absolute magnitude ($M_G$) calculated from Gaia parallax.

## Validation Procedures
- **Astrometric Consistency**: Checked that candidates had reliable Gaia solutions (RUWE < 1.4 and $\varpi/\sigma_\varpi > 5$).
- **Color-Magnitude Analysis**: Placed candidates on a color-magnitude diagram (using $M_G$ and $G-W2$) to ensure they occupy the expected locus for nearby low-mass objects.
- **Flag Inspection**: Automated rejection of WISE sources with significant diffraction spikes or persistence flags in the $W1$ and $W2$ bands.

## Results

### Rejected Candidates
- **Known Objects**: 452 sources from the initial 500 Gaia candidates were found to have existing identifications in SIMBAD or were in close proximity to known objects.
- **Validation Failures**: 2 candidates were rejected during final review due to suspicious WISE flags or lack of consistent red colors.

### Surviving Candidates (Discoveries)
46 candidates survived all filtering and validation steps. These represent high-confidence discoveries of uncatalogued high-proper-motion objects.

#### Top Discovery Candidates (Sample)
| Gaia DR3 Source ID | $\mu$ (mas/yr) | $\varpi$ (mas) | $G$ (mag) | $G-W2$ | Est. Dist (pc) | $M_G$ |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 6473285343147923072 | 224.4 | 7.5 | 20.84 | 5.43 | 133.6 | 15.22 |
| 4707605252467583616 | 223.5 | 5.8 | 20.82 | 3.73 | 173.4 | 14.62 |
| 4987326989167298560 | 215.3 | 6.8 | 20.75 | 4.62 | 146.7 | 14.92 |
| 6402746586984583296 | 221.2 | 7.0 | 20.70 | 5.40 | 142.5 | 14.93 |
| 5121760221446118272 | 207.3 | 4.4 | 20.64 | 7.14 | 226.5 | 13.87 |

## Evidence Supporting Conclusions
- **High Proper Motion**: The Gaia-measured proper motions are significantly above the background population, confirmed by the high significance of the astrometric solutions.
- **Red Colors**: The $G-W2$ colors ($3.7$ to $7.1$) are strongly indicative of late-type M dwarfs or early L dwarfs.
- **Absolute Magnitudes**: The calculated $M_G$ values (mostly $> 14$) are consistent with the low-mass end of the main sequence or the substellar regime.

## Evidence Contradicting Conclusions
- Some candidates lack 2MASS detections, which is expected given their faintness in $G$ and the shallower depth of 2MASS compared to WISE, but it remains a point of minor uncertainty for those specific sources.

## Confidence Assessments
- **Very High Confidence**: 38 candidates with clean AllWISE detections and $G-W2 > 4.0$.
- **High Confidence**: 8 candidates with AllWISE detections but slightly higher Gaia astrometric noise (though still within RUWE limits).

## Limitations
- **Epoch Differences**: The 16-year gap between 2MASS and Gaia DR3 can make cross-matching difficult for the fastest-moving objects, though the 1-arcminute search radius used in this study mitigates this.
- **Faintness**: At $G > 20$, Gaia photometry and parallax errors increase, though we maintained a $5\sigma$ parallax threshold.

## Recommendations for Future Work
- **Spectroscopic Follow-up**: IR spectroscopy (e.g., using SpeX on IRTF) is required to determine precise spectral types and confirm the nature of these objects.
- **Deep NIR Imaging**: Targeted $JHK$ imaging would provide more precise colors for the faintest candidates lacking 2MASS data.

## Final Conclusion
This investigation successfully identified 46 previously uncatalogued high-proper-motion objects in the Gaia DR3 dataset. The combination of high proper motion, significant parallax, and red infrared colors provides compelling evidence that these sources are nearby low-mass stars or brown dwarfs that have escaped previous detection. These results demonstrate the continued discovery potential of Gaia for the solar neighborhood census.
