import numpy as np
from astropy.table import Table
from astroquery.gaia import Gaia
import astropy.units as u
from astropy.coordinates import SkyCoord, Galactocentric
import os

def search_hvs():
    # Looking for high heliocentric tangential velocity first.
    # v_tan = 4.74 * pm / parallax.
    # pm = sqrt(pmra^2 + pmdec^2)
    # v_tan > 500 => pm/parallax > 105.4

    query = """
    SELECT TOP 5000
        source_id, ra, dec, parallax, parallax_error, pmra, pmra_error, pmdec, pmdec_error,
        radial_velocity, radial_velocity_error, ruwe, phot_g_mean_mag
    FROM gaiadr3.gaia_source
    WHERE radial_velocity IS NOT NULL
      AND parallax_over_error > 5
      AND ruwe < 1.4
      AND radial_velocity_error < 10
      AND (pmra*pmra + pmdec*pmdec) > 25 * parallax * parallax
    """
    # pm^2 > 25*plx^2  => pm/plx > 5 => v_tan > 4.74 * 5 = 23.7 km/s (Too low, let's increase)

    # Try pm/plx > 100 => v_tan > 474 km/s
    query = """
    SELECT TOP 5000
        source_id, ra, dec, parallax, parallax_error, pmra, pmra_error, pmdec, pmdec_error,
        radial_velocity, radial_velocity_error, ruwe, phot_g_mean_mag
    FROM gaiadr3.gaia_source
    WHERE radial_velocity IS NOT NULL
      AND parallax_over_error > 8
      AND ruwe < 1.2
      AND radial_velocity_error < 5
      AND (pmra*pmra + pmdec*pmdec) > 10000 * parallax * parallax
    """

    print("Running Gaia query...")
    job = Gaia.launch_job(query)
    results = job.get_results()
    print(f"Found {len(results)} raw candidates.")

    if len(results) == 0:
        # Relaxing criteria
        print("No candidates found, relaxing criteria...")
        query = """
        SELECT TOP 2000
            source_id, ra, dec, parallax, parallax_error, pmra, pmra_error, pmdec, pmdec_error,
            radial_velocity, radial_velocity_error, ruwe, phot_g_mean_mag
        FROM gaiadr3.gaia_source
        WHERE radial_velocity IS NOT NULL
          AND parallax_over_error > 5
          AND (pmra*pmra + pmdec*pmdec) > 2500 * parallax * parallax
        """
        job = Gaia.launch_job(query)
        results = job.get_results()
        print(f"Found {len(results)} raw candidates after relaxation.")

    if len(results) == 0:
        return

    # Calculate 3D Galactic Velocities
    print("Calculating Galactic velocities...")
    results = results[results['parallax'] > 0]

    coords = SkyCoord(ra=results['ra'].data * u.deg,
                      dec=results['dec'].data * u.deg,
                      distance=(1000.0/results['parallax'].data) * u.pc,
                      pm_ra_cosdec=results['pmra'].data * u.mas/u.yr,
                      pm_dec=results['pmdec'].data * u.mas/u.yr,
                      radial_velocity=results['radial_velocity'].data * u.km/u.s,
                      frame='icrs')

    galcen = coords.transform_to(Galactocentric())

    v_x = galcen.v_x.to(u.km/u.s).value
    v_y = galcen.v_y.to(u.km/u.s).value
    v_z = galcen.v_z.to(u.km/u.s).value
    v_gc = np.sqrt(v_x**2 + v_y**2 + v_z**2)

    results['v_gc'] = v_gc

    hvs_candidates = results[results['v_gc'] > 450]
    hvs_candidates.sort('v_gc', reverse=True)

    print(f"Found {len(hvs_candidates)} stars with V_gc > 450 km/s.")
    hvs_candidates.write('hvs_candidates.csv', format='csv', overwrite=True)

    if len(hvs_candidates) > 0:
        print("Top 10 candidates:")
        print(hvs_candidates[:10]['source_id', 'v_gc', 'phot_g_mean_mag'])

if __name__ == "__main__":
    search_hvs()
