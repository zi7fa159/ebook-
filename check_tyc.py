import pandas as pd
from astroquery.simbad import Simbad

print("Querying 2MASS J22011098+7417528...")
res = Simbad.query_object("2MASS J22011098+7417528")
if res:
    print(res)
