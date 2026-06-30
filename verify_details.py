import pandas as pd
from astroquery.gaia import Gaia

def verify_candidates_details(csv_file):
    df = pd.read_csv(csv_file).sort_values('v_diff_kms')
    top_ids = list(df['source_id1'].head(10)) + list(df['source_id2'].head(10))

    query = f"""
    SELECT
        source_id, ra, dec, parallax, parallax_error, pmra, pmdec,
        phot_g_mean_mag, phot_bp_mean_mag, phot_rp_mean_mag, radial_velocity, radial_velocity_error
    FROM gaiadr3.gaia_source
    WHERE source_id IN ({','.join(map(str, map(int, top_ids)))})
    """
    job = Gaia.launch_job(query)
    results = job.get_results().to_pandas()
    results.to_csv('top_candidates_detailed.csv', index=False)
    print(results)

if __name__ == "__main__":
    verify_candidates_details('extreme_wide_candidates.csv')
