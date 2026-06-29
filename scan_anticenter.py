from astroquery.gaia import Gaia
import astropy.units as u
from astropy.coordinates import SkyCoord

def scan_galactic_anticenter():
    # RA=90, Dec=24 (roughly)
    # Galactic plane is rich here too.
    ra, dec = 90.0, 24.0
    hunter = OpenStarClusterHunter(min_cluster_size=50)
    # Scan a grid
    for dr in [-1, 0, 1]:
        for dd in [-1, 0, 1]:
            hunter.run_scan(ra=ra+dr, dec=dec+dd, radius=1.0)
    hunter.report_findings()

if __name__ == "__main__":
    from cluster_hunter import OpenStarClusterHunter
    scan_galactic_anticenter()
