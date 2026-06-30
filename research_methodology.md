# Research Methodology: Discovery of Uncatalogued High-Proper-Motion Objects

## Objective
The primary objective of this investigation is to identify previously unknown high-proper-motion (HPM) objects in the solar neighborhood using Gaia DR3 data. We specifically target faint objects ($G > 15$) that may have been overlooked in previous surveys or cataloged only as anonymous Gaia sources without cross-identifications in SIMBAD. Such objects are likely candidates for nearby low-mass stars (M dwarfs), brown dwarfs (L, T, Y dwarfs), or cool white dwarfs.

## Target Selection Criteria
We will query the `gaiadr3.gaia_source` table for objects satisfying the following criteria:
- **High Proper Motion**: $\mu > 200$ mas/yr (indicating proximity or high tangential velocity).
- **Faintness**: $G > 15$ mag (to avoid well-studied bright stars).
- **Astrometric Quality**: RUWE (Renormalised Unit Weight Error) $< 1.4$ (to ensure a reliable single-source astrometric solution).
- **Galactic Latitude**: $|b| > 30^\circ$ (to minimize crowding and extinction effects in the Galactic plane).

## Methodology

### 1. Candidate Identification (Gaia DR3)
Using `astroquery.gaia`, we will perform an ADQL query to retrieve sources matching our criteria. We will limit the search to a representative high-latitude region or a random sample if the global count is too high for the initial phase.

### 2. Cross-matching with SIMBAD
Each candidate will be cross-matched against the SIMBAD database using `astroquery.simbad`. Sources that already possess identifiers other than Gaia-specific designations will be filtered out.

### 3. Multi-wavelength Validation
For uncatalogued candidates, we will retrieve infrared photometry from:
- **2MASS**: J, H, Ks magnitudes.
- **AllWISE**: W1, W2 magnitudes.

Infrared colors (e.g., $J-H$, $H-Ks$, $W1-W2$, $G-RP$) will be used to characterize the candidates. We expect nearby low-mass objects to be significantly redder in these color indices.

### 4. False Positive Rejection
Candidates will be scrutinized for:
- Consistent astrometry between Gaia and 2MASS/WISE (if applicable).
- Quality flags in WISE (e.g., contamination, diffraction spikes).
- Gaia flags (e.g., `astrometric_excess_noise`, `duplicated_source`).

### 5. Scientific Characterization
Surviving candidates will be evaluated for their estimated distance (via Gaia parallax) and spectral type (via color-magnitude relations).

## Success Criteria
A successful discovery is defined as an object with confirmed high proper motion and reliable photometry that is not currently identified in SIMBAD as a known astronomical object.
