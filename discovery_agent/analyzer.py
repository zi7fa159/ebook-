import numpy as np
from photutils.detection import find_peaks
from astropy.stats import sigma_clipped_stats
import lightkurve as lk

class Analyzer:
    def __init__(self, power_threshold=0.1):
        self.power_threshold = power_threshold

    def detect_sources(self, tpf):
        median_image = np.nanmedian(tpf.flux.value, axis=0)
        median_image = np.nan_to_num(median_image)
        _, median, std = sigma_clipped_stats(median_image, sigma=3.0)

        sources = find_peaks(median_image, threshold=median + 3*std, n_peaks=50)
        return sources

    def extract_lightcurve(self, tpf, x, y):
        h, w = tpf.shape[1:]
        xi, yi = int(round(x)), int(round(y))

        if 1 <= xi < w-1 and 1 <= yi < h-1:
            mask = np.zeros((h, w), dtype=bool)
            mask[yi-1:yi+2, xi-1:xi+2] = True

            try:
                lc = tpf.to_lightcurve(aperture_mask=mask)
                # Basic background subtraction
                bg_mask = ~tpf.create_threshold_mask(threshold=3)
                if bg_mask.any():
                    bg_lc = tpf.to_lightcurve(aperture_mask=bg_mask)
                    lc.flux = lc.flux - (bg_lc.flux * (mask.sum() / bg_mask.sum()))

                return lc.remove_outliers().flatten()
            except Exception:
                return None
        return None

    def analyze_variability(self, lc):
        if lc is None or len(lc) == 0:
            return None

        try:
            # Algorithm 1: Lomb-Scargle
            pg_ls = lc.to_periodogram(method='lombscargle', minimum_period=0.1, maximum_period=10)
            max_power_ls = pg_ls.max_power.value if hasattr(pg_ls.max_power, 'value') else pg_ls.max_power
            period_ls = pg_ls.period_at_max_power

            # Algorithm 2: Box Least Squares (BLS)
            pg_bls = lc.to_periodogram(method='bls', minimum_period=0.1, maximum_period=10)
            max_power_bls = pg_bls.max_power.value if hasattr(pg_bls.max_power, 'value') else pg_bls.max_power
            period_bls = pg_bls.period_at_max_power

            is_candidate = (max_power_ls > self.power_threshold) or (max_power_bls > self.power_threshold * 10)

            return {
                'period_ls': period_ls,
                'power_ls': max_power_ls,
                'period_bls': period_bls,
                'power_bls': max_power_bls,
                'is_candidate': is_candidate
            }
        except Exception:
            return None
