import numpy as np
from astroquery.gaia import Gaia
import pandas as pd

def query_candidates():
    # Increase row limit for synchronous queries or use asynchronous
    Gaia.ROW_LIMIT = -1

    # Let's search for pairs within 50pc.
    # To find wide binaries, we need to cross-match the catalog with itself.
    # However, for 0.5pc separation at 50pc, that's 0.01 radians ~ 0.5 degrees.
    # We can do this more efficiently by querying for all stars and then doing a spatial search.

    query = """
    SELECT
        source_id, ra, dec, parallax, pmra, pmdec,
        phot_g_mean_mag, bp_rp,
        parallax_error, pmra_error, pmdec_error
    FROM gaiadr3.gaia_source
    WHERE parallax > 20
    AND parallax_over_error > 20
    AND phot_g_mean_mag < 18
    """
    # Use asynchronous job for potentially larger results
    job = Gaia.launch_job_async(query)
    results = job.get_results()
    df = results.to_pandas()
    df.to_csv('nearby_stars.csv', index=False)
    print(f"Retrieved {len(df)} stars.")

if __name__ == "__main__":
    query_candidates()
