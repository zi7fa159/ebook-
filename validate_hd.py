from astroquery.simbad import Simbad
from astroquery.vizier import Vizier
from astropy.coordinates import SkyCoord
import astropy.units as u
import pandas as pd
import numpy as np

def validate_hd21962():
    # HD 21962 IDs
    id1 = 42768945033709568
    id2 = 42768532716777088

    print("--- Validating HD 21962 B/C ---")

    # 1. Literature search for HD 21962
    # HD 21962 is a bright F-type star. Let's see its hierarchy.
    result = Simbad.query_object("HD 21962")
    if result:
        print("Main HD 21962 found in SIMBAD.")

    # Search for all objects within 1 degree of HD 21962 with same PM/Plx
    # to see if it's a known moving group
    hd_coord = SkyCoord.from_name("HD 21962")
    v = Vizier(columns=['*'], catalog='I/355/gaiadr3')
    res = v.query_region(hd_coord, radius=1.0*u.deg)

    if len(res) > 0:
        gaia_res = res[0].to_pandas()
        # Filter for similar Plx and PM
        match = gaia_res[
            (abs(gaia_res['Plx'] - 24.3) < 1.0) &
            (abs(gaia_res['pmRA'] - 94.0) < 5.0) &
            (abs(gaia_res['pmDE'] - (-44.0)) < 5.0)
        ]
        print(f"Found {len(match)} stars in the vicinity with similar astrometry.")
        print(match[['Source', 'Plx', 'pmRA', 'pmDE']])

if __name__ == "__main__":
    validate_hd21962()
