import pandas as pd
from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u

# Gaia DR3 6553614253923452800 at 346.5039166796005, -35.8471642082214
# High PM ~ 6.9 arcsec/yr.
# SIMBAD usually has bright stars like this.
result = Simbad.query_region(SkyCoord(ra=346.5039*u.deg, dec=-35.8472*u.deg, frame='icrs'), radius=2*u.arcmin)
if result:
    print(result)
