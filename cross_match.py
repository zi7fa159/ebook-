from astroquery.simbad import Simbad
from astroquery.vizier import Vizier
import pandas as pd

def check_candidates(csv_file):
    df = pd.read_csv(csv_file).sort_values('v_diff_kms')

    # Take top 10 for check
    top_candidates = df.head(10)

    # Configure Simbad to include more info
    Simbad.add_votable_fields('ids')

    results = []

    for idx, row in top_candidates.iterrows():
        print(f"\nChecking pair: {int(row['source_id1'])} and {int(row['source_id2'])}")
        print(f"Separation: {row['sep_pc']:.3f} pc, V_diff: {row['v_diff_kms']:.4f} km/s")

        pair_info = {
            'sid1': int(row['source_id1']),
            'sid2': int(row['source_id2']),
            'sep_pc': row['sep_pc'],
            'v_diff': row['v_diff_kms']
        }

        for i, sid in enumerate([row['source_id1'], row['source_id2']], 1):
            result_table = Simbad.query_object(f"Gaia DR3 {int(sid)}")
            if result_table:
                # Find the MAIN_ID or equivalent
                main_id = result_table[0][0] # First row, first column is usually the ID
                print(f"Simbad ID for {int(sid)}: {main_id}")
                pair_info[f'id{i}'] = main_id
            else:
                print(f"No Simbad entry for {int(sid)}")
                pair_info[f'id{i}'] = "Unknown"

        results.append(pair_info)

        # Cross-match with El-Badry catalog using Vizier
        # El-Badry 2021 (DR3) is J/MNRAS/506/2269
        # Let's see if either star is in it.
        v = Vizier(catalog='J/MNRAS/506/2269')
        for sid in [row['source_id1'], row['source_id2']]:
            res = v.query_constraints(Source1=int(sid))
            if len(res) == 0:
                res = v.query_constraints(Source2=int(sid))

            if len(res) > 0:
                print(f"Star {int(sid)} FOUND in El-Badry wide binary catalog.")
            else:
                print(f"Star {int(sid)} NOT found in El-Badry wide binary catalog.")

    return results

if __name__ == "__main__":
    check_candidates('extreme_wide_candidates.csv')
