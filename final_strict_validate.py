import pandas as pd
import numpy as np

df = pd.read_csv("final_confirmed_candidates.csv")

for index, row in df.iterrows():
    name = row['simbad_name']
    print(f"Final Validation for {name}:")
    print(f"  RA, Dec: {row['ra']}, {row['dec']}")
    print(f"  Parallax: {row['parallax']:.2f} +/- {row['parallax']/row['parallax_over_error']:.2f} mas")
    print(f"  Distance: {1000/row['parallax']:.1f} pc")
    print(f"  M_G: {row['M_G']:.2f}")
    print(f"  M_J: {row['M_J']:.2f}")
    print(f"  W1-W2: {row['W1_W2']:.2f}")
    print(f"  RUWE: {row['ruwe']:.2f}")
    print(f"  Excess Noise: {row['astrometric_excess_noise']:.2f}")

    # Kinematics
    v_t = 4.74 * (row['pm'] / 1000) * (1000 / row['parallax'])
    print(f"  v_t: {v_t:.2f} km/s")

    if row['M_G'] > 14.5 and row['W1_W2'] > 0.6:
        print("  Status: PASSED STRICT UCD VALIDATION")
    else:
        print("  Status: FAILED")
    print("-" * 20)
