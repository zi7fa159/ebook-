from astroquery.gaia import Gaia

# Query a small sample to see column names
job = Gaia.launch_job("SELECT TOP 1 * FROM gaiadr3.gaia_source")
r = job.get_results()
print(r.colnames)
