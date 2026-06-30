import pandas as pd
import numpy as np

# Load current discovery candidates
df = pd.read_csv("final_discovery_candidates.csv")

print("Strict validation of current candidates...")

# STEP 2 & 3: Distance and Absolute Magnitude
df['dist_pc'] = 1 / (df['parallax'] / 1000)
df['M_G'] = df['phot_g_mean_mag'] - 5 * np.log10(df['dist_pc']) + 5
df['M_J'] = df['J'] - 5 * np.log10(df['dist_pc']) + 5

# STEP 5: Tangential Velocity
df['v_t'] = 4.74 * (df['pm'] / 1000) * df['dist_pc']

# UCD Thresholds (approximate)
# L dwarfs typically have M_G > 14.5
# M dwarfs are roughly 8 < M_G < 14.5

results = []
for index, row in df.iterrows():
    name = row['simbad_name'] if pd.notna(row['simbad_name']) else f"Gaia DR3 {int(row['source_id'])}"
    print(f"\nValidating {name}:")
    print(f"  M_G = {row['M_G']:.2f}")
    print(f"  M_J = {row['M_J']:.2f}")
    print(f"  v_t = {row['v_t']:.2f} km/s")
    print(f"  W1-W2 = {row['W1_W2']:.2f}")
    print(f"  RUWE = {row['ruwe']:.2f}")

    classification = "Unknown"
    if row['M_G'] < 14.0:
        classification = "Likely M dwarf (reject UCD interpretation)"
    elif row['M_G'] >= 14.5:
        classification = "Confirmed Ultracool Dwarf Candidate"
    else:
        classification = "Possible transition M/L dwarf"

    print(f"  Result: {classification}")
    results.append({'source_id': row['source_id'], 'classification': classification, 'M_G': row['M_G'], 'v_t': row['v_t']})

results_df = pd.DataFrame(results)
results_df.to_csv("strict_validation_results.csv", index=False)
