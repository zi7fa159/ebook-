from astroquery.simbad import Simbad
from astroquery.vizier import Vizier

# Check SIMBAD
print("Checking SIMBAD access...")
result_table = Simbad.query_object("Vega")
print(result_table)

# Check Vizier
print("\nChecking Vizier access...")
catalogs = Vizier.find_catalogs('CatWISE2020')
print(f"Found {len(catalogs)} catalogs for CatWISE2020")
