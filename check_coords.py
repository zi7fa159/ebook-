from astroquery.gaia import Gaia

def check_region():
    query = """
    SELECT source_id, ra, dec, parallax, pmra, pmdec, phot_g_mean_mag, ruwe
    FROM gaiadr3.gaia_source
    WHERE ra BETWEEN 53.0 AND 54.0
    AND dec BETWEEN -49.0 AND -47.0
    AND parallax > 20
    """
    # Wait, let me check the coordinates from the previous output
    # Source 6: ra=53.37, dec=-48.33 (Wait, I need to check dec from the detailed csv)
    pass

if __name__ == "__main__":
    import pandas as pd
    df = pd.read_csv('top_candidates_detailed.csv')
    print(df[df['source_id'].isin([42768945033709568, 42768532716777088])][['source_id', 'ra', 'dec', 'parallax']])
