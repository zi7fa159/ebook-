"""
AU Mic Transit Isolation Pipeline: Singular Spectrum Analysis (SSA) vs. Savitzky-Golay (SG)
Developed by Jules (Senior Computational Astrophysicist)

--- STEP 1: METRIC AND BASELINE DEFINITION ---
The mathematical gap in standard Box Least Squares (BLS) and Savitzky-Golay (SG) filtering
for young, active stars like AU Mic lies in their assumption of quasi-stationarity or
low-order polynomial trends. AU Mic exhibits high-amplitude rotational modulation
(starspots) and stochastic, high-frequency flaring events. SG filters with fixed
window lengths often 'over-fit' these features, injecting artifacts into the transit
feature, or 'under-fit', leaving high-frequency stellar noise that suppresses the
Transit Signal-to-Noise Ratio (SNR).

METRIC: Transit SNR = (Transit Depth) / (1.4826 * MAD_out_of_transit)
REDUCTION GOAL: Minimize Residual Root-Mean-Square (RMS) noise while preserving transit depth.

--- STEP 2: THE NOVEL ALGORITHMIC SCHEME ---
We implement Singular Spectrum Analysis (SSA), a non-parametric time-series decomposition
technique. SSA works by:
1. Embedding the 1D signal into a multi-dimensional Trajectory Matrix (Hankel matrix).
2. Decomposing the matrix via Singular Value Decomposition (SVD) to identify the
   principal components of the signal (trends, periodicities, and noise).
3. Reconstructing the signal using only the eigenvalues corresponding to the
   non-linear stellar activity (starspots and flares).
Unlike SG filters, SSA is data-adaptive, allowing it to capture the non-linear
morphology of stellar flares without the need for a pre-defined functional form.
"""

# Install dependencies for Google Colab
!pip install lightkurve --quiet

import lightkurve as lk
import numpy as np
import matplotlib.pyplot as plt
from scipy.linalg import svd, hankel
from scipy.signal import medfilt
import warnings

# Suppress lightkurve warnings
warnings.filterwarnings('ignore')

def calculate_snr(flux, transit_mask):
    """
    Calculates Transit SNR: (Depth / Robust Noise Floor)
    Using MAD (Median Absolute Deviation) for robustness against residual flares.
    """
    out_of_transit = flux[~transit_mask]
    in_transit = flux[transit_mask]
    if len(in_transit) == 0:
        return 0

    # Robust Depth calculation
    depth = np.median(out_of_transit) - np.median(in_transit)

    # Robust Standard Deviation (Scale = 1.4826 for Normal distribution consistency)
    mad = np.median(np.abs(out_of_transit - np.median(out_of_transit)))
    std_noise = 1.4826 * mad

    return depth / std_noise

def singular_spectrum_analysis(flux, window_length, n_components=4):
    """
    Performs SSA to extract non-parametric stellar trends.

    Physics/Math: The SVD of the Hankel matrix extracts the most significant
    eigenvectors. The first few components represent the high-variance
    rotational modulation and flare profiles.
    """
    N = len(flux)
    L = window_length  # Embedding dimension
    K = N - L + 1

    # 1. Embedding: Construct Trajectory Matrix (Hankel)
    X = hankel(flux[:L], flux[L-1:])

    # 2. SVD
    U, Sigma, VT = svd(X, full_matrices=False)

    # 3. Grouping & Reconstruction (Diagonal Averaging)
    # We take the first 'n_components' to model the stellar signal
    X_reconstructed = np.zeros_like(X)
    for i in range(n_components):
        X_reconstructed += Sigma[i] * np.outer(U[:, i], VT[i, :])

    # Diagonal averaging to transform back to 1D signal
    reconstructed_flux = np.zeros(N)
    for i in range(N):
        j_min = max(0, i - K + 1)
        j_max = min(i, L - 1)
        count = 0
        val = 0
        for j in range(j_min, j_max + 1):
            val += X_reconstructed[j, i - j]
            count += 1
        reconstructed_flux[i] = val / count

    return reconstructed_flux

