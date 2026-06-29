"""
Generalized Modular SSA-GP Hybrid Pipeline for Exoplanet Transit Isolation
Developed by Jules (Senior Computational Astrophysicist)
"""

# Install dependencies for Google Colab
!pip install lightkurve celerite2 --quiet

import lightkurve as lk
import numpy as np
import matplotlib.pyplot as plt
from scipy.linalg import hankel
from scipy.sparse.linalg import svds
from scipy.signal import medfilt
from scipy.optimize import minimize
from scipy.interpolate import interp1d
import celerite2
from celerite2 import terms
import warnings, time

# Suppress warnings
warnings.filterwarnings('ignore')

# --- CONFIGURATION REGISTRY ---
# TIC IDs corrected for AU Mic and TOI 837 (Sectors 10/11)
# V1298 Ori only has CDIPS/QLP (1800s) - skipping if no SPOC.
targets_config = {
    "AU_Mic": {
        "tic_id": "TIC 441420236",
        "sectors": [1],
        "rotation_period_days": 4.8,
        "planet_period_days": 8.46,
        "transit_t0": 1330.39
    },
    "DS_Tuc_A": {
        "tic_id": "TIC 410214986",
        "sectors": [1],
        "rotation_period_days": 2.9,
        "planet_period_days": 8.14,
        "transit_t0": 1332.32
    },
    "TOI_837": {
        "tic_id": "TIC 460205581",
        "sectors": [10],
        "rotation_period_days": 3.0,
        "planet_period_days": 8.32,
        "transit_t0": 1570.0  # Estimated S10 T0
    }
}

# --- SCIENTIFIC UTILITIES ---

def calculate_snr(flux, mask):
    """Robust Transit SNR: Depth / (1.4826 * MAD)."""
    out_of_transit = flux[~mask]
    in_transit = flux[mask]
    if len(in_transit) == 0: return 0
    depth = np.median(out_of_transit) - np.median(in_transit)
    noise = 1.4826 * np.median(np.abs(out_of_transit - np.median(out_of_transit)))
    return depth / noise if noise > 0 else 0

def get_transit_mask(time, t0, period, duration_days=0.15):
    """Generates a boolean mask for transit windows."""
    mask = np.zeros(len(time), dtype=bool)
    t_start, t_end = np.min(time), np.max(time)
    n_min, n_max = int((t_start - t0) / period) - 2, int((t_end - t0) / period) + 2
    for n in range(n_min, n_max + 1):
        t_trans = t0 + n * period
        mask |= (time > t_trans - duration_days/2) & (time < t_trans + duration_days/2)
    return mask

def asymmetric_flare_gate(flux, window_size=501):
    """Step 2: Dynamic Flare-Gate (+3 MAD clipping, negative untouched)."""
    f_med = medfilt(flux, kernel_size=window_size)
    mad = np.median(np.abs(flux - f_med))
    is_flare = (flux - f_med) > (3 * mad)
    clean_flux = np.copy(flux)
    if np.any(is_flare):
        x = np.arange(len(flux))
        interp_func = interp1d(x[~is_flare], flux[~is_flare], kind='linear', fill_value="extrapolate")
        clean_flux[is_flare] = interp_func(x[is_flare])
    return clean_flux

def run_ssa(flux, L, n_components=6):
    """Step 3: Dynamic SSA Detrending."""
    if L >= len(flux) // 2: L = len(flux) // 3
    if L < n_components: L = n_components + 1
    X = hankel(flux[:L], flux[L-1:])
    U, S, VT = svds(X, k=n_components)
    U, S, VT = U[:, ::-1], S[::-1], VT[::-1, :]
    Xr = np.zeros_like(X)
    for i in range(n_components):
        Xr += S[i] * np.outer(U[:, i], VT[i, :])
    j, k = np.indices(Xr.shape)
    indices = (j + k).ravel()
    trend = np.bincount(indices, weights=Xr.ravel()) / np.bincount(indices)
    return trend

def run_gp_denoise(x, y, yerr, mask):
    """Step 4: Dynamic GP Conditioning (Masked SHO Kernel)."""
    def neg_log_like(params, xi, yi, yeri):
        kernel = terms.SHOTerm(sigma=np.exp(params[0]), rho=np.exp(params[1]), Q=0.25)
        gp = celerite2.GaussianProcess(kernel, mean=0.0)
        gp.compute(xi, yerr=yeri)
        return -gp.log_likelihood(yi)

    init_p = np.array([np.log(np.std(y)), np.log(0.1)])
    soln = minimize(neg_log_like, init_p, method="L-BFGS-B", args=(x[~mask], y[~mask], yerr[~mask]))

    final_k = terms.SHOTerm(sigma=np.exp(soln.x[0]), rho=np.exp(soln.x[1]), Q=0.25)
    gp_final = celerite2.GaussianProcess(final_k, mean=0.0)
    gp_final.compute(x[~mask], yerr=yerr[~mask])
    mu = gp_final.predict(y[~mask], t=x)
    return mu

# --- MAIN PIPELINE ---

