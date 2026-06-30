from astroquery.gaia import Gaia
import pandas as pd

query = """
SELECT
    source_id, ra, dec, parallax, parallax_over_error, pm, pmra, pmdec,
    phot_g_mean_mag, phot_bp_mean_mag, phot_rp_mean_mag, bp_rp,
    astrometric_excess_noise, ruwe
FROM gaiadr3.gaia_source
WHERE pm > 500
  AND phot_g_mean_mag < 20
  AND astrometric_excess_noise < 1
  AND parallax_over_error > 5
"""

print("Launching Gaia query...")
job = Gaia.launch_job(query)
results = job.get_results()
df = results.to_pandas()

print(f"Retrieved {len(df)} candidates.")
if len(df) > 0:
    print("First 5 candidates:")
    print(df.head())
    df.to_csv("gaia_candidates.csv", index=False)
else:
    print("No candidates found with these constraints.")
