import pandas as pd
import matplotlib.pyplot as plt
from cluster_hunter import OpenStarClusterHunter

def analyze_high_score_cand():
    hunter = OpenStarClusterHunter(min_cluster_size=100) # Increased size
    ra, dec = 91.09, 25.0252
    df = hunter.fetch_data(ra, dec, radius=1.0) # Larger field
    clusters = hunter.find_clusters(df)

    if not clusters:
        print("No clusters found.")
        return

    print(f"Found {len(clusters)} clusters.")
    cluster = max(clusters, key=len)

    fig, axs = plt.subplots(2, 2, figsize=(12, 10))
    axs[0, 0].scatter(df['ra'], df['dec'], s=1, c='gray', alpha=0.3)
    axs[0, 0].scatter(cluster['ra'], cluster['dec'], s=5, c='red')
    axs[0, 0].set_title(f'Spatial Distribution (N={len(cluster)})')
    axs[0, 1].scatter(df['pmra'], df['pmdec'], s=1, c='gray', alpha=0.3)
    axs[0, 1].scatter(cluster['pmra'], cluster['pmdec'], s=5, c='red')
    axs[0, 1].set_title('Proper Motion')
    axs[0, 1].set_xlim(cluster['pmra'].mean()-5, cluster['pmra'].mean()+5)
    axs[0, 1].set_ylim(cluster['pmdec'].mean()-5, cluster['pmdec'].mean()+5)

    axs[1, 0].scatter(df['phot_g_mean_mag'], df['parallax'], s=1, c='gray', alpha=0.3)
    axs[1, 0].scatter(cluster['phot_g_mean_mag'], cluster['parallax'], s=5, c='red')
    axs[1, 0].set_title('Parallax vs Magnitude')

    bp_rp = cluster['phot_bp_mean_mag'] - cluster['phot_rp_mean_mag']
    axs[1, 1].scatter(df['phot_bp_mean_mag'] - df['phot_rp_mean_mag'], df['phot_g_mean_mag'], s=1, c='gray', alpha=0.3)
    axs[1, 1].scatter(bp_rp, cluster['phot_g_mean_mag'], s=5, c='red')
    axs[1, 1].set_title('Color-Magnitude Diagram')
    axs[1, 1].invert_yaxis()

    plt.tight_layout()
    plt.savefig('high_score_cand_analysis.png')
    print("Analysis plot saved.")

if __name__ == "__main__":
    analyze_high_score_cand()
