from astroquery.gaia import Gaia
import pandas as pd

def check_hd_hierarchy():
    # ID1 = 42768945033709568 (B?)
    # ID2 = 42768532716777088 (C?)
    # Let's find the main HD 21962 (A)
    query = """
    SELECT source_id, ra, dec, parallax, pmra, pmdec, phot_g_mean_mag, ruwe
    FROM gaiadr3.gaia_source
    WHERE ra BETWEEN 53.0 AND 53.6
    AND dec BETWEEN -44.2 AND -43.8
    AND parallax > 20
    """
    job = Gaia.launch_job(query)
    res = job.get_results().to_pandas()
    print(res.sort_values('phot_g_mean_mag'))

if __name__ == "__main__":
    check_hd_hierarchy()
