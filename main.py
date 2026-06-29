import time
import os
import sys
from discovery_agent.data_manager import DataManager
from discovery_agent.analyzer import Analyzer
from discovery_agent.validator import Validator
import numpy as np

class DiscoveryAgent:
    def __init__(self):
        self.data_manager = DataManager()
        self.analyzer = Analyzer(power_threshold=0.01) # More sensitive
        self.validator = Validator()
        self.iteration = 1
        self.log_file = "research_log.md"
        self.discovery_file = "discovered_stars.md"

    def log(self, message):
        timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
        log_entry = f"[{timestamp}] {message}\n"
        print(log_entry.strip())
        with open(self.log_file, "a") as f:
            f.write(log_entry)

    def log_discovery(self, ra, dec, p_ls, power_ls):
        discovery_id = f"V_{int(time.time())}"
        entry = f"| {discovery_id} | {ra:.5f} | {dec:.5f} | {p_ls:.4f} | {power_ls:.4f} | VSX/Simbad/Gaia Clear | Candidate |\n"
        with open(self.discovery_file, "a") as f:
            f.write(entry)

    def run_iteration(self):
        self.log(f"--- Iteration {self.iteration} Starting ---")

        coord = self.data_manager.select_target()
        tpf = self.data_manager.download_data(coord)

        if tpf is None:
            self.log(f"No data found for {coord.to_string('hmsdms')}. Skipping.")
            return

        self.log(f"Acquired Sector {tpf.sector} data for {coord.to_string('hmsdms')}")

        sources = self.analyzer.detect_sources(tpf)
        if sources is None or len(sources) == 0:
            self.log("No sources detected.")
            return

        for i, source in enumerate(sources):
            x, y = source['x_peak'], source['y_peak']
            lc = self.analyzer.extract_lightcurve(tpf, x, y)
            results = self.analyzer.analyze_variability(lc)

            if results and results['is_candidate']:
                pixel_coords = np.array([[x, y]])
                ra_dec = tpf.wcs.all_pix2world(pixel_coords, 0)
                star_ra, star_dec = ra_dec[0]

                if self.validator.is_new_discovery(star_ra, star_dec):
                    self.log(f"!!! NEW DISCOVERY !!! RA={star_ra:.5f}, Dec={star_dec:.5f}")
                    # Handle possible Quantity
                    p_val = results['period_ls'].value if hasattr(results['period_ls'], 'value') else results['period_ls']
                    self.log_discovery(star_ra, star_dec, p_val, results['power_ls'])
                else:
                    self.log(f"Variable detected at RA={star_ra:.5f}, but already cataloged.")

        self.log(f"Iteration {self.iteration} complete.")
        self.iteration += 1

    def start(self, max_iterations=100):
        self.log("Discovery Agent Online - Continuous Mode")
        for _ in range(max_iterations):
            try:
                self.run_iteration()
            except Exception as e:
                self.log(f"Error: {e}")
            time.sleep(1)

if __name__ == "__main__":
    agent = DiscoveryAgent()
    agent.start(50)
