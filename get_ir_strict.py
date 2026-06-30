import pandas as pd
from astroquery.vizier import Vizier
from astropy.coordinates import SkyCoord
import astropy.units as u
import numpy as np

df = pd.read_csv("interesting_strict_candidates.csv")
print(f"Retrieving IR photometry for {len(df)} candidates...")

results = []
for index, row in df.iterrows():
    coord = SkyCoord(ra=row['ra']*u.deg, dec=row['dec']*u.deg, frame='icrs')
    try:
        cw_res = Vizier(catalog='II/365/catwise', columns=['W1mproPM', 'W2mproPM', 'WISEA']).query_region(coord, radius=30*u.arcsec)
        tm_res = Vizier(catalog='II/246/out', columns=['Jmag', 'Hmag', 'Kmag', '2MASS']).query_region(coord, radius=30*u.arcsec)

        cw_data = {'W1': None, 'W2': None}
        if cw_res:
            cw_data = {'W1': cw_res[0][0]['W1mproPM'], 'W2': cw_res[0][0]['W2mproPM']}

        tm_data = {'J': None, 'H': None, 'K': None}
        if tm_res:
            tm_data = {'J': tm_res[0][0]['Jmag'], 'H': tm_res[0][0]['Hmag'], 'K': tm_res[0][0]['Kmag']}

        combined = {'source_id': row['source_id']}
        combined.update(cw_data)
        combined.update(tm_data)
        results.append(combined)
    except:
        results.append({'source_id': row['source_id']})

    if len(results) % 20 == 0:
        print(f"Processed {len(results)}/{len(df)}...")
    if len(results) >= 100: break

ir_df = pd.DataFrame(results)
final_df = pd.merge(df, ir_df, on='source_id')
final_df['W1_W2'] = final_df['W1'] - final_df['W2']
final_df['dist_pc'] = 1 / (final_df['parallax'] / 1000)
final_df['M_G'] = final_df['phot_g_mean_mag'] - 5 * (np.log10(final_df['dist_pc']) - 1)
final_df['M_J'] = final_df['J'] - 5 * (np.log10(final_df['dist_pc']) - 1)

final_df.to_csv("candidates_strict_with_ir.csv", index=False)
