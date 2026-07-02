# DISCOVERY REPORT: TESS SECTOR 105 SURVEY

## Objective
To identify genuine, previously unknown astronomical discoveries in the newly released TESS Sector 105 data (observations covering June 13 - July 1, 2026).

## Methodology
1. **Target Selection**: Filtered the TESS Input Catalog (TIC) for bright (Tmag < 12), nearby (d < 30pc) M-dwarfs within the Sector 105 footprint.
2. **Data Extraction**: Used `lightkurve` and `TESScut` to extract raw light curves from the Full Frame Images (FFIs), as pipeline-processed SPOC products are not yet available for this extremely fresh sector.
3. **Signal Detection**: Applied the Box Least Squares (BLS) algorithm to identify periodic transit signals with periods between 0.5 and 12 days.
4. **Validation**: Candidates were screened against known TOI/CTOI lists and analyzed for start-of-orbit systematics.

## Results
A systematic scan of 20 high-priority M-dwarfs was conducted.

| Target (TIC) | Status | Notes |
|--------------|--------|-------|
| 160263922    | Checked| Stable, no transit detected |
| 229142860    | Checked| Stable, no transit detected |
| 321908619    | Checked| High noise at start of orbit, no planetary signal |
| ...          | ...    | ... |

## Conclusion
At the time of reporting (July 2, 2026), no new planetary transits or transients meeting the high confidence threshold (SNR > 9) were discovered in the initial priority sample of TESS Sector 105. The survey demonstrates that the "fresh" data space is currently free of large, obvious discoveries in these specific targets, setting a baseline for deeper searches as the full sector data is processed.

*Discovery Pipeline Code included in `discovery_pipeline.py`.*
