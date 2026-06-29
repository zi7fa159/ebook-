from cluster_hunter import OpenStarClusterHunter
import time

def scan_galactic_plane():
    # Scan along the galactic plane (roughly Dec=0 to 60 for northern hemisphere view)
    # RA roughly 0 to 360, but let's focus on a strip
    ra_range = range(0, 360, 5)
    dec = 60.0 # Galactic latitude is more complex but let's stick to Equatorial for simplicity

    hunter = OpenStarClusterHunter(min_cluster_size=40)

    for ra in ra_range:
        print(f"\n--- Scanning RA={ra}, Dec={dec} ---")
        hunter.run_scan(ra=float(ra), dec=dec, radius=1.0)
        # Stop if we find something very promising
        if any(cand['score'] > 20 for cand in hunter.candidates):
             print("High novelty candidate found! Pausing scan for detailed analysis.")
             break
        time.sleep(0.5)

    hunter.report_findings()

if __name__ == "__main__":
    scan_galactic_plane()
