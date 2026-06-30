from astroquery.vizier import Vizier

# Search for CatWISE2020
print("CatWISE2020:")
catalogs = Vizier.find_catalogs('CatWISE2020')
for cat in catalogs:
    print(cat)

# Query CatWISE2020 (II/365)
v = Vizier(columns=['*'])
result = v.query_constraints(catalog='II/365/catwise', TOP=1)
if result:
    print("\nCatWISE2020 columns:")
    print(result[0].colnames)

# Search for 2MASS
print("\n2MASS:")
catalogs_2mass = Vizier.find_catalogs('2MASS All-Sky Catalog of Point Sources')
for cat in catalogs_2mass:
    print(cat)

# Query 2MASS (II/246)
result_2mass = v.query_constraints(catalog='II/246/out', TOP=1)
if result_2mass:
    print("\n2MASS columns:")
    print(result_2mass[0].colnames)
