
from cluster_hunter import OpenStarClusterHunter
import pandas as pd

hunter = OpenStarClusterHunter()

candidates = [
    {"ra": 272.2079, "dec": -8.9617, "name": "Candidate_1_l20_b5"},
    {"ra": 290.9631, "dec": 25.9039, "name": "Candidate_2_l60_b5"},
    {"ra": 261.4402, "dec": -26.3226, "name": "Candidate_3_l0_b5"},
    {"ra": 281.4780, "dec": 8.2325, "name": "Candidate_4_l40_b5"}
]

for cand in candidates:
    print(f"--- Analyzing {cand['name']} ---")
    data = hunter.fetch_gaia_data(cand['ra'], cand['dec'], radius_deg=0.3)
    if data is not None:
        clusters = hunter.find_clusters(data)
        if not clusters.empty:
            for i, cluster in clusters.iterrows():
                match = hunter.cross_match(cluster['ra'], cluster['dec'])
                if match:
                    print(f"Cluster {i} matches: {match}")
                else:
                    print(f"Cluster {i} at RA={cluster['ra']:.4f}, Dec={cluster['dec']:.4f} has NO MATCH.")
        else:
            print("No clusters found in re-analysis.")
