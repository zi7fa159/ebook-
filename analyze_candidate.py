import pandas as pd
import matplotlib.pyplot as plt
from cluster_hunter import OpenStarClusterHunter

def analyze_candidate():
    hunter = OpenStarClusterHunter(min_cluster_size=30)
    # Target the high-score candidate area
    ra, dec = 10.6172, 59.9035
    df = hunter.fetch_data(ra, dec, radius=0.5)
    clusters = hunter.find_clusters(df)

    if not clusters:
        print("No clusters found in targeted analysis.")
        return

    cluster = clusters[0] # Take the largest one

    fig, axs = plt.subplots(2, 2, figsize=(12, 10))

    # 1. Spatial distribution
    axs[0, 0].scatter(df['ra'], df['dec'], s=1, c='gray', alpha=0.3)
    axs[0, 0].scatter(cluster['ra'], cluster['dec'], s=5, c='red')
    axs[0, 0].set_title('Spatial Distribution')
    axs[0, 0].set_xlabel('RA (deg)')
    axs[0, 0].set_ylabel('Dec (deg)')

    # 2. Proper Motion
    axs[0, 1].scatter(df['pmra'], df['pmdec'], s=1, c='gray', alpha=0.3)
    axs[0, 1].scatter(cluster['pmra'], cluster['pmdec'], s=5, c='red')
    axs[0, 1].set_title('Proper Motion')
    axs[0, 1].set_xlabel('pmra (mas/yr)')
    axs[0, 1].set_ylabel('pmdec (mas/yr)')
    axs[0, 1].set_xlim(cluster['pmra'].mean()-5, cluster['pmra'].mean()+5)
    axs[0, 1].set_ylim(cluster['pmdec'].mean()-5, cluster['pmdec'].mean()+5)

    # 3. Parallax vs Magnitude
    axs[1, 0].scatter(df['phot_g_mean_mag'], df['parallax'], s=1, c='gray', alpha=0.3)
    axs[1, 0].scatter(cluster['phot_g_mean_mag'], cluster['parallax'], s=5, c='red')
    axs[1, 0].set_title('Parallax vs Magnitude')
    axs[1, 0].set_xlabel('G Mag')
    axs[1, 0].set_ylabel('Parallax (mas)')

    # 4. Color-Magnitude Diagram
    if 'phot_bp_mean_mag' in cluster.columns and 'phot_rp_mean_mag' in cluster.columns:
        bp_rp = cluster['phot_bp_mean_mag'] - cluster['phot_rp_mean_mag']
        axs[1, 1].scatter(df['phot_bp_mean_mag'] - df['phot_rp_mean_mag'], df['phot_g_mean_mag'], s=1, c='gray', alpha=0.3)
        axs[1, 1].scatter(bp_rp, cluster['phot_g_mean_mag'], s=5, c='red')
        axs[1, 1].set_title('Color-Magnitude Diagram')
        axs[1, 1].set_xlabel('BP - RP')
        axs[1, 1].set_ylabel('G Mag')
        axs[1, 1].invert_yaxis()

    plt.tight_layout()
    plt.savefig('candidate_analysis.png')
    print("Analysis plot saved to candidate_analysis.png")

if __name__ == "__main__":
    analyze_candidate()
