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

def optimized_ssa(flux, L, n_components=6):
    """
    Optimized SSA using Sparse SVD and Vectorized Reconstruction.
    """
    N = len(flux)
    K = N - L + 1
    X = hankel(flux[:L], flux[L-1:])
    # svds for efficiency
    U, Sigma, VT = svds(X, k=n_components)
    U, Sigma, VT = U[:, ::-1], Sigma[::-1], VT[::-1, :]
    Xr = np.zeros_like(X)
    for i in range(n_components):
        Xr += Sigma[i] * np.outer(U[:, i], VT[i, :])
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
    """Generates a mask for multiple transits based on ephemeris."""
    mask = np.zeros(len(time), dtype=bool)
    # Check for transits within the time range
    t_start, t_end = np.min(time), np.max(time)

    # Start from t0 and go backwards
    t = t0
    while t >= t_start:
        mask |= (time > t - duration/2) & (time < t + duration/2)
        t -= period

    # Start from t0 and go forwards
    t = t0 + period
    while t <= t_end:
        mask |= (time > t - duration/2) & (time < t + duration/2)
        t += period

    return mask

def main():
    print("--- AU Mic FULL MISSION CONCLUSION: SSA vs SG ---")

    # 1. DATA INGESTION
    print("Downloading Full AU Mic TESS Sector 1...")
    search = lk.search_lightcurve("AU Mic", author="SPOC", sector=1)
    lc = search.download().remove_nans().normalize()
    time_arr, flux = lc.time.value, lc.flux.value
    print(f"Total samples: {len(flux)}")

    # 2. TRANSIT DEFINITION (AU Mic b)
    # Plavchan et al. 2020: P = 8.4622, T0 = 1330.3905
    period, t0, duration = 8.4622, 1330.3905, 0.15
    global_mask = get_multi_transit_mask(time_arr, t0, period, duration)
    print(f"Transits masked: {np.sum(global_mask)} samples.")

    # 3. BASELINE (Savitzky-Golay)
    print("Calculating Baseline (Full Sector SG Flatten)...")
    start_b = time.time()
    baseline_lc = lc.flatten(window_length=101)
    b_flux = baseline_lc.flux.value
    time_b = time.time() - start_b

    # 4. DUAL-PARAMETER OPTIMIZATION (SSA)
    print("Running Global Dual-Parameter Optimization Sweep...")
    f_med = medfilt(flux, 501)
    f_clipped = np.clip(flux, None, f_med + 5 * np.median(np.abs(flux - f_med)))

    L_range = [200, 400, 600]
    comp_range = [4, 6, 8]

    best_mad, best_ssa_flux, best_L, best_C = np.inf, None, 0, 0
    start_s = time.time()

    for L in L_range:
        for C in comp_range:
            trend = optimized_ssa(f_clipped, L, n_components=C)
            detrended = flux / trend
            res_mad = np.median(np.abs(detrended[~global_mask] - np.median(detrended[~global_mask])))

            if res_mad < best_mad:
                best_mad, best_ssa_flux, best_L, best_C = res_mad, detrended, L, C

    time_s = time.time() - start_s

    # 5. FINAL CONCLUSION METRICS
    b_snr = calculate_snr(b_flux, global_mask)
    s_snr = calculate_snr(best_ssa_flux, global_mask)
    improvement = ((s_snr - b_snr) / abs(b_snr)) * 100

    print("\n--- FINAL CONCLUSION RESULTS ---")
    print(f"Baseline Global SNR: {b_snr:.4f}")
    print(f"Optimized SSA Global SNR: {s_snr:.4f}")
    print(f"Global SNR Improvement: {improvement:.2f}%")
    print(f"Optimal Params: L={best_L}, components={best_C}")
    print(f"Optimization Time: {time_s:.2f}s")

    # 6. FINAL DASHBOARD
    fig, ax = plt.subplots(4, 1, figsize=(15, 16))

    # Full Raw
    ax[0].plot(time_arr, flux, 'k.', markersize=0.5, alpha=0.3)
    ax[0].set_title("1. Full AU Mic Sector 1 Raw Flux", fontweight='bold')

    # Full Detrended Comparison
    ax[1].plot(time_arr, b_flux, 'r.', markersize=0.5, alpha=0.3, label='Baseline (SG)')
    ax[1].plot(time_arr, best_ssa_flux, 'g.', markersize=0.5, alpha=0.3, label='Novel (SSA)')
    ax[1].set_title("2. Global Detrending Comparison (Baseline vs. Novel)", fontweight='bold')
    ax[1].set_ylim(0.98, 1.02)
    ax[1].legend(loc='upper right')

    # Zoom on first transit
    z1 = (time_arr > t0 - 1.0) & (time_arr < t0 + 1.0)
    ax[2].plot(time_arr[z1], b_flux[z1], 'r.', markersize=2, alpha=0.6)
    ax[2].axvspan(t0-duration/2, t0+duration/2, color='blue', alpha=0.1)
    ax[2].set_title(f"3. Zoom: Transit 1 | SG SNR: {calculate_snr(b_flux[z1], global_mask[z1]):.2f}", fontweight='bold')
    ax[2].set_ylim(0.99, 1.01)

    # Zoom on second transit
    t1 = t0 + period
    z2 = (time_arr > t1 - 1.0) & (time_arr < t1 + 1.0)
    ax[3].plot(time_arr[z2], best_ssa_flux[z2], 'g.', markersize=2, alpha=0.6)
    ax[3].axvspan(t1-duration/2, t1+duration/2, color='blue', alpha=0.1)
    ax[3].set_title(f"4. Zoom: Transit 2 | SSA SNR: {calculate_snr(best_ssa_flux[z2], global_mask[z2]):.2f}", fontweight='bold')
    ax[3].set_ylim(0.99, 1.01)
    ax[3].set_xlabel("Time (BTJD)")

    plt.tight_layout()
    plt.savefig("au_mic_final_conclusion.png", dpi=200)
    print("\nFinal Conclusion Dashboard saved: au_mic_final_conclusion.png")

if __name__ == "__main__":
    main()
