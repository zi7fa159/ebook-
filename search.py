import numpy as np
from astroquery.ipac.irsa import Irsa
from astroquery.imcce import Skybot
from astropy.coordinates import SkyCoord
import astropy.units as u
from astropy.table import Table, vstack
from astropy.time import Time
import time

def get_ptf_detections(ra_center, dec_center, radius_deg, mjd_start, mjd_end):
    query = f"""
    SELECT ra, dec, obsmjd, mag_auto, sid, pid, ccdid, fid
    FROM ptf_sources
    WHERE ra BETWEEN {ra_center - radius_deg} AND {ra_center + radius_deg}
      AND dec BETWEEN {dec_center - radius_deg} AND {dec_center + radius_deg}
      AND obsmjd BETWEEN {mjd_start} AND {mjd_end}
    ORDER BY obsmjd
    """
    res = Irsa.query_tap(query)
    return res.to_table()

def group_by_exposure(table, tolerance_sec=5.0):
    if len(table) == 0:
        return []
    table.sort('obsmjd')
    groups = []
    current_group = [table[0]]
    for i in range(1, len(table)):
        if (table[i]['obsmjd'] - current_group[0]['obsmjd']) * 86400.0 < tolerance_sec:
            current_group.append(table[i])
        else:
            groups.append(vstack(current_group))
            current_group = [table[i]]
    groups.append(vstack(current_group))
    return groups

def find_candidates(groups, static_tolerance_arcsec=2.0):
    if len(groups) < 2:
        return []
    static_indices = [set() for _ in range(len(groups))]
    for i in range(len(groups)):
        coords_i = SkyCoord(ra=np.array(groups[i]['ra'])*u.deg, dec=np.array(groups[i]['dec'])*u.deg)
        for j in range(i + 1, len(groups)):
            coords_j = SkyCoord(ra=np.array(groups[j]['ra'])*u.deg, dec=np.array(groups[j]['dec'])*u.deg)
            idx, d2d, _ = coords_i.match_to_catalog_sky(coords_j)
            matches = d2d < static_tolerance_arcsec * u.arcsec
            for k, is_match in enumerate(matches):
                if is_match:
                    static_indices[i].add(k)
                    static_indices[j].add(idx[k])
    candidates = []
    for i in range(len(groups)):
        mask = np.ones(len(groups[i]), dtype=bool)
        for idx in static_indices[i]:
            mask[idx] = False
        candidates.append(groups[i][mask])
    return candidates

def link_candidates(cand_groups, max_velocity_deg_per_day=0.8, prediction_tolerance_arcsec=4.0):
    if len(cand_groups) < 3:
        return []
    tracks = []
    for idx1 in range(len(cand_groups) - 2):
        for idx2 in range(idx1 + 1, len(cand_groups) - 1):
            for idx3 in range(idx2 + 1, len(cand_groups)):
                c1, c2, c3 = cand_groups[idx1], cand_groups[idx2], cand_groups[idx3]
                if not (len(c1) and len(c2) and len(c3)): continue
                dt12 = c2['obsmjd'][0] - c1['obsmjd'][0]
                dt23 = c3['obsmjd'][0] - c2['obsmjd'][0]
                if dt12 < 0.005 or dt23 < 0.005: continue # at least 7 mins apart
                for i in range(len(c1)):
                    for j in range(len(c2)):
                        v_ra = (c2['ra'][j] - c1['ra'][i]) / dt12
                        v_dec = (c2['dec'][j] - c1['dec'][i]) / dt12
                        if np.sqrt(v_ra**2 + v_dec**2) > max_velocity_deg_per_day: continue
                        pred_ra = c2['ra'][j] + v_ra * dt23
                        pred_dec = c2['dec'][j] + v_dec * dt23
                        for k in range(len(c3)):
                            dist = np.sqrt((c3['ra'][k] - pred_ra)**2 + (c3['dec'][k] - pred_dec)**2)
                            if dist < (prediction_tolerance_arcsec / 3600.0):
                                tracks.append([c1[i], c2[j], c3[k]])
    return tracks

def check_skybot(track, tolerance_arcsec=10.0):
    mid_det = track[1]
    field = SkyCoord(mid_det['ra']*u.deg, mid_det['dec']*u.deg)
    epoch = Time(mid_det['obsmjd'], format='mjd')
    try:
        res = Skybot.cone_search(field, tolerance_arcsec*u.arcsec, epoch)
        if len(res) > 0:
            return res[0]['Name']
    except:
        pass
    return None

def run_search(num_regions=50):
    import find_regions
    best = find_regions.find_best_regions()
    print(f"Starting systematic search across {num_regions} regions...")

    for b in best[:num_regions]:
        mjd_list = b['times']
        mjd_s, mjd_e = min(mjd_list) - 0.005, max(mjd_list) + 0.005
        try:
            dets = get_ptf_detections(b['ra'], b['dec'], 0.2, mjd_s, mjd_e)
            groups = group_by_exposure(dets)
            if len(groups) < 3: continue
            cand_groups = find_candidates(groups)
            tracks = link_candidates(cand_groups)

            seen_sids = set()
            for t in tracks:
                sids = tuple(sorted([det['sid'] for det in t]))
                if sids in seen_sids: continue
                seen_sids.add(sids)

                known_name = check_skybot(t)
                if known_name:
                    # print(f"Found known object: {known_name}")
                    pass
                else:
                    print(f"\n*** POSSIBLE NEW DISCOVERY! ***")
                    print(f"Region: RA={b['ra']}, Dec={b['dec']}, Night={b['night']}")
                    for det in t:
                        print(f"  MJD={det['obsmjd']:.5f} RA={det['ra']:.5f} Dec={det['dec']:.5f} Mag={det['mag_auto']:.2f}")
                    with open("discoveries.log", "a") as f:
                        f.write(f"Candidate discovery at RA={b['ra']}, Dec={b['dec']}, Night={b['night']}\n")
                        for det in t:
                            f.write(f"  MJD={det['obsmjd']:.5f} RA={det['ra']:.5f} Dec={det['dec']:.5f} Mag={det['mag_auto']:.2f}\n")
        except Exception as e:
            # print(f"Error in region RA={b['ra']}, Dec={b['dec']}: {e}")
            continue

if __name__ == "__main__":
    run_search(100)
