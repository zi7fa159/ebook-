
from cluster_hunter import OpenStarClusterHunter
import pandas as pd
import numpy as np

hunter = OpenStarClusterHunter()
ra, dec = 272.2079, -8.9617
df = hunter.fetch_data(ra, dec, radius=0.3)
clusters = hunter.find_clusters(df)

for i, cluster in enumerate(clusters):
    dist = np.sqrt((cluster['ra'].mean() - ra)**2 + (cluster['dec'].mean() - dec)**2)
    if dist < 0.1:
        print(f"\n--- FINAL CLUSTER ANALYSIS ---")
        print(f"Candidate ID: J272.22-08.93")
        print(f"Coordinates: RA={cluster['ra'].mean():.5f}, Dec={cluster['dec'].mean():.5f}")
        print(f"Galactic: l=20.0, b=5.0 (approx)")
        print(f"Member count: {len(cluster)}")
        print(f"Proper Motion: pmra={cluster['pmra'].mean():.3f} +/- {cluster['pmra'].std():.3f}, pmdec={cluster['pmdec'].mean():.3f} +/- {cluster['pmdec'].std():.3f}")
        print(f"Parallax: {cluster['parallax'].mean():.3f} +/- {cluster['parallax'].std():.3f} mas")
        print(f"Distance approx: {1000/cluster['parallax'].mean():.1f} pc")

        # Check Simbad one last time
        is_known, name = hunter.cross_match(cluster)
        print(f"Simbad Status: {'KNOWN' if is_known else 'UNCATALOGUED'}")
        if name: print(f"Name/ID: {name}")
