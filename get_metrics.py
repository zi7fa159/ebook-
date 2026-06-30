from astroquery.gaia import Gaia
import pandas as pd

def get_final_metrics():
    ids = [
        42768945033709568, 42768532716777088, # HD 21962 B/C
        1466872554405096192, 1466873344679078912, # LSPM
        49131681384221184, 49131681384221952, # LP 415-19
        39163474607448192, 38929141191925760 # LP 474
    ]
    query = f"""
    SELECT source_id, ruwe, astrometric_excess_noise, duplicated_source
    FROM gaiadr3.gaia_source
    WHERE source_id IN ({','.join(map(str, ids))})
    """
    job = Gaia.launch_job(query)
    res = job.get_results().to_pandas()
    print(res)

if __name__ == "__main__":
    get_final_metrics()
