from astroquery.gaia import Gaia
import pandas as pd

# Query for objects with Absolute G mag consistent with L/T dwarfs
# M_G = G + 5 + 5 * log10(pi / 1000)
# We want M_G > 14.5
# Also pi > 10 mas for reliability and proximity

query = """
SELECT
    source_id, ra, dec, parallax, parallax_over_error, pm, pmra, pmdec,
    phot_g_mean_mag, phot_bp_mean_mag, phot_rp_mean_mag, bp_rp,
    astrometric_excess_noise, ruwe
FROM gaiadr3.gaia_source
WHERE parallax > 10
  AND parallax_over_error > 5
  AND phot_g_mean_mag + 5 + 5 * LOG10(parallax / 1000) > 14.5
  AND phot_g_mean_mag < 21
  AND ruwe < 1.4
  AND astrometric_excess_noise < 1
"""

print("Launching strict Gaia query for true UCDs...")
job = Gaia.launch_job(query)
results = job.get_results()
df = results.to_pandas()

print(f"Retrieved {len(df)} candidates.")
if len(df) > 0:
    df.to_csv("gaia_ucd_strict_candidates.csv", index=False)
    print(df.head())