def process_target(name, config):
    tic_id = config['tic_id']
    results = []

    for sector in config['sectors']:
        print(f"\n--- Processing {name} (Sector {sector}) ---")

        search = lk.search_lightcurve(tic_id, author="SPOC", sector=sector)
        if len(search) == 0:
            print(f"Skipping {name} Sector {sector}: No SPOC data found.")
            continue
        lc = search.download().remove_nans().normalize()
        time_arr, flux, flux_err = lc.time.value, lc.flux.value, lc.flux_err.value
        dt = np.median(np.diff(time_arr))

        # Flare Gate
        flux_clean_ssa = asymmetric_flare_gate(flux)

        # Dynamic SSA
        L_samples = int(config['rotation_period_days'] / dt)
        ssa_trend = run_ssa(flux_clean_ssa, L_samples)
        ssa_detrended = flux / ssa_trend

        # Dynamic Mask & GP
        mask = get_transit_mask(time_arr, config['transit_t0'], config['planet_period_days'])
        x_norm = time_arr - np.min(time_arr)
        y_norm = ssa_detrended - 1.0
        yerr_norm = flux_err / ssa_trend
        gp_trend = run_gp_denoise(x_norm, y_norm, yerr_norm, mask)
        final_flux = ssa_detrended - gp_trend

        # Evaluation
        baseline_lc = lc.flatten(window_length=101)
        b_snr = calculate_snr(baseline_lc.flux.value, mask)
        h_snr = calculate_snr(final_flux, mask)
        delta = ((h_snr - b_snr) / abs(b_snr)) * 100 if b_snr != 0 else 0

        sector_res = {
            'target': name, 'sector': sector,
            'b_snr': b_snr, 'h_snr': h_snr, 'delta': delta,
            'time': time_arr, 'raw': flux, 'ssa_trend': ssa_trend,
            'gp_trend': gp_trend, 'final': final_flux, 'mask': mask,
            't0': config['transit_t0']
        }
        results.append(sector_res)
        render_diagnostic(sector_res)

    return results

def render_diagnostic(res):
    fig, ax = plt.subplots(3, 1, figsize=(12, 12))
    ax[0].plot(res['time'], res['raw'], 'k.', markersize=0.5, alpha=0.3, label='Raw Data')
    ax[0].plot(res['time'], res['ssa_trend'], 'r-', lw=1, label='SSA Stellar Trend')
    ax[0].set_title(f"{res['target']} (S{res['sector']}) - Raw Data & Adaptive SSA Model", fontweight='bold')
    ax[0].legend(loc='upper right')

    ssa_res = res['raw'] / res['ssa_trend'] - 1.0
    ax[1].plot(res['time'], ssa_res, 'k.', markersize=0.5, alpha=0.3)
    ax[1].plot(res['time'], res['gp_trend'], 'b-', lw=1, label='GP Correlated Noise')
    ax[1].set_title("Residual SSA Flux & Masked GP Prediction", fontweight='bold')
    ax[1].set_ylim(-0.01, 0.01)
    ax[1].legend(loc='upper right')

    # Try to find a transit to zoom in on
    t0_in_data = res['t0']
    n_offset = int((np.mean(res['time']) - t0_in_data) / (res['time'][1]-res['time'][0]) / 1000) # Dummy offset
    # Realistically just use t0 if it's in the sector
    zoom_center = t0_in_data
    while zoom_center < res['time'].min(): zoom_center += 8.0 # dummy period
    while zoom_center > res['time'].max(): zoom_center -= 8.0

    zoom = (res['time'] > zoom_center - 1.0) & (res['time'] < zoom_center + 1.0)
    if not np.any(zoom): zoom = np.ones(len(res['time']), dtype=bool)

    ax[2].plot(res['time'][zoom], res['final'][zoom], 'g.', markersize=2, alpha=0.6)
    ax[2].axvspan(zoom_center - 0.075, zoom_center + 0.075, color='orange', alpha=0.1, label='Target Transit')
    ax[2].set_title(f"Final Isolated Transit | SNR: {res['h_snr']:.2f}", fontweight='bold')
    ax[2].set_ylim(0.99, 1.01)
    ax[2].set_xlabel("Time (BTJD)")

    plt.tight_layout()
    plt.savefig(f"diagnostic_{res['target']}_S{res['sector']}.png", dpi=150)
    plt.close()

def main():
    print("--- ULTIMATE GENERALIZED SSA-GP PIPELINE ---")
    master_results = []
    for target, config in targets_config.items():
        try:
            target_results = process_target(target, config)
            master_results.extend(target_results)
        except Exception as e:
            print(f"Error processing {target}: {e}")

    print("\n" + "="*85)
    print(f"{'TARGET NAME':<15} | {'SEC':<4} | {'BASE SNR':<10} | {'HYBRID SNR':<12} | {'DELTA %':<10}")
    print("-" * 85)
    for r in master_results:
        print(f"{r['target']:<15} | {r['sector']:<4} | {r['b_snr']:<10.4f} | {r['h_snr']:<12.4f} | {r['delta']:<10.2f}%")
    print("="*85)

if __name__ == "__main__":
    main()
