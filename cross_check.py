import pandas as pd
from astroquery.simbad import Simbad
import time
import numpy as np
from astropy import units as u
from astropy.coordinates import SkyCoord

def cross_check_simbad():
    Simbad.add_votable_fields('otype')

    hvs = pd.read_csv('hvs_validation.csv')
    cand_df = pd.read_csv('hvs_candidates.csv')

    print("Detailed cross-check for HVS candidates...")
    for sid in hvs['source_id'].head(15):
        try:
            matches = cand_df[np.abs(cand_df['source_id'].astype(float) - float(sid)) < 1000.0]
            if matches.empty:
                continue
            row = matches.iloc[0]
            ra, dec = row['ra'], row['dec']

            coord = SkyCoord(ra=ra*u.deg, dec=dec*u.deg)
            result = Simbad.query_region(coord, radius=2*u.arcsec)

            if result is not None and len(result) > 0:
                print(f"Source {int(sid)}: Found nearby object(s) in Simbad.")
                for i in range(len(result)):
                    # robust column access
                    cols = {c.upper(): c for c in result.colnames}
                    mid = result[cols['MAIN_ID']][i] if 'MAIN_ID' in cols else 'Unknown'
                    otype = result[cols['OTYPE']][i] if 'OTYPE' in cols else 'Unknown'
                    print(f"  - {mid} (Type: {otype})")
            else:
                print(f"Source {int(sid)}: No nearby objects in Simbad within 2 arcsec. (STRONG Candidate)")
        except Exception as e:
            print(f"Error querying {sid}: {e}")
        time.sleep(0.5)

if __name__ == "__main__":
    cross_check_simbad()
