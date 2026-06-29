from astroquery.gaia import Gaia
import astropy.units as u
from astropy.coordinates import SkyCoord

def test_gaia_query():
    query = "SELECT TOP 10 ra, dec, parallax, pmra, pmdec FROM gaiadr3.gaia_source WHERE parallax > 0 AND parallax < 10"
    job = Gaia.launch_job(query)
    results = job.get_results()
    print(results)

if __name__ == "__main__":
    test_gaia_query()
