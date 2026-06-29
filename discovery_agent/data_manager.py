import lightkurve as lk
from astropy.coordinates import SkyCoord
import astropy.units as u
import numpy as np

class DataManager:
    def __init__(self, cutout_size=20):
        self.cutout_size = cutout_size
        # Interesting regions: Galactic Center, Southern CVZ, Orion, Pleiades
        self.interesting_coords = [
            SkyCoord(ra=266.4, dec=-29.0, unit=(u.deg, u.deg)), # GC
            SkyCoord(ra=280.0, dec=-60.0, unit=(u.deg, u.deg)), # CVZ-S
            SkyCoord(ra=83.8, dec=-5.4, unit=(u.deg, u.deg)),   # Orion
            SkyCoord(ra=56.7, dec=24.1, unit=(u.deg, u.deg)),   # Pleiades
        ]

    def select_target(self):
        # Mix of random and interesting targets
        if np.random.random() < 0.3:
            idx = np.random.randint(len(self.interesting_coords))
            return self.interesting_coords[idx]
        else:
            ra = np.random.uniform(0, 360)
            dec = np.random.uniform(-90, 90)
            return SkyCoord(ra=ra, dec=dec, unit=(u.deg, u.deg))

    def download_data(self, coord):
        print(f"Searching for TESS data around {coord.to_string('hmsdms')}...")
        try:
            search_result = lk.search_tesscut(coord)
            if len(search_result) == 0:
                return None

            # Prefer sectors with longer baseline or specific ones if needed
            tpf = search_result[0].download(cutout_size=self.cutout_size)
            return tpf
        except Exception as e:
            print(f"Error downloading data: {e}")
            return None
