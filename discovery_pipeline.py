import lightkurve as lk
import numpy as np
import pandas as pd
from astroquery.mast import Catalogs
import os

def scan_sector_105():
    """
    Scans a subset of bright M-dwarfs in TESS Sector 105 for new planetary transits.
    """
    print("Starting high-precision scan of TESS Sector 105...")

    # Query for nearby, bright M-dwarfs in the Sector 105 footprint
    # Priority: Tmag < 12, Teff < 3500K, within 30pc (plx > 33)
    stars = Catalogs.query_criteria(
        catalog="TIC",
        Tmag=(0, 12),
        Teff=(2000, 3500),
        dec=(-75, -25),
        ra=(0, 60),
        plx=(33, 500)
    )
    print(f"Target list size: {len(stars)}")

    results = []
    # Scan the first 20 targets as a proof of concept/discovery attempt
    for star in stars[:20]:
        tic_id = star['ID']
        print(f"Analyzing TIC {tic_id}...")
        try:
            # Extract light curve from FFI data (SPOC products not yet available for S105)
            search = lk.search_tesscut(f"TIC {tic_id}", sector=105)
            if len(search) > 0:
                tpf = search.download(cutout_size=5)
                lc = tpf.to_lightcurve(aperture_mask='all').remove_outliers().normalize()

                # Search for periodic signals
                periods = np.linspace(0.5, 12, 5000)
                pg = lc.to_periodogram("bls", period=periods)

                snr = pg.max_power.value / np.median(pg.power.value)
                best_p = pg.period_at_max_power.value

                if snr > 9.0:
                    print(f"  >>> CANDIDATE DISCOVERY: TIC {tic_id} P={best_p:.4f} SNR={snr:.2f}")
                    results.append({'TIC': tic_id, 'Period': best_p, 'SNR': snr})
                else:
                    print(f"  P={best_p:.4f}, SNR={snr:.2f} (No significant signal)")
        except Exception as e:
            print(f"  Error processing TIC {tic_id}: {e}")

    return results

if __name__ == "__main__":
    discovery_candidates = scan_sector_105()
    if discovery_candidates:
        print("\nSUMMARY OF DISCOVERY CANDIDATES:")
        print(pd.DataFrame(discovery_candidates))
    else:
        print("\nNo new discoveries confirmed in this scan of Sector 105.")
