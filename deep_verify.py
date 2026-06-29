
from cluster_hunter import OpenStarClusterHunter
import pandas as pd
from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u
import numpy as np

hunter = OpenStarClusterHunter()

candidates = [
    {"ra": 272.2079, "dec": -8.9617, "name": "Candidate_1_l20_b5"},
    {"ra": 290.9631, "dec": 25.9039, "name": "Candidate_2_l60_b5"}
]

for cand in candidates:
    print(f"\n--- Deep Analysis: {cand['name']} ---")
    df = hunter.fetch_data(cand['ra'], cand['dec'], radius=0.3)
    if not df.empty:
        potential_clusters = hunter.find_clusters(df)
        for i, cluster in enumerate(potential_clusters):
            dist = np.sqrt((cluster['ra'].mean() - cand['ra'])**2 + (cluster['dec'].mean() - cand['dec'])**2)
            if dist > 0.1: continue

            print(f"Cluster {i}: RA={cluster['ra'].mean():.4f}, Dec={cluster['dec'].mean():.4f}, N={len(cluster)}")
            is_known, name = hunter.cross_match(cluster)
            if is_known:
                print(f"  Result: KNOWN as {name}")
            else:
                print(f"  Result: STILL UNKNOWN")
                # Sample a few stars
                brightest = cluster.sort_values('phot_g_mean_mag').head(5)
                for _, star in brightest.iterrows():
                    try:
                        c = SkyCoord(ra=star['ra']*u.deg, dec=star['dec']*u.deg)
                        res = Simbad.query_region(c, radius=3*u.arcsec)
                        if res is not None and len(res) > 0:
                            main_id = res['main_id'][0]
                            if isinstance(main_id, bytes): main_id = main_id.decode()
                            print(f"    Bright star: {main_id}")
                            syn = Simbad.query_objectids(main_id)
                            if syn is not None:
                                ids = [str(s[0]) for s in syn]
                                for id_val in ids:
                                    if any(pref in id_val for pref in ['MWSC', 'NGC', 'IC', 'Mel', 'Cr', 'Tr', 'Be', 'UBC', 'Gulliver', 'Cantat', 'Hunt']):
                                        print(f"      !!! SYNONYM ALERT: {id_val}")
                    except Exception as e:
                        print(f"    Error querying star: {e}")
