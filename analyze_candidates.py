import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from cluster_hunter import OpenStarClusterHunter
from astropy.coordinates import SkyCoord
import astropy.units as u

def diagnostic_plot(cluster_df, ra_center, dec_center, title, filename):
    fig, axs = plt.subplots(2, 2, figsize=(15, 12))

    # 1. Spatial Distribution
    axs[0, 0].scatter(cluster_df['ra'], cluster_df['dec'], s=10, color='red', label='Members')
    axs[0, 0].set_title(f"Spatial Distribution (N={len(cluster_df)})")
    axs[0, 0].set_xlabel("RA")
    axs[0, 0].set_ylabel("Dec")
    axs[0, 0].invert_xaxis()
    axs[0, 0].legend()

    # 2. Proper Motion
    axs[0, 1].scatter(cluster_df['pmra'], cluster_df['pmdec'], s=10, color='red')
    axs[0, 1].set_title("Proper Motion")
    axs[0, 1].set_xlabel("pmra (mas/yr)")
    axs[0, 1].set_ylabel("pmdec (mas/yr)")
    pm_center_ra = cluster_df['pmra'].mean()
    pm_center_dec = cluster_df['pmdec'].mean()
    axs[0, 1].set_xlim(pm_center_ra - 5, pm_center_ra + 5)
    axs[0, 1].set_ylim(pm_center_dec - 5, pm_center_dec + 5)

    # 3. Parallax vs Magnitude
    axs[1, 0].scatter(cluster_df['phot_g_mean_mag'], cluster_df['parallax'], s=10, color='red')
    axs[1, 0].set_title("Parallax vs Magnitude")
    axs[1, 0].set_xlabel("G Mag")
    axs[1, 0].set_ylabel("Parallax (mas)")

    # 4. Color-Magnitude Diagram
    bp_rp = cluster_df['phot_bp_mean_mag'] - cluster_df['phot_rp_mean_mag']
    axs[1, 1].scatter(bp_rp, cluster_df['phot_g_mean_mag'], s=10, color='red')
    axs[1, 1].set_title("Color-Magnitude Diagram")
    axs[1, 1].set_xlabel("BP - RP")
    axs[1, 1].set_ylabel("G Mag")
    axs[1, 1].invert_yaxis()

    plt.suptitle(title)
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig(filename)
    print(f"Saved diagnostic plot to {filename}")

def analyze_best_candidates():
    hunter = OpenStarClusterHunter(min_cluster_size=15)

    # Based on previous scan output:
    # Candidate at RA=272.2079, Dec=-8.9617, Score=2.30
    # Candidate at RA=290.9631, Dec=25.9039, Score=1.94

    cands_to_check = [
        {'ra': 272.2079, 'dec': -8.9617, 'name': 'Candidate_1_l20_b5'},
        {'ra': 290.9631, 'dec': 25.9039, 'name': 'Candidate_2_l60_b5'}
    ]

    for cand in cands_to_check:
        print(f"\n--- Deep Analysis of {cand['name']} ---")
        df = hunter.fetch_data(cand['ra'], cand['dec'], radius=0.5)
        if not df.empty:
            potential_clusters = hunter.find_clusters(df)
            # Find the cluster closest to our candidate center
            best_cluster = None
            min_dist = 999
            for cluster in potential_clusters:
                dist = np.sqrt((cluster['ra'].mean() - cand['ra'])**2 + (cluster['dec'].mean() - cand['dec'])**2)
                if dist < min_dist:
                    min_dist = dist
                    best_cluster = cluster

            if best_cluster is not None:
                is_known, info = hunter.cross_match(best_cluster)
                title = f"{cand['name']} | RA={best_cluster['ra'].mean():.4f}, Dec={best_cluster['dec'].mean():.4f}"
                if is_known:
                    title += f" (Matched: {info})"
                    print(f"Candidate {cand['name']} matched to {info}")
                else:
                    title += " (NO KNOWN MATCH)"
                    print(f"Candidate {cand['name']} STILL UNCATALOGUED!")

                diagnostic_plot(best_cluster, cand['ra'], cand['dec'], title, f"{cand['name']}_diag.png")

if __name__ == "__main__":
    analyze_best_candidates()
