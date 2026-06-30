from astroquery.gaia import Gaia
import pandas as pd

def search_more_anomalies():
    print("Searching for high RUWE anomalies with IR excess (using CatWise2020)...")

    # We join gaia_source with CatWise2020 using a positional join or pre-existing cross-match if available.
    # Gaia archive has 'external.gaiaedr3_distance' etc, but let's try a simpler approach if possible.
    # Actually, the best way in ADQL for cross-catalog is usually using the provided cross-match tables.
    # But I don't see a gaia-catwise crossmatch table in the list.
    # Let's try a positional join.

    query = """
    SELECT TOP 100
        g.source_id, g.ra, g.dec, g.parallax, g.ruwe, g.phot_g_mean_mag,
        c.w1mpro_pm, c.w2mpro_pm
    FROM gaiadr3.gaia_source AS g, external.catwise2020 AS c
    WHERE g.ruwe > 10.0
      AND g.parallax_over_error > 20
      AND g.phot_g_mean_mag < 15
      AND contains(point('', g.ra, g.dec), circle('', c.ra_icrs, c.de_icrs, 0.00027)) = 1
    """
    # 0.00027 degrees is ~1 arcsec

    print("Running positional join query (this might take a while)...")
    job = Gaia.launch_job(query)
    results = job.get_results()
    print(f"Found {len(results)} high RUWE anomalies with CatWise data.")
    results.to_pandas().to_csv('ruwe_catwise_anomalies.csv', index=False)

if __name__ == "__main__":
    search_more_anomalies()
