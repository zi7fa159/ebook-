from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u

Simbad.add_votable_fields('otype')

def check_candidate_2():
    ra, dec = 10.6172, 59.9035
    coord = SkyCoord(ra=ra*u.deg, dec=dec*u.deg, frame='icrs')
    result = Simbad.query_region(coord, radius=30*u.arcmin)
    if result:
        print(f"Simbad results within 30' of RA={ra}, Dec={dec}:")
        print(result.colnames)
        for row in result:
            otype = str(row['otype'])
            name = row['main_id'].decode() if isinstance(row['main_id'], bytes) else row['main_id']
            if any(x in otype for x in ['Cl', 'Assoc', 'OpC', 'GCl', 'Cl?', 'As*', 'PaC']):
                 print(f"FOUND KNOWN OBJECT: {name} ({otype}) at {row['ra']} {row['dec']}")
    else:
        print("No Simbad results.")

if __name__ == "__main__":
    check_candidate_2()
