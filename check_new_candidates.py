from astroquery.simbad import Simbad
ids = ["Gaia DR3 2202703050401536000", "LEDA 1697774", "SIPS J0145-3729B", "SDSS J093929.50+495200.5", "DESI J165.3818+65.1308", "1RXS J065612.9+684122", "Gaia DR3 1741216167416222208"]
for id in ids:
    print(f"\nChecking {id}...")
    Simbad.reset_votable_fields()
    Simbad.add_votable_fields('otype', 'nbref', 'biblio')
    res = Simbad.query_object(id)
    if res:
        print(res['main_id'][0], res['otype'][0], res['nbref'][0], res['biblio'][0])
    else:
        print("Not found.")
