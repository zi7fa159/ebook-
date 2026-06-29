from astroquery.gaia import Gaia
import astropy.units as u

def test_count():
    ra, dec, radius = 56.75, 24.12, 3.0
    query = f"SELECT count(*) FROM gaiadr3.gaia_source WHERE 1=CONTAINS(POINT('ICRS', ra, dec), CIRCLE('ICRS', {ra}, {dec}, {radius}))"
    job = Gaia.launch_job(query)
    print(job.get_results())

if __name__ == "__main__":
    test_count()
