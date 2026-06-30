import pandas as pd
from astroquery.simbad import Simbad
import time
import numpy as np
from astropy import units as u
from astropy.coordinates import SkyCoord

def cross_check_anomalies():
    Simbad.add_votable_fields('otype')
    anom_df = pd.read_csv('ruwe_anomalies_v2.csv')

    print("Detailed cross-check for RUWE anomalies...")
    # Check top 20 RUWE anomalies
    for sid in anom_df['source_id'].head(20):
        try:
            row = anom_df[anom_df['source_id'] == sid].iloc[0]
            ra, dec = row['ra'], row['dec']

            coord = SkyCoord(ra=ra*u.deg, dec=dec*u.deg)
            result = Simbad.query_region(coord, radius=2*u.arcsec)

            if result is not None and len(result) > 0:
                print(f"Source {int(sid)} (RUWE {row['ruwe']:.2f}): Found nearby object(s) in Simbad.")
                for i in range(len(result)):
                    cols = {c.upper(): c for c in result.colnames}
                    mid = result[cols['MAIN_ID']][i] if 'MAIN_ID' in cols else 'Unknown'
                    otype = result[cols['OTYPE']][i] if 'OTYPE' in cols else 'Unknown'
                    print(f"  - {mid} (Type: {otype})")
            else:
                print(f"Source {int(sid)} (RUWE {row['ruwe']:.2f}): No nearby objects in Simbad. (POSSIBLE DARK COMPANION HOST)")
        except Exception as e:
            print(f"Error querying {sid}: {e}")
        time.sleep(0.5)

if __name__ == "__main__":
    cross_check_anomalies()
