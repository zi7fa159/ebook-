from astroquery.simbad import Simbad
print("Checking SIPS J0145-3729A...")
Simbad.reset_votable_fields()
Simbad.add_votable_fields('otype', 'nbref', 'biblio')
res = Simbad.query_object("SIPS J0145-3729A")
if res:
    print(res)
else:
    print("Not found.")
