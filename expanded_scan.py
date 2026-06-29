import numpy as np
from cluster_hunter import OpenStarClusterHunter
from astropy.coordinates import SkyCoord
import astropy.units as u
import time
import pandas as pd
import os

def expanded_scan():
    # Use slightly smaller min_cluster_size for discovery of sparse clusters
    hunter = OpenStarClusterHunter(min_cluster_size=15)

    # Range of Galactic longitudes (let's sample systematically)
    l_range = np.arange(0, 360, 20)
    b_values = [5, -5, 10, -10, 20, -20]

    # Check if we have previous results to avoid duplication if restarted
    if os.path.exists('found_candidates.csv'):
        existing_df = pd.read_csv('found_candidates.csv')
        print(f"Loaded {len(existing_df)} existing candidates.")
    else:
        existing_df = pd.DataFrame(columns=['ra', 'dec', 'score', 'n_stars'])

    for b in b_values:
        for l in l_range:
            gal_coord = SkyCoord(l=l*u.deg, b=b*u.deg, frame='galactic')
            icrs = gal_coord.icrs
            ra = icrs.ra.deg
            dec = icrs.dec.deg

            print(f"\n--- SCANNING: l={l}, b={b} (RA={ra:.2f}, Dec={dec:.2f}) ---")

            try:
                # Radius 0.8 to cover a decent area without too much overlap/crowding
                hunter.run_scan(ra=ra, dec=dec, radius=0.8)
            except Exception as e:
                print(f"Scan failed for RA={ra:.2f}, Dec={dec:.2f}: {e}")

            # Periodically save results
            if hunter.candidates:
                summary = []
                for cand in hunter.candidates:
                    summary.append({
                        'ra': cand['ra'],
                        'dec': cand['dec'],
                        'score': cand['score'],
                        'n_stars': len(cand['data'])
                    })
                new_df = pd.DataFrame(summary)
                combined_df = pd.concat([existing_df, new_df]).drop_duplicates(subset=['ra', 'dec'])
                combined_df.to_csv('found_candidates.csv', index=False)

            time.sleep(1.5) # Respectful delay

    hunter.report_findings()

if __name__ == "__main__":
    expanded_scan()
