import pandas as pd
from astroquery.gaia import Gaia

def query_hpm_candidates():
    print("Querying Gaia DR3 for faint high-proper-motion candidates...")

    # We increase the G magnitude limit and use a different sample
    # Sorting by G magnitude might find more overlooked objects than sorting by PM
    query = """
    SELECT TOP 500
        source_id, ra, dec, parallax, parallax_error,
        pm, pmra, pmdec, phot_g_mean_mag, phot_bp_mean_mag, phot_rp_mean_mag,
        bp_rp, g_rp, ruwe, l, b
    FROM gaiadr3.gaia_source
    WHERE pm > 200
      AND phot_g_mean_mag > 18
      AND ruwe < 1.4
      AND abs(b) > 30
      AND parallax_over_error > 5
    ORDER BY phot_g_mean_mag DESC
    """

    job = Gaia.launch_job(query)
    results = job.get_results()

    df = results.to_pandas()
    print(f"Found {len(df)} candidates.")

    df.to_csv('gaia_candidates.csv', index=False)
    print("Results saved to gaia_candidates.csv")

if __name__ == "__main__":
    query_hpm_candidates()
