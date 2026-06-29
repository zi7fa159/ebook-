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
from scipy.interpolate import interp1d
import warnings, time

# Suppress warnings
warnings.filterwarnings('ignore')

# --- CORE SCIENTIFIC UTILITIES ---

def asymmetric_flare_clip(flux, window_size=501):
    """
    Step 1: Asymmetric Flare Clipping (Pre-SSA)
    Removes positive outliers (> +3 MAD) while preserving transit dips.
    """
    f_med = medfilt(flux, kernel_size=window_size)
    mad = np.median(np.abs(flux - f_med))
    is_flare = (flux - f_med) > (3 * mad)
    clean_flux = np.copy(flux)
    x = np.arange(len(flux))
    if np.any(is_flare):
        interp_func = interp1d(x[~is_flare], flux[~is_flare], kind='linear', fill_value="extrapolate")
        clean_flux[is_flare] = interp_func(x[is_flare])
    return clean_flux

def fast_diagonal_averaging(X):
    """Vectorized diagonal averaging for SSA."""
    L, K = X.shape
    j, k = np.indices(X.shape)
    indices = (j + k).ravel()
    sums = np.bincount(indices, weights=X.ravel())
    counts = np.bincount(indices)
    return sums / counts

def optimized_ssa(flux, L, n_components=6):
    """Data-adaptive SSA decomposition using Sparse SVD."""
    N = len(flux)
    K = N - L + 1
    X = hankel(flux[:L], flux[L-1:])
    U, S, VT = svds(X, k=n_components)
    U, S, VT = U[:, ::-1], S[::-1], VT[::-1, :]
    Xr = np.zeros_like(X)
    for i in range(n_components):
        Xr += S[i] * np.outer(U[:, i], VT[i, :])
    return fast_diagonal_averaging(Xr)

def calculate_snr(flux, mask):
    """Robust Transit SNR: Depth / (1.4826 * MAD)."""
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
    n_min, n_max = int((t_start - t0) / period) - 2, int((t_end - t0) / period) + 2
    for n in range(n_min, n_max + 1):
        t_trans = t0 + n * period
        mask |= (time > t_trans - duration/2) & (time < t_trans + duration/2)
    return mask

def process_sector(lc_raw, sector_id):
    """Refined Sector-by-Sector Pipeline."""
    lc = lc_raw.remove_nans().normalize()
    time_arr, flux, flux_err = lc.time.value, lc.flux.value, lc.flux_err.value
    dt = np.median(np.diff(time_arr))

    period, t0, duration = 8.4622, 1330.3905, 0.15
    mask = get_multi_transit_mask(time_arr, t0, period, duration)

    # 1. Flare Clipping
    clean_flux_ssa = asymmetric_flare_clip(flux)

    # 2. Physically-Grounded SSA (L = 4.8 days)
    L = int(4.8 / dt)
    if L > len(flux) // 2: L = len(flux) // 3
    trend_ssa = optimized_ssa(clean_flux_ssa, L, n_components=6)
    ssa_detrended = flux / trend_ssa

    # 3. GP with Pre-GP Masking
    x, y, yerr = time_arr - np.min(time_arr), ssa_detrended - 1.0, flux_err / trend_ssa

    # Kernel: SHOTerm for residual activity
    def neg_log_like(params, x_in, y_in, yerr_in):
        kernel = terms.SHOTerm(sigma=np.exp(params[0]), rho=np.exp(params[1]), Q=np.exp(params[2]))
        gp = celerite2.GaussianProcess(kernel, mean=0.0)
        gp.compute(x_in, yerr=yerr_in)
        return -gp.log_likelihood(y_in)

    x_train, y_train, yerr_train = x[~mask], y[~mask], yerr[~mask]
    initial_params = np.array([np.log(np.std(y_train)), np.log(0.1), np.log(0.25)])
    soln = minimize(neg_log_like, initial_params, method="L-BFGS-B", args=(x_train, y_train, yerr_train))

    final_kernel = terms.SHOTerm(sigma=np.exp(soln.x[0]), rho=np.exp(soln.x[1]), Q=np.exp(soln.x[2]))
    gp_final = celerite2.GaussianProcess(final_kernel, mean=0.0)
    gp_final.compute(x_train, yerr=yerr_train)
    mu = gp_final.predict(y_train, t=x)
    h_flux = ssa_detrended - mu

    b_flux = lc.flatten(window_length=101).flux.value

    return {
        'sector': sector_id, 'time': time_arr,
        'b_snr': calculate_snr(b_flux, mask), 'h_snr': calculate_snr(h_flux, mask),
        'h_flux': h_flux, 'mask': mask
    }

def main():
    print("--- AU Mic REFINED MULTI-SECTOR PIPELINE ---")
    search = lk.search_lightcurve("AU Mic", author="SPOC")
    indices = [0, 2] # S1 and S27
    if len(search) > 4: indices.append(4)
    lcs = search[indices].download_all()
    results = [process_sector(lc, f"Sector {lc.sector}") for lc in lcs]

    fig, axes = plt.subplots(len(results), 1, figsize=(15, 6 * len(results)))
    if len(results) == 1: axes = [axes]
    for ax, res in zip(axes, results):
        ax.plot(res['time'], res['h_flux'], 'k.', markersize=0.5, alpha=0.3)
        ax.plot(res['time'][res['mask']], res['h_flux'][res['mask']], 'r.', markersize=1.5, label='Transits')
        ax.set_title(f"AU Mic {res['sector']} | Refined Hybrid SNR: {res['h_snr']:.2f}")
        ax.set_ylim(0.985, 1.015)
        ax.legend(loc='upper right')
    plt.tight_layout()
    plt.savefig("au_mic_refined_dashboard.png", dpi=200)

    print("\n" + "="*60)
    print(f"{'SECTOR':<15} | {'BASELINE SNR':<15} | {'REFINED HYBRID SNR':<15}")
    print("-" * 60)
    for res in results:
        print(f"{res['sector']:<15} | {res['b_snr']:<15.4f} | {res['h_snr']:<15.4f}")
    print("="*60)

if __name__ == "__main__":
    main()
