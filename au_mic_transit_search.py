try:
    import lightkurve as lk
except ImportError:
    import subprocess, sys
    subprocess.check_call([sys.executable, "-m", "pip", "install", "lightkurve"])
    import lightkurve as lk

try:
    import celerite2
    from celerite2 import terms
except ImportError:
    import subprocess, sys
    subprocess.check_call([sys.executable, "-m", "pip", "install", "celerite2"])
    import celerite2
    from celerite2 import terms

import numpy as np
import matplotlib.pyplot as plt
from scipy.linalg import hankel
from scipy.sparse.linalg import svds
from scipy.signal import medfilt
from scipy.optimize import minimize
import warnings, time

# Suppress warnings
warnings.filterwarnings('ignore')

def fast_diagonal_averaging(X):
    """Vectorized reconstruction for SSA."""
    L, K = X.shape
    j, k = np.indices(X.shape)
    indices = (j + k).ravel()
    sums = np.bincount(indices, weights=X.ravel())
    counts = np.bincount(indices)
    return sums / counts

def optimized_ssa(flux, L, n_components=8):
    """Data-adaptive SSA decomposition."""
    N = len(flux)
    K = N - L + 1
    X = hankel(flux[:L], flux[L-1:])
    U, Sigma, VT = svds(X, k=n_components)
    U, Sigma, VT = U[:, ::-1], Sigma[::-1], VT[::-1, :]
    Xr = np.zeros_like(X)
    for i in range(n_components):
        Xr += Sigma[i] * np.outer(U[:, i], VT[i, :])
    return fast_diagonal_averaging(Xr)

def calculate_snr(flux, mask):
    """Robust SNR Metric (1.4826 * MAD)."""
    out_of_transit = flux[~mask]
    in_transit = flux[mask]
    if len(in_transit) == 0: return 0
    depth = np.median(out_of_transit) - np.median(in_transit)
    noise = 1.4826 * np.median(np.abs(out_of_transit - np.median(out_of_transit)))
    return depth / noise

def get_multi_transit_mask(time, t0, period, duration):
    """Ephemeris-based mask for multiple transits."""
    mask = np.zeros(len(time), dtype=bool)
    t_start, t_end = np.min(time), np.max(time)
    n_min = int((t_start - t0) / period) - 2
    n_max = int((t_end - t0) / period) + 2
    for n in range(n_min, n_max + 1):
        t_trans = t0 + n * period
        mask |= (time > t_trans - duration/2) & (time < t_trans + duration/2)
    return mask

def process_sector(lc_raw, sector_id):
    """Complete SSA-GP pipeline for a single TESS sector."""
    lc = lc_raw.remove_nans().normalize()
    time_arr, flux, flux_err = lc.time.value, lc.flux.value, lc.flux_err.value

    # AU Mic b Ephemeris (Plavchan 2020)
    period, t0, duration = 8.4622, 1330.3905, 0.15
    mask = get_multi_transit_mask(time_arr, t0, period, duration)

    # Baseline
    b_flux = lc.flatten(window_length=101).flux.value

    # SSA Stage
    f_med = medfilt(flux, 501)
    f_clipped = np.clip(flux, None, f_med + 5 * np.median(np.abs(flux - f_med)))
    best_ssa_detrended = flux / optimized_ssa(f_clipped, 200, n_components=6)

    # GP Stage
    x = time_arr - np.min(time_arr)
    y = best_ssa_detrended - 1.0
    yerr = flux_err / (flux / best_ssa_detrended)

    def neg_log_like(params, x_in, y_in, yerr_in):
        kernel = terms.SHOTerm(sigma=np.exp(params[0]), rho=np.exp(params[1]), Q=np.exp(params[2]))
        gp = celerite2.GaussianProcess(kernel, mean=0.0)
        gp.compute(x_in, yerr=yerr_in)
        return -gp.log_likelihood(y_in)

    init_p = np.array([np.log(np.std(y)), np.log(0.5), np.log(0.25)])
    soln = minimize(neg_log_like, init_p, method="L-BFGS-B", args=(x[~mask], y[~mask], yerr[~mask]))

    final_k = terms.SHOTerm(sigma=np.exp(soln.x[0]), rho=np.exp(soln.x[1]), Q=np.exp(soln.x[2]))
    gp_final = celerite2.GaussianProcess(final_k, mean=0.0)
    gp_final.compute(x[~mask], yerr=yerr[~mask])
    mu = gp_final.predict(y[~mask], t=x)
    h_flux = best_ssa_detrended - mu

    return {
        'sector': sector_id,
        'time': time_arr,
        'b_snr': calculate_snr(b_flux, mask),
        'h_snr': calculate_snr(h_flux, mask),
        'h_flux': h_flux,
        'mask': mask
    }

def main():
    print("--- AU Mic ENSEMBLE MULTI-SECTOR PIPELINE ---")
    search = lk.search_lightcurve("AU Mic", author="SPOC")
    # Process Sector 1, 27 (120s), and 95 (120s) if available
    # Search indices for 120s products: 0 (S1), 2 (S27), 4 (S95)
    indices = [0, 2]
    if len(search) > 4: indices.append(4)

    lcs = search[indices].download_all()
    results = [process_sector(lc, f"Sector {lc.sector}") for lc in lcs]

    fig, axes = plt.subplots(len(results), 1, figsize=(15, 5 * len(results)))
    for ax, res in zip(axes, results):
        ax.plot(res['time'], res['h_flux'], 'k.', markersize=0.5, alpha=0.3)
        ax.plot(res['time'][res['mask']], res['h_flux'][res['mask']], 'r.', markersize=1, label='Transits')
        ax.set_title(f"AU Mic {res['sector']} | Hybrid SNR: {res['h_snr']:.2f}")
        ax.set_ylim(0.99, 1.01)
        ax.legend()

    plt.tight_layout()
    plt.savefig("au_mic_ensemble_results.png")

    print("\n" + "="*50)
    print(f"{'SECTOR':<15} | {'BASELINE SNR':<15} | {'HYBRID SNR':<15}")
    print("-" * 50)
    for res in results:
        print(f"{res['sector']:<15} | {res['b_snr']:<15.4f} | {res['h_snr']:<15.4f}")
    print("="*50)

if __name__ == "__main__":
    main()
