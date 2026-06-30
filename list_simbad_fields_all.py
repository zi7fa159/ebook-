from astroquery.simbad import Simbad

fields = Simbad.list_votable_fields()
for field in fields:
    print(field)
