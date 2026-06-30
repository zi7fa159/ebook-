from astroquery.gaia import Gaia

def check_hd_again():
    query = """
    SELECT source_id, ra, dec, parallax, pmra, pmdec, phot_g_mean_mag, ruwe
    FROM gaiadr3.gaia_source
    WHERE ra BETWEEN 53.0 AND 53.6
    AND dec BETWEEN 16.4 AND 16.8
    AND parallax > 15
    """
    job = Gaia.launch_job(query)
    res = job.get_results().to_pandas()
    print(res.sort_values('phot_g_mean_mag'))

if __name__ == "__main__":
    check_hd_again()
