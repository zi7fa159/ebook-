from astroquery.gaia import Gaia
import pandas as pd

def search_more_anomalies():
    print("Searching for high RUWE anomalies (no join)...")

    query = """
    SELECT TOP 1000
        source_id, ra, dec, parallax, ruwe, phot_g_mean_mag, pmra, pmdec
    FROM gaiadr3.gaia_source
    WHERE ruwe > 10.0
      AND parallax_over_error > 20
      AND phot_g_mean_mag < 15
    """

    job = Gaia.launch_job(query)
    results = job.get_results()
    print(f"Found {len(results)} high RUWE anomalies.")
    df = results.to_pandas()
    df.to_csv('ruwe_anomalies_v2.csv', index=False)

if __name__ == "__main__":
    search_more_anomalies()
