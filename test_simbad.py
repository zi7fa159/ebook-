from astroquery.simbad import Simbad
import astropy.units as u
from astropy.coordinates import SkyCoord

def test_simbad():
    coord = SkyCoord(ra=56.75*u.deg, dec=24.12*u.deg, frame='icrs')
    result = Simbad.query_region(coord, radius=1*u.deg)
    print(result.colnames)
    print(result[0])

if __name__ == "__main__":
    test_simbad()
