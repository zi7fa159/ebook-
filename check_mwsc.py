from astroquery.simbad import Simbad
result = Simbad.query_object("[KPS2012] MWSC 0270")
print(result)
