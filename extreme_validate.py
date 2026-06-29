import json
import numpy as np
from astroquery.imcce import Skybot
from astroquery.vizier import Vizier
from astropy.coordinates import SkyCoord
from astropy.time import Time
import astropy.units as u
import time

def extreme_validate(candidates_file):
    with open(candidates_file, 'r') as f:
        candidates = json.load(f)

    extreme_results = []
    print(f"Starting extreme validation of {len(candidates)} candidates...")

    for i, dets in enumerate(candidates):
        print(f"Validating Candidate {i+1}...")

        mjd1, ra1, dec1 = dets[0]['mjd'], dets[0]['ra'], dets[0]['dec']
        mjd_last, ra_last, dec_last = dets[-1]['mjd'], dets[-1]['ra'], dets[-1]['dec']
        dt_total = (mjd_last - mjd1) * 24.0

        c1 = SkyCoord(ra1*u.deg, dec1*u.deg)
        clast = SkyCoord(ra_last*u.deg, dec_last*u.deg)
        total_dist_arcsec = c1.separation(clast).arcsec
        rate_arcsec_hr = total_dist_arcsec / dt_total if dt_total > 0 else 0

        mid_idx = len(dets) // 2
        mid_det = dets[mid_idx]
        dt_frac = (mid_det['mjd'] - mjd1) / (mjd_last - mjd1) if (mjd_last - mjd1) > 0 else 0
        pred_ra = ra1 + (ra_last - ra1) * dt_frac
        pred_dec = dec1 + (dec_last - dec1) * dt_frac
        deviation_arcsec = np.sqrt((mid_det['ra'] - pred_ra)**2 + (mid_det['dec'] - pred_dec)**2) * 3600.0

        skybot_matches = []
        for det in dets:
            field = SkyCoord(det['ra']*u.deg, det['dec']*u.deg)
            epoch = Time(det['mjd'], format='mjd')
            try:
                res = Skybot.cone_search(field, 2.0*u.arcmin, epoch)
                if len(res) > 0:
                    for row in res:
                        d = field.separation(SkyCoord(row['RA'], row['DEC'])).arcsec
                        if d < 60.0:
                            skybot_matches.append({'name': row['Name'], 'dist': d, 'mjd': det['mjd']})
            except:
                pass
            time.sleep(0.05)

        gaia_matches = []
        for det in dets:
            field = SkyCoord(det['ra']*u.deg, det['dec']*u.deg)
            try:
                v = Vizier(columns=['*'], catalog='I/355/gaiadr3')
                res = v.query_region(field, radius=5*u.arcsec)
                if len(res) > 0:
                    gaia_matches.append({'count': len(res[0]), 'mjd': det['mjd']})
            except:
                pass
            time.sleep(0.05)

        is_unknown = (len(skybot_matches) == 0) and (len(gaia_matches) == 0)

        extreme_results.append({
            'id': i + 1,
            'detections': dets,
            'rate_arcsec_hr': rate_arcsec_hr,
            'linearity_deviation_arcsec': deviation_arcsec,
            'skybot_matches': skybot_matches,
            'gaia_matches': gaia_matches,
            'is_unknown': is_unknown
        })

        if is_unknown:
            print(f"  VERDICT: EXTREME CONFIRMATION - Candidate {i+1} is UNDOCUMENTED.")
        else:
            print(f"  VERDICT: Candidate {i+1} Rejected (Skybot: {len(skybot_matches)}, Gaia: {len(gaia_matches)})")

    return extreme_results

if __name__ == "__main__":
    results = extreme_validate("parsed_candidates.json")
    with open("extreme_validation_results.json", "w") as f:
        json.dump(results, f, indent=2)
    print("Extreme validation complete.")
