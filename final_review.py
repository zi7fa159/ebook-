import pandas as pd
import numpy as np

def final_review():
    print("Performing final scientific review for discovery candidates...")
    df = pd.read_csv('validated_candidates.csv', dtype={'source_id': str})

    # 1. Check for valid photometry
    # We want candidates with either AllWISE or 2MASS data
    has_ir = df['w1mag'].notna() | df['jmag'].notna()

    # 2. Check for suspicious WISE flags (diffraction spikes, persistence, etc. in CC_FLAGS)
    # 0000 is clean. We allow some minor flags but prefer 0 in first two digits (W1, W2)
    def clean_wise(flags):
        if pd.isna(flags) or flags == 'None': return False
        return flags[0] == '0' and flags[1] == '0'

    wise_clean = df['wise_cc_flags'].apply(clean_wise)

    # 3. Check for reasonable colors for low-mass/cool objects
    # G-W2 > 3 is a good indicator for M dwarfs and later
    # J-K > 0.5 for M dwarfs (late M and L can be higher)
    red_color = (df['G-W2'] > 3.0) | (df['bp_rp'] > 2.0)

    # 4. Filter candidates
    # We are conservative: must have clean IR or very strong Gaia evidence
    final_candidates = df[has_ir & (wise_clean | df['wise_cc_flags'].isna()) & red_color].copy()

    # Calculate absolute G magnitude if parallax is reliable
    # Dist = 1000 / parallax (mas)
    # M_G = G + 5 + 5*log10(parallax/1000)
    final_candidates['dist_pc'] = 1000.0 / final_candidates['parallax']
    final_candidates['abs_g'] = final_candidates['phot_g_mean_mag'] + 5 + 5 * np.log10(final_candidates['parallax'] / 1000.0)

    print(f"Final review complete. {len(final_candidates)} candidates survive.")

    final_candidates.to_csv('final_discoveries.csv', index=False)

    # Summary of findings
    if len(final_candidates) > 0:
        print("\nSummary of Top Candidates:")
        for i, row in final_candidates.head(10).iterrows():
            print(f"Source ID: {row['source_id']}")
            print(f"  PM: {row['pm']:.1f} mas/yr, Parallax: {row['parallax']:.1f} mas")
            print(f"  G: {row['phot_g_mean_mag']:.2f}, G-W2: {row['G-W2']:.2f}")
            print(f"  Estimated Dist: {row['dist_pc']:.1f} pc, Abs G: {row['abs_g']:.2f}")
            print("-" * 30)
    else:
        print("No candidates survived the final review.")

if __name__ == "__main__":
    final_review()