def main():
    print("--- AU Mic Transit Isolation Pipeline (SSA-Optimized) ---")

    # --- STEP 3: COMPREHENSIVE PRODUCTION-READY CODE ---

    # 1. Data Ingestion & Target Selection
    print("Fetching AU Mic (TIC 441462348) data from TESS Sector 1...")
    search_result = lk.search_lightcurve("AU Mic", author="SPOC", mission="TESS", sector=1)
    lc = search_result.download().remove_nans().normalize()

    time = lc.time.value
    flux = lc.flux.value

    # 2. Baseline Control Pipeline
    print("Establishing Baseline Control (Savitzky-Golay Flatten)...")
    baseline_lc = lc.flatten(window_length=101)
    baseline_flux = baseline_lc.flux.value

    # 3. Novel Treatment Pipeline & Tenacity Loop
    print("Initiating Novel SSA Treatment & Optimization Loop...")

    # Targeted Transit (BTJD ~1330.39)
    transit_center = 1330.39
    transit_duration = 0.12 # days
    transit_mask = (time > transit_center - transit_duration/2) & (time < transit_center + transit_duration/2)

    # Pre-clip extreme flares (Top 1% outliers) to assist SVD convergence on trend
    f_med = medfilt(flux, 501)
    mad_val = np.median(np.abs(flux - f_med))
    flux_clipped = np.clip(flux, None, f_med + 5 * mad_val)

    best_rms = np.inf
    best_ssa_flux = None
    best_L = 0

    # Optimization Loop: Iterate across 5 window lengths
    L_candidates = [100, 200, 300, 400, 500]

    for L in L_candidates:
        # Extract stellar trend (Activity components = 5)
        trend = singular_spectrum_analysis(flux_clipped, window_length=L, n_components=5)
        detrended = flux / trend

        # Calculate Residual MAD (Metric for success)
        rms = np.median(np.abs(detrended[~transit_mask] - np.median(detrended[~transit_mask])))
        print(f"  Iteration L={L} | Residual MAD: {rms:.6f}")

        if rms < best_rms:
            best_rms = rms
            best_ssa_flux = detrended
            best_L = L

    print(f"Optimal Hyper-parameter Selected: L={best_L}")

    # 4. Analytics & Quantitative Benchmarking
    baseline_snr = calculate_snr(baseline_flux, transit_mask)
    ssa_snr = calculate_snr(best_ssa_flux, transit_mask)
    improvement = ((ssa_snr - baseline_snr) / abs(baseline_snr)) * 100

    print("\n--- QUANTITATIVE BENCHMARKS ---")
    print(f"Baseline Control SNR:   {baseline_snr:.4f}")
    print(f"Optimized Novel SSA SNR: {ssa_snr:.4f}")
    print(f"SNR Improvement:         {improvement:.2f}%")

    # 5. Visual Dashboard Generation
    fig, axes = plt.subplots(3, 1, figsize=(14, 12), sharex=False)

    # Top Panel: Raw Data
    axes[0].plot(time, flux, 'k.', markersize=0.5, alpha=0.5)
    axes[0].set_title("1. Raw AU Mic TESS Sector 1 (Extreme Activity)", fontweight='bold', fontsize=14)
    axes[0].set_ylabel("Normalized Flux")

    # Focus region for comparison
    zoom = (time > transit_center - 1.0) & (time < transit_center + 1.0)

    # Middle Panel: Baseline
    axes[1].plot(time[zoom], baseline_flux[zoom], 'r.', markersize=2, alpha=0.7)
    axes[1].axvspan(transit_center - transit_duration/2, transit_center + transit_duration/2,
                    color='blue', alpha=0.15, label='Expected Transit')
    axes[1].set_title(f"2. Baseline Control (Savitzky-Golay) | SNR: {baseline_snr:.2f}", fontweight='bold', fontsize=12)
    axes[1].set_ylabel("Relative Flux")
    axes[1].set_ylim(0.99, 1.01)
    axes[1].legend(loc='upper right')

    # Bottom Panel: SSA Optimized
    axes[2].plot(time[zoom], best_ssa_flux[zoom], 'g.', markersize=2, alpha=0.7)
    axes[2].axvspan(transit_center - transit_duration/2, transit_center + transit_duration/2,
                    color='blue', alpha=0.15)
    axes[2].set_title(f"3. Optimized Novel SSA (L={best_L}) | SNR: {ssa_snr:.2f}", fontweight='bold', fontsize=12)
    axes[2].set_ylabel("Relative Flux")
    axes[2].set_xlabel("Time (BTJD)")
    axes[2].set_ylim(0.99, 1.01)

    plt.tight_layout()
    plt.savefig("au_mic_dashboard.png", dpi=200)
    print("\nVisual Dashboard saved as 'au_mic_dashboard.png'")

if __name__ == "__main__":
    main()
