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
    """Vectorized diagonal averaging for SSA reconstruction."""
    L, K = X.shape
    j, k = np.indices(X.shape)
    indices = (j + k).ravel()
    sums = np.bincount(indices, weights=X.ravel())
    counts = np.bincount(indices)
    return sums / counts

def optimized_ssa(flux, L, n_components=8):
    """Optimized SSA using Sparse SVD."""
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
    t_start, t_end = np.min(time), np.max(time)
    t = t0
    while t >= t_start:
        mask |= (time > t - duration/2) & (time < t + duration/2)
        t -= period
    t = t0 + period
    while t <= t_end:
        mask |= (time > t - duration/2) & (time < t + duration/2)
        t += period
    return mask

def main():
    print("--- AU Mic ULTIMATE HYBRID SSA-GP PIPELINE ---")

    # 1. DATA INGESTION
    print("Downloading Full AU Mic TESS Sector 1...")
    search = lk.search_lightcurve("AU Mic", author="SPOC", sector=1)
    lc = search.download().remove_nans().normalize()
    time_arr, flux, flux_err = lc.time.value, lc.flux.value, lc.flux_err.value

    # 2. TRANSIT DEFINITION
    period, t0, duration = 8.4622, 1330.3905, 0.15
    global_mask = get_multi_transit_mask(time_arr, t0, period, duration)

    # 3. BASELINE (SG)
    print("Processing Baseline (Savitzky-Golay)...")
    baseline_lc = lc.flatten(window_length=101)
    b_flux = baseline_lc.flux.value

    # 4. SSA OPTIMIZATION LOOP (THE TENACITY LOOP)
    print("Running SSA Tenacity Loop (Hyper-parameter Optimization)...")
    f_med = medfilt(flux, 501)
    f_clipped = np.clip(flux, None, f_med + 5 * np.median(np.abs(flux - f_med)))

    L_candidates = [100, 200, 300, 400, 500]
    best_ssa_mad = np.inf
    best_ssa_detrended = None
    best_L = 0

    for L in L_candidates:
        trend = optimized_ssa(f_clipped, L, n_components=6)
        detrended = flux / trend
        mad = np.median(np.abs(detrended[~global_mask] - np.median(detrended[~global_mask])))
        print(f"  L={L} -> MAD: {mad:.6f}")
        if mad < best_ssa_mad:
            best_ssa_mad, best_ssa_detrended, best_L = mad, detrended, L

    print(f"Optimal SSA window length: {best_L}")

    # 5. HYBRID STAGE 2: GAUSSIAN PROCESS (Fine-Scale Denoising)
    print("Stage 2: Gaussian Process (SHO Kernel) Residual Modeling...")
    x = time_arr - np.min(time_arr)
    y = best_ssa_detrended - 1.0
    yerr = flux_err / (flux / best_ssa_detrended)

    # Manual optimization of GP parameters
    def neg_log_like(params, x_in, y_in, yerr_in):
        # params: [log_sigma, log_rho, log_Q]
        kernel = terms.SHOTerm(sigma=np.exp(params[0]), rho=np.exp(params[1]), Q=np.exp(params[2]))
        gp = celerite2.GaussianProcess(kernel, mean=0.0)
        gp.compute(x_in, yerr=yerr_in)
        return -gp.log_likelihood(y_in)

    initial_params = np.array([np.log(np.std(y)), np.log(0.5), np.log(0.25)])
    soln = minimize(neg_log_like, initial_params, method="L-BFGS-B", args=(x[~global_mask], y[~global_mask], yerr[~global_mask]))

    # Final GP with optimized parameters
    final_kernel = terms.SHOTerm(sigma=np.exp(soln.x[0]), rho=np.exp(soln.x[1]), Q=np.exp(soln.x[2]))
    gp_final = celerite2.GaussianProcess(final_kernel, mean=0.0)
    gp_final.compute(x[~global_mask], yerr=yerr[~global_mask])
    mu = gp_final.predict(y[~global_mask], t=x)

    hybrid_detrended = best_ssa_detrended - mu

    # 6. FINAL BENCHMARKING
    b_snr = calculate_snr(b_flux, global_mask)
    s_snr = calculate_snr(best_ssa_detrended, global_mask)
    h_snr = calculate_snr(hybrid_detrended, global_mask)

    print("\n--- FINAL QUANTITATIVE BENCHMARKS ---")
    print(f"Baseline (SG) SNR:  {b_snr:.4f}")
    print(f"Optimized SSA SNR:  {s_snr:.4f}")
    print(f"Ultimate Hybrid SNR: {h_snr:.4f}")
    print(f"Total Improvement:   {((h_snr - b_snr)/abs(b_snr))*100:.2f}%")

    # 7. VISUAL DASHBOARD
    fig, axes = plt.subplots(4, 1, figsize=(15, 18))
    axes[0].plot(time_arr, flux, 'k.', markersize=0.5, alpha=0.3)
    axes[0].set_title("1. Raw AU Mic (TESS Sector 1)", fontweight='bold')

    axes[1].plot(time_arr, b_flux, 'r.', markersize=0.5, alpha=0.2, label='Baseline')
    axes[1].plot(time_arr, hybrid_detrended, 'b.', markersize=0.5, alpha=0.4, label='Hybrid SSA-GP')
    axes[1].set_title("2. Global Detrending Comparison", fontweight='bold')
    axes[1].set_ylim(0.985, 1.015)
    axes[1].legend(loc='upper right')

    z1 = (time_arr > t0 - 0.8) & (time_arr < t0 + 0.8)
    axes[2].plot(time_arr[z1], best_ssa_detrended[z1], 'g.', markersize=2, alpha=0.3, label='SSA Only')
    axes[2].plot(time_arr[z1], hybrid_detrended[z1], 'b.', markersize=2, alpha=0.8, label='Hybrid SSA-GP')
    axes[2].axvspan(t0-duration/2, t0+duration/2, color='orange', alpha=0.1, label='Transit')
    axes[2].set_title(f"3. Zoom: Transit 1 | Hybrid SNR: {calculate_snr(hybrid_detrended[z1], global_mask[z1]):.2f}", fontweight='bold')
    axes[2].set_ylim(0.99, 1.01)
    axes[2].legend(loc='upper right')

    t1 = t0 + period
    z2 = (time_arr > t1 - 0.8) & (time_arr < t1 + 0.8)
    axes[3].plot(time_arr[z2], hybrid_detrended[z2], 'b.', markersize=2, alpha=0.8)
    axes[3].axvspan(t1-duration/2, t1+duration/2, color='orange', alpha=0.1)
    axes[3].set_title(f"4. Zoom: Transit 2 | Hybrid SNR: {calculate_snr(hybrid_detrended[z2], global_mask[z2]):.2f}", fontweight='bold')
    axes[3].set_ylim(0.99, 1.01)
    axes[3].set_xlabel("Time (BTJD)")

    plt.tight_layout()
    plt.savefig("au_mic_ultimate_conclusion.png", dpi=200)
    print("\nDashboard saved: au_mic_ultimate_conclusion.png")

if __name__ == "__main__":
    main()
