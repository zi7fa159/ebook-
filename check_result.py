import pandas as pd
from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u

# Try a larger radius for the brightest candidate
coord = SkyCoord(ra=346.5039166796005*u.deg, dec=-35.8471642082214*u.deg, frame='icrs')
result = Simbad.query_region(coord, radius=10*u.arcmin)
if result:
    print(f"Found {len(result)} matches within 10 arcmin.")
    print(result[:5])
