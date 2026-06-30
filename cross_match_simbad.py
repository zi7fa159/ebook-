import pandas as pd
from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u
from astropy.time import Time

# Load Gaia candidates
df = pd.read_csv("gaia_candidates.csv")
df = df.sort_values('pm', ascending=False)

Simbad.reset_votable_fields()
Simbad.add_votable_fields('otype', 'nbref')

results = []
print(f"Starting SIMBAD cross-match for {len(df)} candidates...")

for index, row in df.iterrows():
    coord = SkyCoord(ra=row['ra']*u.deg, dec=row['dec']*u.deg, frame='icrs')
    try:
        # Increase radius to 2 arcmin to catch high PM stars even with epoch mismatch
        result_table = Simbad.query_region(coord, radius=2 * u.arcmin)
        if result_table is None or len(result_table) == 0:
            results.append({'source_id': row['source_id'], 'simbad_name': None, 'otype': None, 'nbref': 0})
        else:
            colnames = [c.lower() for c in result_table.colnames]
            name = str(result_table[result_table.colnames[colnames.index('main_id')]][0]) if 'main_id' in colnames else None
            otype = str(result_table[result_table.colnames[colnames.index('otype')]][0]) if 'otype' in colnames else None
            nbref = result_table[result_table.colnames[colnames.index('nbref')]][0] if 'nbref' in colnames else 0

            results.append({
                'source_id': row['source_id'],
                'simbad_name': name,
                'otype': otype,
                'nbref': nbref
            })
    except Exception as e:
        results.append({'source_id': row['source_id'], 'simbad_name': None, 'otype': None, 'nbref': 0})

    if (len(results)) % 200 == 0:
        print(f"Processed {len(results)}/{len(df)}...")

simbad_df = pd.DataFrame(results)
final_df = pd.merge(df, simbad_df, on='source_id')
final_df.to_csv("gaia_simbad_results.csv", index=False)

# Filter for "interesting" ones
vague_types = ['Star', 'Star*', 'IR', 'Infrared', 'Possible_var*', 'Candidate_BrownDwarf', 'HighPM*', 'Low-mass*', 'PM*', 'V*', 'Irregular_V*', 'Candidate_BD*', 'Unknown']
interesting = final_df[
    (final_df['simbad_name'].isna()) |
    ((final_df['otype'].isin(vague_types)) & (final_df['nbref'] < 10))
]

print(f"Found {len(interesting)} interesting candidates out of {len(df)}.")
interesting.to_csv("interesting_candidates.csv", index=False)
