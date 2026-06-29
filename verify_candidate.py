from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u

def verify_candidate():
    # Candidate 2: RA=10.6172, Dec=59.9035
    coord = SkyCoord(ra=10.6172*u.deg, dec=59.9035*u.deg, frame='icrs')
    result = Simbad.query_region(coord, radius=10*u.arcmin)
    if result:
        print(result)
    else:
        print("No Simbad results.")

if __name__ == "__main__":
    verify_candidate()
