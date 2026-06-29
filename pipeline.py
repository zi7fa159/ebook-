import numpy as np
from astroquery.ipac.irsa import Irsa
from astroquery.mpc import MPC
from astropy.coordinates import SkyCoord
import astropy.units as u
from astropy.table import Table, vstack
from astropy.time import Time
import time
import os

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
        ra_i = np.array(groups[i]['ra']) * u.deg
        dec_i = np.array(groups[i]['dec']) * u.deg
        coords_i = SkyCoord(ra=ra_i, dec=dec_i)
        for j in range(i + 1, len(groups)):
            ra_j = np.array(groups[j]['ra']) * u.deg
            dec_j = np.array(groups[j]['dec']) * u.deg
            coords_j = SkyCoord(ra=ra_j, dec=dec_j)
            idx, d2d, d3d = coords_i.match_to_catalog_sky(coords_j)
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

def link_candidates(cand_groups, max_velocity_deg_per_day=0.5, prediction_tolerance_arcsec=3.0):
    # Search for linear motion among all combinations of candidates across all groups
    # This is more robust than just looking at the first 3 exposures
    if len(cand_groups) < 3:
        return []

    tracks = []
    # Simplified version: try all triplets
    for idx1 in range(len(cand_groups) - 2):
        for idx2 in range(idx1 + 1, len(cand_groups) - 1):
            for idx3 in range(idx2 + 1, len(cand_groups)):
                c1 = cand_groups[idx1]
                c2 = cand_groups[idx2]
                c3 = cand_groups[idx3]
                if len(c1) == 0 or len(c2) == 0 or len(c3) == 0:
                    continue

                dt12 = (c2['obsmjd'][0] - c1['obsmjd'][0])
                dt23 = (c3['obsmjd'][0] - c2['obsmjd'][0])

                for i in range(len(c1)):
                    for j in range(len(c2)):
                        v_ra = (c2['ra'][j] - c1['ra'][i]) / dt12
                        v_dec = (c2['dec'][j] - c1['dec'][i]) / dt12

                        vel = np.sqrt(v_ra**2 + v_dec**2)
                        if vel > max_velocity_deg_per_day or vel < 0.01:
                            continue

                        pred_ra = c2['ra'][j] + v_ra * dt23
                        pred_dec = c2['dec'][j] + v_dec * dt23

                        for k in range(len(c3)):
                            dist = np.sqrt((c3['ra'][k] - pred_ra)**2 + (c3['dec'][k] - pred_dec)**2)
                            if dist < (prediction_tolerance_arcsec / 3600.0):
                                # Check if this track is already found (duplicate)
                                tracks.append([c1[i], c2[j], c3[k]])
    return tracks

def run_iteration(ra, dec, mjd_list, radius=0.15):
    mjd_s = min(mjd_list) - 0.005
    mjd_e = max(mjd_list) + 0.005

    log = f"RA={ra}, Dec={dec}, MJD Range={mjd_s:.3f} - {mjd_e:.3f}\n"
    try:
        dets = get_ptf_detections(ra, dec, radius, mjd_s, mjd_e)
    except Exception as e:
        return f"Query failed: {e}"

    if len(dets) == 0:
        return "No detections found."

    groups = group_by_exposure(dets)
    if len(groups) < 3:
        return f"Only {len(groups)} exposures found."

    cand_groups = find_candidates(groups)
    tracks = link_candidates(cand_groups)

    log += f"Found {len(tracks)} tracks.\n"
    # Deduplicate tracks (very rough)
    unique_tracks = []
    seen_sids = set()
    for t in tracks:
        sids = tuple(sorted([det['sid'] for det in t]))
        if sids not in seen_sids:
            unique_tracks.append(t)
            seen_sids.add(sids)

    log += f"Unique tracks: {len(unique_tracks)}\n"
    for t in unique_tracks:
        log += "Track:\n"
        for det in t:
            log += f"  MJD={det['obsmjd']:.5f} RA={det['ra']:.5f} Dec={det['dec']:.5f} Mag={det['mag_auto']:.2f} SID={det['sid']}\n"

    return log

if __name__ == "__main__":
    import find_regions
    best = find_regions.find_best_regions()

    # Run on top 5 eligible region-nights
    for b in best[:10]:
        print(f"Processing RA={b['ra']}, Dec={b['dec']}, Night={b['night']}...")
        result = run_iteration(b['ra'], b['dec'], b['times'])
        print(result)
        with open("research_log.md", "a") as f:
            f.write(f"\n## Region-Night: RA={b['ra']}, Dec={b['dec']}, Night={b['night']}\n")
            f.write(result)
            f.write("\n")
