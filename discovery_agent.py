import numpy as np
from astropy.coordinates import SkyCoord
import astropy.units as u
from astroquery.gaia import Gaia
from astroquery.simbad import Simbad
import pandas as pd
import hdbscan
import sys
import time

# Increase row limit for Gaia queries
Gaia.ROW_LIMIT = 100000

def fetch_gaia_data(ra, dec, radius=0.5):
    """
    Fetches Gaia DR3 data for a given region.
    """
    query = f"""
    SELECT
        ra, dec, parallax, pmra, pmdec,
        phot_g_mean_mag, phot_bp_mean_mag, phot_rp_mean_mag,
        parallax_error, pmra_error, pmdec_error
    FROM gaiadr3.gaia_source
    WHERE CONTAINS(POINT('ICRS', ra, dec), CIRCLE('ICRS', {ra}, {dec}, {radius})) = 1
    AND parallax IS NOT NULL
    AND pmra IS NOT NULL
    AND pmdec IS NOT NULL
    AND phot_g_mean_mag > 15
    """
    try:
        job = Gaia.launch_job_async(query)
        results = job.get_results()
        return results.to_pandas()
    except Exception as e:
        print(f"  Gaia query failed: {e}")
        return pd.DataFrame()

def preprocess_data(df):
    """
    Normalizes features for clustering.
    Uses tangent plane projection for RA/Dec to avoid distortion.
    """
    if df.empty:
        return df, None

    center_ra = df.ra.mean()
    center_dec = df.dec.mean()

    ra_rad = np.radians(df.ra.values)
    dec_rad = np.radians(df.dec.values)
    ra0_rad = np.radians(center_ra)
    dec0_rad = np.radians(center_dec)

    denom = np.sin(dec_rad) * np.sin(dec0_rad) + np.cos(dec_rad) * np.cos(dec0_rad) * np.cos(ra_rad - ra0_rad)
    xi = np.cos(dec_rad) * np.sin(ra_rad - ra0_rad) / denom
    eta = (np.cos(dec0_rad) * np.sin(dec_rad) - np.sin(dec0_rad) * np.cos(dec_rad) * np.cos(ra_rad - ra0_rad)) / denom

    data = pd.DataFrame({
        'xi': np.degrees(xi),
        'eta': np.degrees(eta),
        'parallax': df.parallax.values,
        'pmra': df.pmra.values,
        'pmdec': df.pmdec.values
    })

    mean = data.mean()
    std = data.std()
    std[std == 0] = 1.0
    data_scaled = (data - mean) / std

    return data_scaled, (mean, std)

def isochrone_filter(cluster_stars):
    """
    Checks if the cluster stars follow a plausible CMD distribution.
    For UFDs: old, metal-poor population.
    """
    if len(cluster_stars) < 5:
        return False

    g = cluster_stars.phot_g_mean_mag
    bp_rp = cluster_stars.phot_bp_mean_mag - cluster_stars.phot_rp_mean_mag

    if bp_rp.std() > 1.5:
        return False

    if (g < 17).sum() / len(g) > 0.8:
        return False

    return True

def filter_clusters(df, labels):
    """
    Filters clusters based on kinematic coherence and other criteria.
    """
    unique_labels = set(labels)
    if -1 in unique_labels:
        unique_labels.remove(-1)

    candidates = []
    for label in unique_labels:
        cluster_stars = df[labels == label]

        if len(cluster_stars) < 8 or len(cluster_stars) > 5000:
            continue

        pmra_std = cluster_stars.pmra.std()
        pmdec_std = cluster_stars.pmdec.std()

        if pmra_std > 2.0 or pmdec_std > 2.0:
            continue

        parallax_std = cluster_stars.parallax.std()
        if parallax_std > 1.0:
            continue

        candidates.append({
            'label': label,
            'size': len(cluster_stars),
            'mean_ra': cluster_stars.ra.mean(),
            'mean_dec': cluster_stars.dec.mean(),
            'mean_pmra': cluster_stars.pmra.mean(),
            'mean_pmdec': cluster_stars.pmdec.mean(),
            'mean_parallax': cluster_stars.parallax.mean(),
            'pmra_std': pmra_std,
            'pmdec_std': pmdec_std,
            'parallax_std': parallax_std,
            'stars': cluster_stars
        })

    return candidates

def detect_clusters(df_scaled, min_cluster_size=8):
    """
    Applies HDBSCAN to detect clusters in the feature space.
    """
    if df_scaled.empty:
        return []

    clusterer = hdbscan.HDBSCAN(min_cluster_size=min_cluster_size, gen_min_span_tree=True, min_samples=2)
    cluster_labels = clusterer.fit_predict(df_scaled)

    return cluster_labels

def check_known_objects(ra, dec, radius=0.1):
    """
    Checks if there are known objects at the given coordinates using SIMBAD.
    """
    coord = SkyCoord(ra=ra*u.degree, dec=dec*u.degree, frame='icrs')
    try:
        result_table = Simbad.query_region(coord, radius=radius*u.degree)
        return result_table
    except Exception as e:
        print(f"  SIMBAD query failed: {e}")
        return None

