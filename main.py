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
        self.analyzer = Analyzer()
        self.validator = Validator()
        self.iteration = 1
        self.log_file = "research_log.md"

    def log(self, message):
        timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
        log_entry = f"[{timestamp}] {message}\n"
        print(log_entry.strip())
        with open(self.log_file, "a") as f:
            f.write(log_entry)

    def run_iteration(self):
        self.log(f"--- Iteration {self.iteration} Starting ---")

        # 1. Selection & Acquisition
        coord = self.data_manager.select_target()
        tpf = self.data_manager.download_data(coord)

        if tpf is None:
            self.log(f"No data found for {coord.to_string('hmsdms')}. Skipping.")
            return False

        self.log(f"Acquired Sector {tpf.sector} data for {coord.to_string('hmsdms')}")

        # 2. Analysis
        sources = self.analyzer.detect_sources(tpf)
        if sources is None or len(sources) == 0:
            self.log("No sources detected.")
            return False

        for i, source in enumerate(sources):
            x, y = source['x_peak'], source['y_peak']
            lc = self.analyzer.extract_lightcurve(tpf, x, y)
            results = self.analyzer.analyze_variability(lc)

            if results and results['is_candidate']:
                pixel_coords = np.array([[x, y]])
                ra_dec = tpf.wcs.all_pix2world(pixel_coords, 0)
                star_ra, star_dec = ra_dec[0]

                # 3. Multi-catalog Validation
                if self.validator.is_new_discovery(star_ra, star_dec):
                    self.log(f"!!! DISCOVERY CANDIDATE !!! RA={star_ra:.5f}, Dec={star_dec:.5f}")
                    self.log(f"Analysis: P_LS={results['period_ls']:.4f}, P_BLS={results['period_bls']:.4f}")
                    self.log("PAUSING AUTOMATION FOR REVIEW.")
                    return True # FOUND A CANDIDATE, SIGNAL PAUSE
                else:
                    self.log(f"Variable detected at RA={star_ra:.5f}, but already cataloged.")

        self.log(f"Iteration {self.iteration} complete. No new candidates.")
        self.iteration += 1
        return False

    def start(self, max_iterations=100):
        self.log("Discovery Agent Online")
        for _ in range(max_iterations):
            try:
                found = self.run_iteration()
                if found:
                    break # STOP AS PER RULE
            except Exception as e:
                self.log(f"Error: {e}")
            time.sleep(2)

if __name__ == "__main__":
    agent = DiscoveryAgent()
    agent.start()
