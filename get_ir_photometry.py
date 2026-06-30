import pandas as pd
from astroquery.vizier import Vizier
from astropy.coordinates import SkyCoord
import astropy.units as u
import os

# Load interesting candidates
df = pd.read_csv("interesting_candidates.csv")

# Only process the first 100 to save time if needed, or process in smaller chunks
if os.path.exists("ir_results_partial.csv"):
    processed_df = pd.read_csv("ir_results_partial.csv")
    processed_ids = processed_df['source_id'].tolist()
else:
    processed_ids = []

print(f"Retrieving IR photometry. Total: {len(df)}, already processed: {len(processed_ids)}")

results = []

for index, row in df.iterrows():
    if row['source_id'] in processed_ids:
        continue

    coord = SkyCoord(ra=row['ra']*u.deg, dec=row['dec']*u.deg, frame='icrs')

    try:
        # CatWISE
        cw_res = Vizier(catalog='II/365/catwise', columns=['W1mproPM', 'W2mproPM', 'WISEA']).query_region(coord, radius=30*u.arcsec)
        # 2MASS
        tm_res = Vizier(catalog='II/246/out', columns=['Jmag', 'Hmag', 'Kmag', '2MASS']).query_region(coord, radius=30*u.arcsec)

        cw_data = {'W1': None, 'W2': None, 'WISEA': None}
        if cw_res and len(cw_res) > 0:
            best_cw = cw_res[0][0]
            cw_data = {'W1': best_cw['W1mproPM'], 'W2': best_cw['W2mproPM'], 'WISEA': str(best_cw['WISEA'])}

        tm_data = {'J': None, 'H': None, 'K': None, '2MASS': None}
        if tm_res and len(tm_res) > 0:
            best_tm = tm_res[0][0]
            tm_data = {'J': best_tm['Jmag'], 'H': best_tm['Hmag'], 'K': best_tm['Kmag'], '2MASS': str(best_tm['2MASS'])}

        combined = {'source_id': row['source_id']}
        combined.update(cw_data)
        combined.update(tm_data)
        results.append(combined)

    except Exception as e:
        results.append({'source_id': row['source_id'], 'W1': None, 'W2': None, 'WISEA': None, 'J': None, 'H': None, 'K': None, '2MASS': None})

    if (len(results)) % 20 == 0:
        print(f"Processed {len(results)} new candidates...")
        # Save progress
        partial_df = pd.DataFrame(results)
        if len(processed_ids) > 0:
            all_partial = pd.concat([processed_df, partial_df])
        else:
            all_partial = partial_df
        all_partial.to_csv("ir_results_partial.csv", index=False)

    if len(results) + len(processed_ids) >= 150: # Limit to 150 for speed
        break

if len(results) > 0:
    partial_df = pd.DataFrame(results)
    if len(processed_ids) > 0:
        final_ir_df = pd.concat([processed_df, partial_df])
    else:
        final_ir_df = partial_df
else:
    final_ir_df = processed_df

final_df = pd.merge(df, final_ir_df, on='source_id')

# Calculate colors
final_df['G_W2'] = final_df['phot_g_mean_mag'] - final_df['W2']
final_df['W1_W2'] = final_df['W1'] - final_df['W2']
final_df['J_K'] = final_df['J'] - final_df['K']
final_df['G_J'] = final_df['phot_g_mean_mag'] - final_df['J']

final_df.to_csv("candidates_with_ir.csv", index=False)

# Identify ultra-cool dwarfs: W1-W2 > 0.4 or G-W2 > 4.0
ucd_candidates = final_df[
    (final_df['W1_W2'] > 0.4) | (final_df['G_W2'] > 4.0)
]

print(f"Found {len(ucd_candidates)} UCD candidates out of {len(final_df)} processed.")
ucd_candidates.to_csv("ucd_candidates.csv", index=False)
