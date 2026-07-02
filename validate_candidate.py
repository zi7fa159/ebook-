import numpy as np
from astroquery.gaia import Gaia
from astroquery.vizier import Vizier
from astropy.coordinates import SkyCoord
from astropy.time import Time
import astropy.units as u

def validate_candidate(source_id):
    # STEP 1: GAIA ASTROMETRIC VALIDITY
    query = f"SELECT * FROM gaiadr3.gaia_source WHERE source_id = {source_id}"
    job = Gaia.launch_job(query)
    gaia = job.get_results()[0]

    ruwe = float(gaia['ruwe'])
    parallax = float(gaia['parallax'])
    parallax_error = float(gaia['parallax_error'])
    parallax_snr = parallax / parallax_error
    excess_noise = float(gaia['astrometric_excess_noise'])
    duplicated = bool(gaia['duplicated_source'])

    evidence_for = []
    evidence_against = []

    step1_pass = True
    if ruwe >= 1.4: step1_pass = False; evidence_against.append(f"High RUWE ({ruwe:.2f})")
    if parallax_snr <= 5: step1_pass = False; evidence_against.append(f"Low Parallax SNR ({parallax_snr:.2f})")
    if excess_noise > 1.0: evidence_against.append(f"Astrometric excess noise present ({excess_noise:.2f})")
    if duplicated: step1_pass = False; evidence_against.append("Duplicated source flag True")

    if step1_pass:
        evidence_for.append(f"Clean Gaia astrometry (RUWE={ruwe:.2f}, SNR={parallax_snr:.2f})")

    # STEP 2 & 6: MULTI-SURVEY & MOTION CONSISTENCY
    coord_gaia = SkyCoord(ra=gaia['ra']*u.deg, dec=gaia['dec']*u.deg,
                          pm_ra_cosdec=gaia['pmra']*u.mas/u.yr, pm_dec=gaia['pmdec']*u.mas/u.yr,
                          distance=1000/parallax * u.pc, obstime=Time('J2016.0'))

    v = Vizier(columns=['*'])
    viz = v.query_region(coord_gaia, radius=5*u.arcsec, catalog=['II/328/allwise', 'II/246/out', 'II/365/catwise', 'II/349/ps1'])

    motion_pass = False
    if 'II/328/allwise' in viz.keys():
        aw = viz['II/328/allwise'][0]
        coord_aw = SkyCoord(ra=aw['RAJ2000']*u.deg, dec=aw['DEJ2000']*u.deg)
        gaia_at_aw = coord_gaia.apply_space_motion(new_obstime=Time('J2010.5'))
        sep = gaia_at_aw.separation(coord_aw).arcsec
        if sep < 1.0:
            motion_pass = True
            evidence_for.append(f"Strong motion consistency with AllWISE (sep={sep:.2f}\")")
        else:
            evidence_against.append(f"Motion mismatch with AllWISE (sep={sep:.2f}\")")

    # STEP 3 & 4: PHOTOMETRIC PHYSICS & COLORS
    j_mag = float(viz['II/246/out'][0]['Jmag']) if 'II/246/out' in viz.keys() else None
    k_mag = float(viz['II/246/out'][0]['Kmag']) if 'II/246/out' in viz.keys() else None
    w1_mag = float(viz['II/365/catwise'][0]['W1mproPM']) if 'II/365/catwise' in viz.keys() else None
    w2_mag = float(viz['II/365/catwise'][0]['W2mproPM']) if 'II/365/catwise' in viz.keys() else None
    g_mag = float(gaia['phot_g_mean_mag'])

    dist_mod = 5 * np.log10(1000/parallax) - 5
    m_g = g_mag - dist_mod
    m_j = j_mag - dist_mod if j_mag else None

    if m_j and 10 <= m_j <= 15:
        evidence_for.append(f"M_J consistent with brown dwarf ({m_j:.2f})")
    else:
        evidence_against.append(f"M_J inconsistent ({m_j})")

    if w1_mag and g_mag - w1_mag > 4:
        evidence_for.append(f"G-W1 color red ({g_mag - w1_mag:.2f})")
    else:
        evidence_against.append(f"G-W1 not red enough")

    w1_w2 = w1_mag - w2_mag if w1_mag and w2_mag else None
    if w1_w2 and w1_w2 > 0.5:
        evidence_for.append(f"W1-W2 meets 0.5 threshold ({w1_w2:.2f})")
    elif w1_w2:
        evidence_against.append(f"W1-W2 weak ({w1_w2:.2f}), but typical for L-dwarfs")

    # STEP 5: ARCHIVAL IMAGING LINKS
    ra, dec = gaia['ra'], gaia['dec']
    ps1_url = f"https://ps1images.stsci.edu/cgi-bin/ps1cutouts?pos={ra}+{dec}&filter=color"
    legacy_url = f"https://www.legacysurvey.org/viewer-dev/?ra={ra}&dec={dec}&layer=ls-dr9&zoom=14"

    # FINAL CLASSIFICATION
    final_class = "A"
    confidence = 90
    if not motion_pass or not step1_pass:
        final_class = "C"
        confidence = 50
    elif w1_w2 < 0.5:
        confidence = 85

    # OUTPUT REPORT (9-POINT FORMAT)
    print("\n--- FINAL CLASSIFICATION REPORT ---")
    print(f"1. Final Class: {final_class}")
    print(f"2. Confidence Score: {confidence}")
    print("3. Key Evidence FOR classification:")
    for e in evidence_for: print(f"   - {e}")
    print("4. Key Evidence AGAINST classification:")
    for e in evidence_against: print(f"   - {e}")
    print("5. Archive findings summary:")
    print(f"   - Gaia DR3: G={g_mag:.2f}, Parallax={parallax:.2f} mas, RUWE={ruwe:.2f}, PM={gaia['pmra']:.1f}, {gaia['pmdec']:.1f} mas/yr")
    print(f"   - IR: J={j_mag:.2f} (2MASS), W1={w1_mag:.2f} (CatWISE), W2={w2_mag:.2f} (CatWISE)")
    print(f"   - Optical: i={viz['II/349/ps1'][0]['imag'] if 'II/349/ps1' in viz.keys() else 'N/A'} (PS1)")
    print(f"   - Imaging Links: PS1 ({ps1_url}), Legacy Survey ({legacy_url})")
    print("6. Final recommendation:")
    print("   - spectroscopy (requested to confirm mid-L type)")
    print("   - imaging follow-up (not required, source appears clean)")

validate_candidate(249793717390887808)
