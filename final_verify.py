import pandas as pd
import numpy as np

def final_verification(candidates_csv, detailed_csv):
    cand = pd.read_csv(candidates_csv)
    det = pd.read_csv(detailed_csv)

    # Create a mapping for quick lookup
    det_map = det.set_index('source_id').to_dict('index')

    final_list = []
    for _, row in cand.sort_values('v_diff_kms').iterrows():
        id1, id2 = int(row['source_id1']), int(row['source_id2'])
        if id1 in det_map and id2 in det_map:
            s1 = det_map[id1]
            s2 = det_map[id2]

            rv1 = s1.get('radial_velocity')
            rv2 = s2.get('radial_velocity')

            rv_consistent = "N/A"
            if pd.notnull(rv1) and pd.notnull(rv2):
                rv_diff = abs(rv1 - rv2)
                rv_err = np.sqrt(s1['radial_velocity_error']**2 + s2['radial_velocity_error']**2)
                rv_consistent = rv_diff < 3 * max(rv_err, 2.0) # 2km/s floor for RV errors

            final_list.append({
                'id1': id1, 'id2': id2,
                'sep_pc': row['sep_pc'],
                'v_diff_kms': row['v_diff_kms'],
                'rv_consistent': rv_consistent,
                'mag1': s1['phot_g_mean_mag'],
                'mag2': s2['phot_g_mean_mag']
            })

    final_df = pd.DataFrame(final_list)
    print(final_df)
    final_df.to_csv('final_verified_candidates.csv', index=False)

if __name__ == "__main__":
    final_verification('extreme_wide_candidates.csv', 'top_candidates_detailed.csv')
