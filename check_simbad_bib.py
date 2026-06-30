from astroquery.simbad import Simbad

# Check bibliography query
print("Checking SIMBAD biblio query...")
Simbad.add_votable_fields('biblio')
result = Simbad.query_object("Vega")
print(result)
