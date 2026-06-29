from cluster_hunter import OpenStarClusterHunter
import time

def pilot_scan():
    # Targeted regions in the Galactic Plane
    targets = [
        {"ra": 290.0, "dec": 10.0, "name": "Galactic Plane 1"},
        {"ra": 300.0, "dec": 30.0, "name": "Galactic Plane 2"},
        {"ra": 45.0, "dec": 60.0, "name": "Galactic Plane 3"},
        {"ra": 10.0, "dec": 60.0, "name": "Galactic Plane 4"}
    ]

    hunter = OpenStarClusterHunter(min_cluster_size=30)

    for target in targets:
        print(f"\n--- Scanning {target['name']} at RA={target['ra']}, Dec={target['dec']} ---")
        hunter.run_scan(ra=target['ra'], dec=target['dec'], radius=1.0)
        time.sleep(1) # Be nice to the server

    hunter.report_findings()

if __name__ == "__main__":
    pilot_scan()
