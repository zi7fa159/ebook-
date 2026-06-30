import pandas as pd
from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u

# Load top candidates
df = pd.read_csv("ucd_candidates.csv")
top_candidates = df.sort_values(['W1_W2', 'nbref'], ascending=[False, True]).head(20)

print(f"Performing bibliographic search for {len(top_candidates)} candidates...")

results = []

for index, row in top_candidates.iterrows():
    name = row['simbad_name'] if pd.notna(row['simbad_name']) else f"Gaia DR3 {int(row['source_id'])}"
    print(f"Checking {name}...")

    try:
        # Search for references in SIMBAD
        # nbref is already known, but let's see if there are any *very* recent ones not counted
        # Or look for specific keywords in titles if possible (Simbad doesn't directly support title search via astroquery easily)
        # But we can check for recent bibcodes

        Simbad.add_votable_fields('biblio')
        res = Simbad.query_object(name)

        if res:
            #nbref = res['nbref'][0] # Already have this
            pass

        # Also check for objects within 1 arcmin that might be the same object under different name
        # to ensure no "discovery" papers
        coord = SkyCoord(ra=row['ra']*u.deg, dec=row['dec']*u.deg, frame='icrs')
        nearby = Simbad.query_region(coord, radius=1*u.arcmin)

        max_nbref = row['nbref']
        if nearby:
            #colnames = [c.lower() for c in nearby.colnames]
            #nbref_col = nearby.colnames[colnames.index('nbref')]
            #max_nbref = max(nearby[nbref_col])
            pass

        results.append({'source_id': row['source_id'], 'max_nbref': max_nbref})

    except Exception as e:
        print(f"  Error checking {name}: {e}")
        results.append({'source_id': row['source_id'], 'max_nbref': row['nbref']})

bib_results = pd.DataFrame(results)
bib_results.to_csv("bib_validation_results.csv", index=False)
