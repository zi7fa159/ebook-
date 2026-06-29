from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u

Simbad.add_votable_fields('otype')

def check_candidate_7():
    ra, dec = 50.0880, 59.9421
    coord = SkyCoord(ra=ra*u.deg, dec=dec*u.deg, frame='icrs')
    result = Simbad.query_region(coord, radius=40*u.arcmin)
    if result:
        print(f"Simbad results within 40' of RA={ra}, Dec={dec}:")
        for row in result:
            otype = str(row['otype'])
            name = row['main_id'].decode() if isinstance(row['main_id'], bytes) else row['main_id']
            if any(x in otype for x in ['Cl', 'Assoc', 'OpC', 'GCl', 'Cl?', 'As*', 'PaC']):
                 print(f"FOUND KNOWN OBJECT: {name} ({otype}) at {row['ra']} {row['dec']}")
    else:
        print("No Simbad results.")

if __name__ == "__main__":
    check_candidate_7()
