import pandas as pd
from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u
from astropy.time import Time

# Load Gaia candidates
df = pd.read_csv("gaia_candidates.csv")
# Take top 100 for a quick deep check
df = df.sort_values('pm', ascending=False).head(100)

Simbad.add_votable_fields('otype', 'nbref')

gaia_epoch = Time(2016.0, format='jyear')

print("Starting deep SIMBAD test with different radii and no correction...")

for index, row in df.iterrows():
    c_gaia = SkyCoord(ra=row['ra']*u.deg, dec=row['dec']*u.deg, frame='icrs')

    print(f"Candidate {row['source_id']} at {row['ra']}, {row['dec']}")
    # Try 1 arcmin radius
    result = Simbad.query_region(c_gaia, radius=1 * u.arcmin)
    if result:
        print(f"  Found {len(result)} matches within 1 arcmin.")
        print(f"  Top match: {result['main_id'][0]} - {result['main_type'][0]}")
    else:
        print("  No matches within 1 arcmin.")

    if index >= 4: # Just check first 5
        break
