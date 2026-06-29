import numpy as np
from cluster_hunter import OpenStarClusterHunter
from astropy.coordinates import SkyCoord
import astropy.units as u
import time
import pandas as pd

def systematic_scan():
    hunter = OpenStarClusterHunter(min_cluster_size=20)

    # Define galactic longitude range to scan
    # Let's try a region in the second quadrant (l=90 to 180)
    # and slightly off the midplane to avoid the most extreme crowding/extinction
    l_range = np.arange(100, 160, 2)
    b_values = [0, 2, -2, 5, -5]

    results = []

    for b in b_values:
        for l in l_range:
            gal_coord = SkyCoord(l=l*u.deg, b=b*u.deg, frame='galactic')
            icrs = gal_coord.icrs
            ra = icrs.ra.deg
            dec = icrs.dec.deg

            print(f"\n--- Scanning Galactic (l={l}, b={b}) -> ICRS (RA={ra:.2f}, Dec={dec:.2f}) ---")

            try:
                hunter.run_scan(ra=ra, dec=dec, radius=0.7)
                # After each scan, check if new candidates were added
                if hunter.candidates:
                    # We only care about the latest ones added in this specific scan
                    # (Though run_scan appends to hunter.candidates)
                    pass
            except Exception as e:
                print(f"Error during scan: {e}")

            # Sleep briefly to be nice to Gaia servers
            time.sleep(1)

    hunter.report_findings()

    # Save candidates to a file for later analysis
    if hunter.candidates:
        summary = []
        for cand in hunter.candidates:
            summary.append({
                'ra': cand['ra'],
                'dec': cand['dec'],
                'score': cand['score'],
                'n_stars': len(cand['data'])
            })
        pd.DataFrame(summary).to_csv('found_candidates.csv', index=False)
        print("Saved candidates to found_candidates.csv")

if __name__ == "__main__":
    systematic_scan()
