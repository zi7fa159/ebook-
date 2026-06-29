import numpy as np
from astroquery.ipac.irsa import Irsa
from astropy.table import vstack
import astropy.units as u
from astropy.coordinates import SkyCoord

def search_around(ra, dec, mjd, radius_arcsec=30.0):
    query = f"""
    SELECT ra, dec, obsmjd, mag_auto, sid
    FROM ptf_sources
    WHERE ra BETWEEN {ra - radius_arcsec/3600.0} AND {ra + radius_arcsec/3600.0}
      AND dec BETWEEN {dec - radius_arcsec/3600.0} AND {dec + radius_arcsec/3600.0}
      AND obsmjd BETWEEN {mjd - 0.5} AND {mjd + 0.5}
    """
    try:
        res = Irsa.query_tap(query).to_table()
        return res
    except:
        return []

def follow_candidate(mjd_init, ra_init, dec_init, v_ra, v_dec, nights):
    print(f"Following candidate RA={ra_init}, Dec={dec_init}, MJD={mjd_init}")
    results = []
    for night_mjd in nights:
        dt = night_mjd - mjd_init
        pred_ra = ra_init + v_ra * dt
        pred_dec = dec_init + v_dec * dt
        print(f"  Night {night_mjd}: Predicted RA={pred_ra:.5f}, Dec={pred_dec:.5f}")
        found = search_around(pred_ra, pred_dec, night_mjd, radius_arcsec=300.0)
        if len(found) > 0:
            print(f"    FOUND {len(found)} detections")
            print(found)
            results.append(found)
        else:
            print(f"    Not found")
    return results

if __name__ == "__main__":
    mjd1, ra1, dec1 = 55626.21626, 187.35810, 4.59529
    mjd2, ra2, dec2 = 55626.44482, 187.34163, 4.54640
    dt = mjd2 - mjd1
    v_ra = (ra2 - ra1) / dt
    v_dec = (dec2 - dec1) / dt

    # Check other nights
    nights = [55624.4, 55625.4, 55627.4, 55628.4]
    follow_candidate(mjd1, ra1, dec1, v_ra, v_dec, nights)