def get_scan_regions(n_regions=1000, min_lat=50):
    """
    Generates a list of sky regions (RA, Dec, radius) to scan.
    """
    regions = []

    target_points = [
        (260.05, 57.91), # Draco
        (227.28, 67.22), # Draco II
        (210.03, 14.50), # Bootes I
        (158.07, 51.92), # Ursa Major I
        (153.26, -0.26), # Sextans
        (34.75, -54.05), # Phoenix
        (132.87, 45.40), # Leo V
        (202.48, 12.04), # Bootes III
        (239.58, 27.23), # Hercules
    ]
    for ra, dec in target_points:
        regions.append({'ra': ra, 'dec': dec, 'radius': 0.5})

    ra_range = np.linspace(0, 360, 60)
    dec_range = np.linspace(-85, 85, 30)
    for ra in ra_range:
        for dec in dec_range:
            c = SkyCoord(ra=ra*u.degree, dec=dec*u.degree, frame='icrs')
            if abs(c.galactic.b.value) > min_lat:
                regions.append({'ra': ra, 'dec': dec, 'radius': 0.5})

    return regions[:n_regions]

def run_discovery():
    print(f"STARTING DISCOVERY LOOP: Continuous Sky Scanning...")
    sys.stdout.flush()

    iteration = 0
    # Generate the full grid once
    all_grid_regions = get_scan_regions(n_regions=2000)

    while True:
        offset = (iteration // len(all_grid_regions)) * 0.1

        for region in all_grid_regions:
            iteration += 1
            ra = (region['ra'] + offset) % 360
            dec = region['dec']
            radius = region['radius']

            df = fetch_gaia_data(ra, dec, radius)
            num_stars = len(df)

            overdensities_detected = 0
            remaining_candidates = 0
            rejected = 0

            if num_stars >= 50:
                df_scaled, (mean, std) = preprocess_data(df)
                labels = detect_clusters(df_scaled)
                candidates = filter_clusters(df, labels)
                overdensities_detected = len(set(labels)) - (1 if -1 in labels else 0)

                final_candidates = []
                for cand in candidates:
                    known = check_known_objects(cand['mean_ra'], cand['mean_dec'])

                    is_known_structure = False
                    if known is not None:
                        for row in known:
                            match_name = ""
                            if 'MAIN_ID' in known.colnames:
                                match_name = row['MAIN_ID']
                            elif len(known.colnames) > 0:
                                match_name = row[known.colnames[0]]

                            if isinstance(match_name, bytes):
                                match_name = match_name.decode()

                            match_name = match_name.lower()
                            keywords = ['galaxy', 'cl', 'dwarf', 'gc', 'ngc', 'ugc', 'pgc',
                                        'draco', 'ursa', 'sextans', 'vcc', 'pcc', 'hgc',
                                        'segue', 'bootes', 'leo', 'canes', 'hercules', 'coma', 'pisces',
                                        'sculptor', 'fornax', 'carina', 'antlia', 'leo', 'tucana']
                            if any(word in match_name for word in keywords):
                                is_known_structure = True
                                break

                    if not is_known_structure:
                        if isochrone_filter(cand['stars']):
                            novelty_score = 1.0 / (1.0 + cand['pmra_std'] + cand['pmdec_std'])
                            cand['novelty_score'] = novelty_score
                            final_candidates.append(cand)
                        else:
                            rejected += 1
                    else:
                        rejected += 1

                remaining_candidates = len(final_candidates)
                rejected += (overdensities_detected - len(candidates))

            print(f"--- REPORT ---")
            print(f"Iteration: {iteration}")
            print(f"Sky Region: RA={ra:.2f}, Dec={dec:.2f}, Radius={radius}")
            print(f"Stars Processed: {num_stars}")
            print(f"Overdensities Detected: {overdensities_detected}")
            print(f"Rejected: {rejected}")
            print(f"Remaining Candidates: {remaining_candidates}")
            if remaining_candidates > 0:
                scores = [f"{c['novelty_score']:.2f}" for c in final_candidates]
                print(f"Confidence Scores: {', '.join(scores)}")
                for cand in final_candidates:
                    if cand['novelty_score'] >= 0.95:
                        print(f"POTENTIAL NEW ULTRA-FAINT DWARF GALAXY CANDIDATE")
                        print(f"Coordinates: ({cand['mean_ra']:.4f}, {cand['mean_dec']:.4f})")
                        print(f"Size: {cand['size']} stars")
                        print(f"PM: ({cand['mean_pmra']:.2f}±{cand['pmra_std']:.2f}, {cand['mean_pmdec']:.2f}±{cand['pmdec_std']:.2f})")
            else:
                print(f"Confidence Scores: N/A")

            print(f"Next Planned Action: Scan region {iteration + 1}")
            print(f"--------------")
            sys.stdout.flush()

if __name__ == "__main__":
    run_discovery()
