
import unittest
import pandas as pd
import numpy as np
from cluster_hunter import OpenStarClusterHunter

class TestOpenStarClusterHunter(unittest.TestCase):
    def setUp(self):
        # Using a small min_cluster_size for testing
        self.hunter = OpenStarClusterHunter(min_cluster_size=10)

    def test_find_clusters_synthetic(self):
        """Test with synthetic cluster data."""
        np.random.seed(42)
        n_members = 50
        cluster_data = {
            'ra': np.random.normal(100.0, 0.001, n_members),
            'dec': np.random.normal(0.0, 0.001, n_members),
            'parallax': np.random.normal(1.0, 0.005, n_members),
            'pmra': np.random.normal(5.0, 0.01, n_members),
            'pmdec': np.random.normal(-5.0, 0.01, n_members),
            'phot_g_mean_mag': np.random.uniform(10, 15, n_members)
        }

        n_noise = 200
        noise_data = {
            'ra': np.random.uniform(99.0, 101.0, n_noise),
            'dec': np.random.uniform(-1.0, 1.0, n_noise),
            'parallax': np.random.uniform(0.1, 2.0, n_noise),
            'pmra': np.random.uniform(-10, 10, n_noise),
            'pmdec': np.random.uniform(-10, 10, n_noise),
            'phot_g_mean_mag': np.random.uniform(10, 15, n_noise)
        }

        df = pd.concat([pd.DataFrame(cluster_data), pd.DataFrame(noise_data)]).reset_index(drop=True)

        clusters = self.hunter.find_clusters(df)
        self.assertGreaterEqual(len(clusters), 1, "Failed to detect synthetic cluster")

        # Find the cluster closest to center
        found = False
        for c in clusters:
            if abs(c['ra'].mean() - 100.0) < 0.1:
                found = True
                break
        self.assertTrue(found, "The detected cluster is not the synthetic one.")

    def test_validate_candidate(self):
        """Test validation logic."""
        data = {
            'pmra': [5.0]*15,
            'pmdec': [-5.0]*15,
            'parallax': [1.0]*15
        }
        df = pd.DataFrame(data)
        is_valid, reason = self.hunter.validate_candidate(df)
        self.assertTrue(is_valid, f"Should be valid: {reason}")

        data_bad = {
            'pmra': np.random.uniform(-10, 10, 15),
            'pmdec': np.random.uniform(-10, 10, 15),
            'parallax': np.random.uniform(0, 5, 15)
        }
        df_bad = pd.DataFrame(data_bad)
        is_valid, reason = self.hunter.validate_candidate(df_bad)
        self.assertFalse(is_valid, "Should be invalid due to high dispersion")

if __name__ == '__main__':
    unittest.main()
