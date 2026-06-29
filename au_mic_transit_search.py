try:
    import lightkurve as lk
except ImportError:
    import subprocess, sys
    subprocess.check_call([sys.executable, "-m", "pip", "install", "lightkurve"])
    import lightkurve as lk

import numpy as np
import matplotlib.pyplot as plt
from scipy.linalg import hankel
from scipy.sparse.linalg import svds
from scipy.signal import medfilt
import warnings, time

# Suppress warnings
warnings.filterwarnings('ignore')

def fast_diagonal_averaging(X):
    """Vectorized diagonal averaging for SSA reconstruction."""
    L, K = X.shape
    j, k = np.indices(X.shape)
    indices = (j + k).ravel()
    sums = np.bincount(indices, weights=X.ravel())
    counts = np.bincount(indices)
    return sums / counts

def optimized_ssa(flux, L, n_components=5):
    """
    Optimized Singular Spectrum Analysis using Sparse SVD and
    Vectorized Diagonal Averaging.
    """
    N = len(flux)
    K = N - L + 1
    X = hankel(flux[:L], flux[L-1:])
    # svds is significantly faster for large N when k << L
    U, Sigma, VT = svds(X, k=n_components)
    U, Sigma, VT = U[:, ::-1], Sigma[::-1], VT[::-1, :]
    Xr = np.zeros_like(X)
    for i in range(n_components):
        Xr += Sigma[i] * np.outer(U[:, i], VT[i, :])
    return fast_diagonal_averaging(Xr)

def calculate_snr(flux, mask):
    """Robust SNR: Depth / (1.4826 * MAD)."""
    out_of_transit = flux[~mask]
    in_transit = flux[mask]
    if len(in_transit) == 0: return 0
    depth = np.median(out_of_transit) - np.median(in_transit)
    noise = 1.4826 * np.median(np.abs(out_of_transit - np.median(out_of_transit)))
    return depth / noise

def main():
    print("--- AU Mic Optimized 10k Samples SSA Pipeline ---")

    # 1. DATA INGESTION
    print("Downloading AU Mic TESS Sector 1...")
    search = lk.search_lightcurve("AU Mic", author="SPOC", sector=1)
    lc = search.download().remove_nans().normalize()

    # Scale to 10,000 samples for the benchmark requirement
    lc = lc[:10000]
    time_arr, flux = lc.time.value, lc.flux.value
    print(f"Processing {len(flux)} samples.")

    # 2. BASELINE
    start_b = time.time()
    baseline_lc = lc.flatten(window_length=101)
    b_flux = baseline_lc.flux.value
    time_b = time.time() - start_b

    # 3. NOVEL SSA PIPELINE (IMPROVED)
    print("Running Optimized SSA Pipeline...")
    t_center, t_dur = 1330.39, 0.12
    t_mask = (time_arr > t_center - t_dur/2) & (time_arr < t_center + t_dur/2)

    # Pre-clip flares to isolate the trend more effectively
    f_med = medfilt(flux, 501)
    mad_val = np.median(np.abs(flux - f_med))
    flux_clipped = np.clip(flux, None, f_med + 5 * mad_val)

    # Tenacity Optimization Loop
    L_range = [100, 250, 500, 750, 1000]
    best_mad, best_ssa_flux, best_L = np.inf, None, 0
    start_s = time.time()

    for L in L_range:
        trend = optimized_ssa(flux_clipped, L, n_components=6)
        detrended = flux / trend
        res_mad = np.median(np.abs(detrended[~t_mask] - np.median(detrended[~t_mask])))
        if res_mad < best_mad:
            best_mad, best_ssa_flux, best_L = res_mad, detrended, L

    time_s = time.time() - start_s

    # 4. BENCHMARKING
    b_snr = calculate_snr(b_flux, t_mask)
    s_snr = calculate_snr(best_ssa_flux, t_mask)
    improvement = ((s_snr - b_snr) / abs(b_snr)) * 100

    print("\n--- PERFORMANCE SUMMARY (10,000 SAMPLES) ---")
    print(f"Baseline SNR: {b_snr:.4f} | Time: {time_b:.4f}s")
    print(f"Novel SSA SNR: {s_snr:.4f} | Time: {time_s:.4f}s (Optimized L={best_L})")
    print(f"SNR Improvement: {improvement:.2f}%")

    # 5. VISUAL DASHBOARD
    fig, ax = plt.subplots(3, 1, figsize=(14, 12))
    ax[0].plot(time_arr, flux, 'k.', markersize=0.5)
    ax[0].set_title("Raw AU Mic (10,000 Samples)", fontweight='bold')

    z = (time_arr > t_center - 1.0) & (time_arr < t_center + 1.0)
    ax[1].plot(time_arr[z], b_flux[z], 'r.', markersize=2, label='Baseline')
    ax[1].axvspan(t_center-t_dur/2, t_center+t_dur/2, color='blue', alpha=0.1)
    ax[1].set_title(f"Baseline (SG) | SNR: {b_snr:.2f}", fontweight='bold')
    ax[1].set_ylim(0.99, 1.01)

    ax[2].plot(time_arr[z], best_ssa_flux[z], 'g.', markersize=2, label='SSA')
    ax[2].axvspan(t_center-t_dur/2, t_center+t_dur/2, color='blue', alpha=0.1)
    ax[2].set_title(f"Optimized SSA | SNR: {s_snr:.2f}", fontweight='bold')
    ax[2].set_ylim(0.99, 1.01)
    ax[2].set_xlabel("Time (BTJD)")

    plt.tight_layout()
    plt.savefig("au_mic_10k_benchmark.png", dpi=200)
    print("\nBenchmark dashboard saved: au_mic_10k_benchmark.png")

if __name__ == "__main__":
    main()
