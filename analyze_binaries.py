import pandas as pd
import numpy as np
from scipy.spatial import KDTree
from astropy.coordinates import SkyCoord
import astropy.units as u

def find_wide_binaries(csv_file):
    df = pd.read_csv(csv_file)

    # Convert RA/Dec to Cartesian for fast proximity search
    coords = SkyCoord(ra=df['ra'].values*u.deg, dec=df['dec'].values*u.deg, distance=(1000/df['parallax'].values)*u.pc)
    cartesian_coords = coords.cartesian.xyz.value.T

    tree = KDTree(cartesian_coords)

    # Search for pairs within 1 pc (extreme wide binaries)
    max_sep_pc = 1.0
    pairs = tree.query_pairs(max_sep_pc)

    results = []
    for i, j in pairs:
        star1 = df.iloc[i]
        star2 = df.iloc[j]

        # Check proper motion consistency
        # Difference in proper motion should be small for a bound pair
        pm_diff = np.sqrt((star1['pmra'] - star2['pmra'])**2 + (star1['pmdec'] - star2['pmdec'])**2)

        # Parallax consistency
        plx_diff = abs(star1['parallax'] - star2['parallax'])
        plx_err = np.sqrt(star1['parallax_error']**2 + star2['parallax_error']**2)

        # Physical separation in pc
        dist_pc = np.sqrt(np.sum((cartesian_coords[i] - cartesian_coords[j])**2))

        # Simple binding criteria (can be refined)
        # 1. Delta PM < 2 km/s (approximate)
        # At 50pc, 1 km/s is 1/4.74 * 1/50 * 1000 ~ 4.2 mas/yr
        # Let's use a conservative 5 mas/yr for nearby stars
        if pm_diff < 5.0 and plx_diff < 3 * plx_err:
            results.append({
                'source_id1': star1['source_id'],
                'source_id2': star2['source_id'],
                'ra1': star1['ra'], 'dec1': star1['dec'],
                'ra2': star2['ra'], 'dec2': star2['dec'],
                'parallax1': star1['parallax'], 'parallax2': star2['parallax'],
                'pmra1': star1['pmra'], 'pmdec1': star1['pmdec'],
                'pmra2': star2['pmra'], 'pmdec2': star2['pmdec'],
                'pm_diff': pm_diff,
                'sep_pc': dist_pc,
                'g_mag1': star1['phot_g_mean_mag'],
                'g_mag2': star2['phot_g_mean_mag']
            })

    res_df = pd.DataFrame(results)
    res_df.to_csv('candidate_binaries.csv', index=False)
    print(f"Found {len(res_df)} candidate wide binaries.")

if __name__ == "__main__":
    find_wide_binaries('nearby_stars.csv')
