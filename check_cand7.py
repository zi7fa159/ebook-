from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u

def check_candidate_7():
    ra, dec = 50.0880, 59.9421
    coord = SkyCoord(ra=ra*u.deg, dec=dec*u.deg, frame='icrs')
    result = Simbad.query_region(coord, radius=20*u.arcmin)
    if result:
        print(f"Simbad results within 20' of RA={ra}, Dec={dec}:")
        for row in result:
            name = row['main_id'].decode() if isinstance(row['main_id'], bytes) else row['main_id']
            print(name)
    else:
        print("No Simbad results.")

if __name__ == "__main__":
    check_candidate_7()
