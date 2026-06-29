import numpy as np
import pandas as pd
from astroquery.gaia import Gaia
from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u
from hdbscan import HDBSCAN
from sklearn.preprocessing import StandardScaler
import time
import matplotlib.pyplot as plt

# Set Simbad timeout
Simbad.TIMEOUT = 120

class OpenStarClusterHunter:
    def __init__(self, min_cluster_size=15):
        self.min_cluster_size = min_cluster_size
        self.candidates = []

    def fetch_data(self, ra, dec, radius, limit=10000):
        """Fetches Gaia DR3 data for a circular region."""
        print(f"Fetching data for RA={ra}, Dec={dec}, radius={radius}...")
        query = f"""
        SELECT TOP {limit} ra, dec, parallax, pmra, pmdec, phot_g_mean_mag, phot_bp_mean_mag, phot_rp_mean_mag
        FROM gaiadr3.gaia_source
        WHERE 1=CONTAINS(
            POINT('ICRS', ra, dec),
            CIRCLE('ICRS', {ra}, {dec}, {radius})
        )
        AND parallax > 0
        AND phot_g_mean_mag < 18
        AND parallax_over_error > 5
        """
        try:
            job = Gaia.launch_job(query)
            results = job.get_results()
            df = results.to_pandas()
            print(f"Fetched {len(df)} stars.")
            return df
        except Exception as e:
            print(f"Gaia query failed: {e}")
            return pd.DataFrame()

    def find_clusters(self, df):
        """Runs HDBSCAN on the fetch data."""
        if len(df) < self.min_cluster_size:
            return []

        # Feature selection
        features = df[['ra', 'dec', 'parallax', 'pmra', 'pmdec']].copy()

        # Normalize spatial coordinates relative to the center to avoid scale issues at different Dec
        ra_mean = features['ra'].mean()
        dec_mean = features['dec'].mean()
        features['ra'] = (features['ra'] - ra_mean) * np.cos(np.radians(dec_mean))
        features['dec'] = features['dec'] - dec_mean

        scaler = StandardScaler()
        scaled = scaler.fit_transform(features)

        # Adjusting weights to emphasize kinematics
        scaled[:, 0] *= 1.0 # Adjusted RA
        scaled[:, 1] *= 1.0 # Adjusted Dec
        scaled[:, 2] *= 5.0 # Parallax
        scaled[:, 3] *= 15.0 # PMRA
        scaled[:, 4] *= 15.0 # PMDEC

        # Use min_samples to reduce noise sensitivity
        clusterer = HDBSCAN(
            min_cluster_size=self.min_cluster_size,
            min_samples=max(5, self.min_cluster_size // 4),
            metric='euclidean',
            cluster_selection_method='eom'
        )
        cluster_labels = clusterer.fit_predict(scaled)

        df['cluster_label'] = cluster_labels

        unique_labels = set(cluster_labels)
        detected_clusters = []

        for label in unique_labels:
            if label == -1:
                continue

            cluster_stars = df[df['cluster_label'] == label]
            if len(cluster_stars) >= self.min_cluster_size:
                detected_clusters.append(cluster_stars)

        print(f"Detected {len(detected_clusters)} potential clusters.")
        return detected_clusters

    def validate_candidate(self, cluster_df):
        """Performs initial validation of a cluster candidate."""
        pmra_std = cluster_df['pmra'].std()
        pmdec_std = cluster_df['pmdec'].std()
        parallax_std = cluster_df['parallax'].std()

        # Dispersion check
        if pmra_std > 2.0 or pmdec_std > 2.0 or parallax_std > 1.0:
            return False, f"Low kinematic coherence (PM std: {pmra_std:.2f}, {pmdec_std:.2f}, Parallax std: {parallax_std:.2f})"

        # Member count check
        if len(cluster_df) < self.min_cluster_size:
            return False, f"Too few members: {len(cluster_df)}"

        return True, "Valid"

    def cross_match(self, cluster_df):
        """Cross-matches candidate with Simbad and known cluster prefixes."""
        ra_mean = cluster_df['ra'].mean()
        dec_mean = cluster_df['dec'].mean()

        try:
            custom_simbad = Simbad()
            custom_simbad.add_votable_fields('otype', 'otypes')
            coord = SkyCoord(ra=ra_mean*u.deg, dec=dec_mean*u.deg, frame='icrs')
            result_table = custom_simbad.query_region(coord, radius=10*u.arcmin)

            if result_table is not None and len(result_table) > 0:
                colnames = result_table.colnames
                for row in result_table:
                    main_id = row['main_id']
                    if isinstance(main_id, bytes): main_id = main_id.decode()

                    # Common cluster prefixes and general catalog names
                    known_prefixes = [
                        'Messier', 'NGC', 'IC', 'Melotte', 'Pleiades', 'Hyades', 'Collinder',
                        'Stock', 'Trumpler', 'Berkeley', 'Haffner', 'Ruprecht', 'King',
                        'Biurakan', 'Dolidze', 'Lodén', 'Lyngå', 'Markarian', 'Pismis',
                        'Roslund', 'Sher', 'Stephenson', 'Thackeray', 'Tombaugh', 'Westerlund',
                        'MWSC', 'Kroneberger', 'UBC', 'COIN-Gaia', 'Cantat-Gaudin', 'Gulliver',
                        'RSG', 'ASCC', 'FSR', 'ESO', 'Harvard', 'Basel', 'Czernik', 'Dutra',
                        'Loden', 'Lynga', 'SAI', 'Teutsch', 'vdBergh', 'Waterloo', 'Westerlund',
                        'Alessi', 'BH', 'Bica', 'Dolidze', 'Feibelman', 'Frolov', 'Gum',
                        'Hogg', 'Ivanov', 'Juchert', 'Kharchenko', 'Koposov', 'Latyshev',
                        'Lund', 'Majaess', 'Pfleiderer', 'Riddle', 'Saurer', 'Schuster',
                        'Skiff', 'Turner', 'Vandenbergh', 'Waterloo', 'Winner'
                    ]

                    if any(prefix in main_id for prefix in known_prefixes) or \
                       ('[' in main_id and ']' in main_id):
                        return True, main_id

                    # Check OTYPE
                    otype = ""
                    if 'OTYPE' in colnames:
                        otype = row['OTYPE']
                    elif 'otype' in colnames:
                        otype = row['otype']

                    if isinstance(otype, bytes): otype = otype.decode()
                    otype = str(otype)

                    # Check OTYPES
                    otypes = ""
                    if 'OTYPES' in colnames:
                        otypes = row['OTYPES']
                    elif 'otypes' in colnames:
                        otypes = row['otypes']

                    if isinstance(otypes, bytes): otypes = otypes.decode()
                    otypes = str(otypes)

                    cluster_types = ['Cl*', 'OpC', 'Assoc', 'GlC', 'Cl?', 'OCL', 'GCl']
                    if otype in cluster_types or any(ct in otypes for ct in cluster_types):
                        return True, f"{main_id} ({otype})"

                    if 'Cl' in main_id or 'Cluster' in main_id or 'Assoc' in main_id or 'OCL' in main_id:
                        return True, main_id

        except Exception as e:
            print(f"Simbad query failed: {e}")

        return False, None

    def calculate_novelty_score(self, cluster_df):
        """Assigns a novelty score to the candidate."""
        # Score based on member count and kinematic tightness
        pmra_std = cluster_df['pmra'].std()
        pmdec_std = cluster_df['pmdec'].std()
        pm_std = np.sqrt(pmra_std**2 + pmdec_std**2)
        score = (len(cluster_df) / 50.0) + (1.0 / (pm_std + 0.1))
        return score

    def run_scan(self, ra, dec, radius):
        df = self.fetch_data(ra, dec, radius)
        if df.empty:
            return

        potential_clusters = self.find_clusters(df)

        for cluster in potential_clusters:
            is_valid, reason = self.validate_candidate(cluster)
            if is_valid:
                is_known, name = self.cross_match(cluster)
                if not is_known:
                    score = self.calculate_novelty_score(cluster)
                    print(f"*** POTENTIAL NEW OPEN CLUSTER CANDIDATE ***")
                    print(f"Location: RA={cluster['ra'].mean():.4f}, Dec={cluster['dec'].mean():.4f}")
                    print(f"Stars: {len(cluster)}")
                    print(f"Mean PM: ({cluster['pmra'].mean():.2f}, {cluster['pmdec'].mean():.2f})")
                    print(f"Mean Parallax: {cluster['parallax'].mean():.2f}")
                    print(f"Novelty Score: {score:.2f}")
                    self.candidates.append({
                        'data': cluster,
                        'ra': cluster['ra'].mean(),
                        'dec': cluster['dec'].mean(),
                        'score': score
                    })
                else:
                    print(f"Known cluster {name} detected at RA={cluster['ra'].mean():.4f}, Dec={cluster['dec'].mean():.4f}")
            else:
                # print(f"Rejected cluster at RA={cluster['ra'].mean():.4f}, Dec={cluster['dec'].mean():.4f} because: {reason}")
                pass

    def report_findings(self):
        if not self.candidates:
            print("No new cluster candidates found in this scan.")
            return

        print(f"\nScan complete. Found {len(self.candidates)} candidates.")
        for i, cand in enumerate(self.candidates):
            print(f"Candidate {i+1}: RA={cand['ra']:.4f}, Dec={cand['dec']:.4f}, Score={cand['score']:.2f}")

if __name__ == "__main__":
    hunter = OpenStarClusterHunter(min_cluster_size=20)
    hunter.run_scan(ra=290.0, dec=10.0, radius=0.7)
    hunter.report_findings()
