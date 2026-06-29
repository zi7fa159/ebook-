import os
import time
import requests
import numpy as np
import pandas as pd
from io import StringIO
from astropy.coordinates import SkyCoord
import astropy.units as u
from astropy.io import fits
from astropy.wcs import WCS
from astropy.stats import mad_std
from photutils.detection import DAOStarFinder
import cv2

class SupernovaHunter:
    def __init__(self):
        self.log_file = "research_log.md"
        self.iteration = 1
        self.data_dir = "data"
        if not os.path.exists(self.data_dir):
            os.makedirs(self.data_dir)

    def log_progress(self, message):
        print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] {message}")

    def update_research_log(self, galaxy, coord, dataset, detections, rejected, candidates, confidence, decisions):
        with open(self.log_file, "a") as f:
            f.write(f"| {time.strftime('%Y-%m-%d %H:%M:%S')} | {galaxy} | {coord} | {dataset} | {detections} | {rejected} | {candidates} | {confidence} | {decisions} |\n")

    def select_target(self):
        targets = [
            {"name": "M101", "coord": SkyCoord.from_name("M101")},
            {"name": "M51", "coord": SkyCoord.from_name("M51")},
            {"name": "NGC 6946", "coord": SkyCoord.from_name("NGC 6946")},
            {"name": "M81", "coord": SkyCoord.from_name("M81")},
            {"name": "NGC 4559", "coord": SkyCoord.from_name("NGC 4559")},
            {"name": "NGC 4631", "coord": SkyCoord.from_name("NGC 4631")},
        ]
        target = targets[(self.iteration - 1) % len(targets)]
        return target

    def query_ztf_metadata(self, coord, product='sci'):
        base_url = f"https://irsa.ipac.caltech.edu/ibe/search/ztf/products/{product}"
        params = {"POS": f"{coord.ra.deg},{coord.dec.deg}", "CT": "csv"}
        try:
            response = requests.get(base_url, params=params, timeout=30)
            if response.status_code == 200:
                return pd.read_csv(StringIO(response.text))
            return None
        except: return None

    def get_cutout_url(self, row, coord, size_arcsec=600, product='sci'):
        padded_field = f"{row['field']:06d}"
        padded_ccdid = f"{row['ccdid']:02d}"
        if product == 'sci':
            filefracday = str(row['filefracday'])
            year, monthday, fracday = filefracday[:4], filefracday[4:8], filefracday[8:]
            filename = f"ztf_{filefracday}_{padded_field}_{row['filtercode']}_c{padded_ccdid}_o_q{row['qid']}_sciimg.fits"
            path = f"{year}/{monthday}/{fracday}/"
        elif product == 'ref':
            fieldprefix = padded_field[:3]
            filename = f"ztf_{padded_field}_{row['filtercode']}_c{padded_ccdid}_q{row['qid']}_refimg.fits"
            path = f"{fieldprefix}/field{padded_field}/{row['filtercode']}/ccd{padded_ccdid}/q{row['qid']}/"
        else: return None
        url = f"https://irsa.ipac.caltech.edu/ibe/data/ztf/products/{product}/{path}{filename}"
        return f"{url}?center={coord.ra.deg},{coord.dec.deg}&size={size_arcsec}arcsec&gzip=false"

    def download_fits(self, url, target_name, suffix):
        filename = os.path.join(self.data_dir, f"{target_name}_{suffix}.fits")
        if os.path.exists(filename): return filename
        try:
            response = requests.get(url, timeout=60)
            if response.status_code == 200:
                with open(filename, 'wb') as f: f.write(response.content)
                return filename
            return None
        except: return None

    def cross_match_tns(self, coord):
        # Placeholder for real TNS query
        return False

    def process_and_subtract(self, sci_fits, ref_fits):
        try:
            with fits.open(sci_fits) as hdul_sci:
                sci_data = hdul_sci[0].data.astype(float)
                sci_header = hdul_sci[0].header
                sci_wcs = WCS(sci_header)
            with fits.open(ref_fits) as hdul_ref:
                ref_data = hdul_ref[0].data.astype(float)

            # 1. Background subtraction
            sci_data -= np.nanmedian(sci_data)
            ref_data -= np.nanmedian(ref_data)

            # 2. Gaussian Blur to match PSFs roughly (if needed, but here we just smooth noise)
            # Actually, let's use a very simple blur on the noisier one

            # 3. Shape matching
            min_h, min_w = min(sci_data.shape[0], ref_data.shape[0]), min(sci_data.shape[1], ref_data.shape[1])
            sci_data, ref_data = sci_data[:min_h, :min_w], ref_data[:min_h, :min_w]

            # 4. Global scaling
            std_sci, std_ref = np.nanstd(sci_data), np.nanstd(ref_data)
            if std_ref > 0: ref_data *= (std_sci / std_ref)

            diff = sci_data - ref_data
            return diff, sci_header, sci_wcs
        except: return None, None, None

    def run_iteration(self):
        self.log_progress(f"--- Iteration {self.iteration} ---")
        target = self.select_target()
        coord = target['coord']
        sci_meta, ref_meta = self.query_ztf_metadata(coord, 'sci'), self.query_ztf_metadata(coord, 'ref')

        if sci_meta is not None and not sci_meta.empty and ref_meta is not None and not ref_meta.empty:
            sci_row = sci_meta.sort_values('obsjd', ascending=False).iloc[0]
            ref_match = ref_meta[(ref_meta['field'] == sci_row['field']) & (ref_meta['ccdid'] == sci_row['ccdid']) &
                                 (ref_meta['qid'] == sci_row['qid']) & (ref_meta['filtercode'] == sci_row['filtercode'])]

            if not ref_match.empty:
                ref_row = ref_match.iloc[0]
                sci_file = self.download_fits(self.get_cutout_url(sci_row, coord, product='sci'), target['name'], f"sci_{sci_row['filtercode']}_{sci_row['filefracday']}")
                ref_file = self.download_fits(self.get_cutout_url(ref_row, coord, product='ref'), target['name'], f"ref_{sci_row['filtercode']}")

                if sci_file and ref_file:
                    diff, header, wcs = self.process_and_subtract(sci_file, ref_file)
                    if diff is not None:
                        bkg_sigma = mad_std(diff, ignore_nan=True)
                        daofind = DAOStarFinder(fwhm=4.0, threshold=10.*bkg_sigma) # Stricter detection
                        sources = daofind(diff)

                        detections = len(sources) if sources is not None else 0
                        candidates_found = []
                        if sources is not None:
                            for src in sources:
                                x, y = src['x_centroid'], src['y_centroid']
                                # Artifact rejection: Sharpness and Roundness
                                if 0.2 < src['sharpness'] < 0.8 and -0.5 < src['roundness1'] < 0.5:
                                    if 50 < x < diff.shape[1]-50 and 50 < y < diff.shape[0]-50:
                                        sky_pos = wcs.pixel_to_world(x, y)
                                        if not self.cross_match_tns(sky_pos):
                                            candidates_found.append(sky_pos)

                        num_cand = len(candidates_found)
                        self.log_progress(f"Detected {detections} sources, {num_cand} candidates remain after filtering.")
                        self.update_research_log(target['name'], coord.to_string('hmsdms'), "ZTF", detections, detections - num_cand, num_cand, f"{0.1*num_cand:.2f}", f"Filtered by sharpness/roundness.")
                        if num_cand > 0:
                            for c in candidates_found: self.log_progress(f"Candidate: {c.to_string('hmsdms')}")
        self.iteration += 1

if __name__ == "__main__":
    hunter = SupernovaHunter()
    for _ in range(3):
        hunter.run_iteration()
        time.sleep(1)
