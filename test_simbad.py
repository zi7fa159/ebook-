import pandas as pd
from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u

# Test with Barnards Star
coord = SkyCoord("17h57m48.5s", "+04d41m36s", frame='icrs')
# Barnard's Star has PM ~10 arcsec/yr. In Gaia epoch (2016) it was at a different position than J2000.
# Simbad query_region uses current Simbad coordinates (mostly J2000 or ICRS)

print("Searching for Barnard's Star...")
result = Simbad.query_region(coord, radius=2 * u.arcmin)
if result:
    print(result[:5])
else:
    print("Not found.")
