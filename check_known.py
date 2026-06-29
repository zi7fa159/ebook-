from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u

def check_known():
    coord = SkyCoord(ra=11.0162*u.deg, dec=60.1657*u.deg, frame='icrs')
    # Use a larger radius and search for clusters
    result = Simbad.query_region(coord, radius=30*u.arcmin)
    if result:
        # Filter for cluster-like names
        for row in result:
            name = row['main_id'].decode() if isinstance(row['main_id'], bytes) else row['main_id']
            print(name)
    else:
        print("No results.")

if __name__ == "__main__":
    check_known()
