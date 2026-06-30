import pandas as pd
from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u

df = pd.read_csv("gaia_ucd_strict_candidates.csv")
# Take top 500 by parallax for best reliability
df = df.sort_values('parallax', ascending=False).head(500)

Simbad.reset_votable_fields()
Simbad.add_votable_fields('otype', 'nbref')

results = []
print(f"Cross-matching {len(df)} strict candidates...")

for index, row in df.iterrows():
    coord = SkyCoord(ra=row['ra']*u.deg, dec=row['dec']*u.deg, frame='icrs')
    try:
        # High parallax means nearby, so epoch mismatch is less of an issue than high PM
        # but let's use 1 arcmin to be safe
        result_table = Simbad.query_region(coord, radius=1 * u.arcmin)
        if result_table is None or len(result_table) == 0:
            results.append({'source_id': row['source_id'], 'simbad_name': None, 'otype': None, 'nbref': 0})
        else:
            colnames = [c.lower() for c in result_table.colnames]
            name = str(result_table[result_table.colnames[colnames.index('main_id')]][0])
            otype = str(result_table[result_table.colnames[colnames.index('otype')]][0])
            nbref = result_table[result_table.colnames[colnames.index('nbref')]][0]

            results.append({
                'source_id': row['source_id'],
                'simbad_name': name,
                'otype': otype,
                'nbref': nbref
            })
    except Exception as e:
        results.append({'source_id': row['source_id'], 'simbad_name': None, 'otype': None, 'nbref': 0})

    if (len(results)) % 100 == 0:
        print(f"Processed {len(results)}/{len(df)}...")

simbad_df = pd.DataFrame(results)
final_df = pd.merge(df, simbad_df, on='source_id')

# We want objects with LOW nbref and not already classified as L/T dwarfs
# (Though finding a known one is a good verification)
interesting = final_df[final_df['nbref'] < 5]

print(f"Found {len(interesting)} interesting new candidates.")
interesting.to_csv("interesting_strict_candidates.csv", index=False)
