import pandas as pd
import numpy as np

def filter_extremes(csv_file):
    df = pd.read_csv(csv_file)

    # Filter for separation > 0.1 pc to focus on wide systems
    # And extremely similar proper motions

    # Convert pm_diff to tangential velocity difference at average distance
    # 1 mas/yr * d_pc / 1000 * 4.7405 = v_tan (km/s)
    avg_plx = (df['parallax1'] + df['parallax2']) / 2
    avg_dist = 1000 / avg_plx
    df['v_diff_kms'] = df['pm_diff'] * avg_dist / 1000 * 4.7405

    # Filter for very wide (>0.5 pc) and very bound-looking (v_diff < 0.5 km/s)
    extremes = df[(df['sep_pc'] > 0.5) & (df['v_diff_kms'] < 0.5)]

    extremes.to_csv('extreme_wide_candidates.csv', index=False)
    print(f"Found {len(extremes)} extremely wide binary candidates (>0.5 pc).")

    # Sort by v_diff to see the most promising ones
    print(extremes[['source_id1', 'source_id2', 'sep_pc', 'v_diff_kms']].sort_values('v_diff_kms').head(10))

if __name__ == "__main__":
    filter_extremes('candidate_binaries.csv')
