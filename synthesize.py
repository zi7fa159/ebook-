import pandas as pd
import numpy as np

def synthesize():
    # Load all findings
    hvs_cand = pd.read_csv('hvs_candidates.csv')
    hvs_val = pd.read_csv('hvs_validation.csv')
    ruwe_anom = pd.read_csv('ruwe_anomalies_v2.csv')

    # Merge HVS data
    # Use a small tolerance for matching source_id if they are floats
    hvs_merged = hvs_val.copy()
    hvs_merged['v_gc'] = np.nan
    for i, row in hvs_merged.iterrows():
        match = hvs_cand[np.abs(hvs_cand['source_id'] - row['source_id']) < 1000]
        if not match.empty:
            hvs_merged.at[i, 'v_gc'] = match.iloc[0]['v_gc']
            hvs_merged.at[i, 'phot_g_mean_mag'] = match.iloc[0]['phot_g_mean_mag']

    print("--- Final HVS Synthesis ---")
    print(hvs_merged.sort_values('v_gc', ascending=False).head(10))

    print("\n--- Final RUWE Anomaly Synthesis ---")
    print(ruwe_anom.sort_values('ruwe', ascending=False).head(10))

    # Identify "Novel" Candidates (not found in Simbad as per previous manual check)
    # HVS: 4337459232822884864, 4294439878351151104, 3877058564258759168, 6655834986669991936
    # RUWE: 141545595042005376, 215942885401270528, 224869030034826880, 262120101730772864, 270871187792446976, 276164988384459264

    # Save synthesis for report
    hvs_merged.to_csv('final_hvs_synthesis.csv', index=False)
    ruwe_anom.head(100).to_csv('final_ruwe_synthesis.csv', index=False)

if __name__ == "__main__":
    synthesize()
