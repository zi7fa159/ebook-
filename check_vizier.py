
from astroquery.vizier import Vizier
import astropy.units as u
from astropy.coordinates import SkyCoord
import pandas as pd

def check_vizier_clusters(ra, dec, radius_arcmin=10):
    coord = SkyCoord(ra=ra, dec=dec, unit=(u.deg, u.deg), frame='icrs')

    # Common open cluster catalogs on VizieR
    catalogs = [
        "J/A+A/640/A1/table1", # Cantat-Gaudin 2020
        "J/A+A/673/A114/clusters", # Hunt 2023
        "J/A+A/661/A118/table1", # Hao 2022
        "B/mwsc/catalog", # Kharchenko MWSC
        "J/A+A/618/A93/members", # Cantat-Gaudin 2018
    ]

    print(f"Checking VizieR for clusters near RA={ra}, Dec={dec} (r={radius_arcmin}')")

    results = {}
    for cat in catalogs:
        try:
            v = Vizier(columns=['*', '_r'])
            result = v.query_region(coord, radius=radius_arcmin*u.arcmin, catalog=cat)
            if result and len(result) > 0:
                results[cat] = result[0].to_pandas()
        except Exception as e:
            print(f"Error querying {cat}: {e}")

    return results

if __name__ == "__main__":
    # Candidate 1
    ra, dec = 272.2079, -8.9617
    res = check_vizier_clusters(ra, dec)
    if not res:
        print("No VizieR matches found in standard OC catalogs.")
    for cat, table in res.items():
        print(f"\nMatches in {cat}:")
        print(table)

    # Candidate 2
    ra, dec = 290.9631, 25.9039
    res = check_vizier_clusters(ra, dec)
    if not res:
        print("\nNo VizieR matches found for Candidate 2.")
    for cat, table in res.items():
        print(f"\nMatches in {cat}:")
        print(table)
