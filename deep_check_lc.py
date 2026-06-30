import pandas as pd
import lightkurve as lk
import matplotlib.pyplot as plt
import os
from astropy.coordinates import SkyCoord
import astropy.units as u

# Try a larger radius for LP 762-1
ra = 24.37258079541334
dec = -28.94196160161405

print("Retrieving TESS light curve for LP 762-1 (larger radius)...")

try:
    search_result = lk.search_lightcurve(f"{ra} {dec}", mission='TESS', radius=120*u.arcsec)
    print(f"Found {len(search_result)} datasets.")
    if len(search_result) > 0:
        print(search_result)
except Exception as e:
    print(f"Error: {e}")
