import pandas as pd
import lightkurve as lk
import matplotlib.pyplot as plt
import os
from astropy.coordinates import SkyCoord
import astropy.units as u

# Load UCD candidates
df = pd.read_csv("ucd_candidates.csv")

# Select top candidates
top_candidates = df.sort_values(['W1_W2', 'nbref'], ascending=[False, True]).head(15)

print(f"Checking TESS light curves for {len(top_candidates)} candidates...")

if not os.path.exists("plots"):
    os.makedirs("plots")

results = []

for index, row in top_candidates.iterrows():
    name = row['simbad_name'] if pd.notna(row['simbad_name']) else f"GaiaDR3_{int(row['source_id'])}"
    print(f"Processing {name}...")

    try:
        # Search by coordinates always to avoid TIC resolve issues
        coord = f"{row['ra']} {row['dec']}"
        search_result = lk.search_lightcurve(coord, mission='TESS', radius=30*u.arcsec)

        if len(search_result) > 0:
            print(f"  Found {len(search_result)} datasets.")
            # Limit to download first 2 sectors to save time
            lc_collection = search_result[:2].download_all()
            if lc_collection:
                lc = lc_collection.stitch().remove_nans().flatten()

                # Plot and save
                plt.figure(figsize=(10, 4))
                lc.scatter()
                plt.title(f"TESS LC: {name}")
                plt.savefig(f"plots/lc_{name.replace(' ', '_')}.png")
                plt.close()

                # Check for variability
                std = lc.flux.std().value
                results.append({'source_id': row['source_id'], 'has_tess': True, 'lc_std': std})
            else:
                results.append({'source_id': row['source_id'], 'has_tess': False})
        else:
            print(f"  No TESS light curves found for {name}.")
            results.append({'source_id': row['source_id'], 'has_tess': False})

    except Exception as e:
        print(f"  Error processing {name}: {e}")
        results.append({'source_id': row['source_id'], 'has_tess': False, 'error': str(e)})

tess_results = pd.DataFrame(results)
tess_results.to_csv("tess_validation_results.csv", index=False)
