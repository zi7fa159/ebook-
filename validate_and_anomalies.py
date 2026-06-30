import numpy as np
import pandas as pd
from galpy.orbit import Orbit
from galpy.potential import MWPotential2014
from astropy import units as u
from astropy.coordinates import SkyCoord, Galactocentric
import matplotlib.pyplot as plt

def validate_hvs():
    df = pd.read_csv('hvs_candidates.csv')
    if len(df) == 0:
        print("No candidates to validate.")
        return

    # Let's take the top 20 for detailed orbital integration
    top_df = df.head(20)

    print(f"Integrating orbits for {len(top_df)} candidates...")

    origins = []

    for i, row in top_df.iterrows():
        # Define orbit
        o = Orbit(vxvv=[row['ra']*u.deg, row['dec']*u.deg, (1000/row['parallax'])*u.pc,
                        row['pmra']*u.mas/u.yr, row['pmdec']*u.mas/u.yr, row['radial_velocity']*u.km/u.s],
                  radec=True)

        # Integrate backward for 100 Myr
        ts = np.linspace(0, -100, 1000) * u.Myr
        o.integrate(ts, MWPotential2014)

        # Check closest approach to Galactic Center (roughly [0,0,0] in Galactocentric)
        # o.x(ts), o.y(ts), o.z(ts) are in Galactocentric coordinates
        dist_gc = np.sqrt(o.x(ts)**2 + o.y(ts)**2 + o.z(ts)**2)
        min_dist = np.min(dist_gc)

        # Origin classification (simplified)
        if min_dist < 0.5: # 500 pc
            origin = "Galactic Center"
        elif min_dist < 5.0:
            origin = "Disk/Bulge"
        else:
            origin = "Halo/Extragalactic?"

        origins.append({'source_id': row['source_id'], 'min_dist_gc': min_dist, 'origin': origin})

    origins_df = pd.DataFrame(origins)
    print(origins_df)
    origins_df.to_csv('hvs_validation.csv', index=False)

def search_anomalies():
    from astroquery.gaia import Gaia
    print("Searching for high RUWE anomalies...")
    # RUWE > 3.0 and significant PM to avoid purely static artifacts
    query = """
    SELECT TOP 1000
        source_id, ra, dec, parallax, pmra, pmdec, ruwe, phot_g_mean_mag
    FROM gaiadr3.gaia_source
    WHERE ruwe > 5.0
      AND parallax_over_error > 10
      AND phot_g_mean_mag < 15
      AND sqrt(pmra*pmra + pmdec*pmdec) > 10
    """
    job = Gaia.launch_job(query)
    results = job.get_results()
    print(f"Found {len(results)} high RUWE anomalies.")
    results.to_pandas().to_csv('ruwe_anomalies.csv', index=False)

if __name__ == "__main__":
    validate_hvs()
    search_anomalies()
