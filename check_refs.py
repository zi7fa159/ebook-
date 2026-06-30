from astroquery.simbad import Simbad
print("Checking 2MASS J22011098+7417528 references...")
Simbad.reset_votable_fields()
Simbad.add_votable_fields('biblio')
res = Simbad.query_object("2MASS J22011098+7417528")
if res:
    print(res['biblio'][0])
else:
    print("No result.")
