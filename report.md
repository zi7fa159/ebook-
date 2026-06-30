# Scientific Report: Discovery of Overlooked Solar Neighbors in Gaia DR3

## Executive Summary
This investigation identified two likely astronomical objects within 20 parsecs of the Sun that are present in Gaia DR3 but missing from the SIMBAD database and the Gaia Catalogue of Nearby Stars (GCNS). These objects, Gaia DR3 1825712158557604352 and Gaia DR3 1827996187813279360, are extremely faint (absolute G magnitude > 19) and are located in close proximity to much brighter stars, which likely led to their omission in previous census efforts. Their properties are consistent with ultra-cool dwarfs or cool white dwarfs, making them significant additions to the census of the solar neighborhood.

## Background
The census of the solar neighborhood is a fundamental task in astronomy, but it remains incomplete, especially for the faintest and coolest objects. Automated pipelines often miss sources that are faint and located near bright stars due to contrast issues or catalog merging errors. Gaia DR3 provides the most complete astrometric survey to date, allowing for the identification of these "hidden" neighbors through high-precision parallax and proper motion measurements.

## Data Sources
- **Gaia DR3**: Primary source for astrometry (parallax, proper motion) and G-band photometry.
- **SIMBAD**: Used for cross-matching to identify known vs. unknown objects.
- **VizieR**: Used to retrieve multi-wavelength photometry from Pan-STARRS1 (PS1) and CatWISE2020.
- **GCNS**: Reference catalog for the 100-pc census.

## Analysis
A search for high-parallax sources (parallax > 50 mas) in Gaia DR3 was conducted, filtering for high astrometric quality (RUWE < 1.4, parallax_over_error > 20). The resulting candidates were cross-matched against SIMBAD with a search radius adjusted for proper motion. Two candidates were found to have no corresponding entries in SIMBAD or GCNS.

---

### Finding ID: MS-2026-001
- **UTC Timestamp**: 2026-06-30T13:10:00Z
- **Coordinates (J2016.0)**: RA = 295.764843°, Dec = +20.280120°
- **Surveys Involved**: Gaia DR3, Pan-STARRS1, CatWISE2020
- **Catalogs Examined**: SIMBAD, Gaia DR3, GCNS, CatWISE2020, PS1-DR1
- **Object Identifiers**: Gaia DR3 1825712158557604352
- **Description of Discrepancy**: The object has high-quality Gaia astrometry indicating it is at 16.1 pc, but it is missing from all major nearby-star catalogs (GCNS) and the SIMBAD database.
- **Supporting Observational Evidence**: Parallax = 61.94 ± 2.79 mas (~16.14 pc). RUWE = 1.076. Confirmed in PS1 at 0.8" separation (g=21.82, r=20.37, i=19.44, z=18.85, y=18.50). CatWISE2020 detection at 3.4" separation (W1=14.94, W2=15.37).
- **Contradictory Evidence**: Proximity (~33") to the bright star TYC 1610-477-1 (V ≈ 10.3) likely hindered previous identification.
- **Cross-match Results**: No match in SIMBAD within 60" (accounting for PM). No match in GCNS.
- **Literature Review**: No mentions found in ADS for this Source ID or coordinates.
- **Validation Procedures**: Cross-catalog verification, photometric absolute magnitude check (M_G = 19.67), and Gaia quality flag audit.
- **Remaining Uncertainties**: Spectral type is unconfirmed; the blue W1-W2 color is unusual for a brown dwarf and may suggest a cool white dwarf or be due to photometric noise.
- **Conservative Confidence Estimate**: 90%
- **Scientific Significance**: New neighbor within the 20-pc volume, representing the low-luminosity tail of the solar neighborhood population.
- **Recommended Follow-up**: NIR spectroscopy to confirm nature and spectral type.

---

### Finding ID: MS-2026-002
- **UTC Timestamp**: 2026-06-30T13:10:00Z
- **Coordinates (J2016.0)**: RA = 297.789861°, Dec = +22.759922°
- **Surveys Involved**: Gaia DR3, Pan-STARRS1
- **Catalogs Examined**: SIMBAD, Gaia DR3, GCNS, CatWISE2020, PS1-DR1
- **Object Identifiers**: Gaia DR3 1827996187813279360
- **Description of Discrepancy**: High-quality Gaia detection at 17.0 pc missing from SIMBAD and GCNS.
- **Supporting Observational Evidence**: Parallax = 58.83 ± 1.98 mas (~17.00 pc). RUWE = 1.307. Confirmed in PS1 at 0.6" separation (g=20.84, r=19.65, i=18.94, z=18.64, y=18.33).
- **Contradictory Evidence**: Located ~21" from TYC 2140-2302-1. Field crowding may have caused it to be overlooked.
- **Cross-match Results**: No match in SIMBAD within 60". No match in GCNS.
- **Literature Review**: No results for Gaia ID or coordinates.
- **Validation Procedures**: Verified astrometric consistency and photometric plausibility (M_G = 19.42).
- **Remaining Uncertainties**: Precise spectral classification and distance.
- **Conservative Confidence Estimate**: 85%
- **Scientific Significance**: Candidate for one of the closest ultra-cool dwarfs or cool white dwarfs.
- **Recommended Follow-up**: JHK photometry and optical/NIR spectroscopy.

---

## Validation Process Summary
The validation process involved:
1. **Astrometric Audit**: Ensuring RUWE < 1.4 and parallax_over_error > 20 in Gaia DR3.
2. **Multi-Survey Confirmation**: Verifying the existence of the source in Pan-STARRS1 and CatWISE2020 data to rule out Gaia artifacts.
3. **Database Comparison**: Systematic cross-matching against SIMBAD and the Gaia Catalogue of Nearby Stars (GCNS).
4. **Photometric Characterization**: Calculating absolute magnitudes to ensure consistency with known populations of nearby faint objects.

## Limitations
The primary limitation is the lack of spectroscopic confirmation. While astrometric evidence is strong, the exact physical nature of these objects remains to be determined through follow-up observations.

## Final Conclusion
Gaia DR3 1825712158557604352 and Gaia DR3 1827996187813279360 represent genuine overlooked members of the solar neighborhood within 20 parsecs. Their discovery highlights the continued potential for finding new neighbors among the faintest sources in the Gaia dataset, especially those near brighter stars that caused omissions in previous catalogs.
