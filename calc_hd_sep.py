import numpy as np
from astropy.coordinates import SkyCoord
import astropy.units as u

def calculate_separation_hd():
    # A: 42768532716776704 (Mag 6.9)
    # B: 42768945033709568 (Mag 15.3)
    # C: 42768532716777088 (Mag 15.0)

    # Coords from previous run
    # A: 53.2396, 16.6006 (Approx, need exact if possible but for 0.5pc scale this is fine)
    # Actually let's use the 3D distance

    # We found B-C is 0.645 pc.
    # What is A-C and A-B?

    # Star A (HD 21962)
    c_a = SkyCoord(ra=53.239606*u.deg, dec=16.6006*u.deg, distance=(1000/20.4)*u.pc) # Roughly 20.4 plx
    # Star B
    c_b = SkyCoord(ra=53.374932*u.deg, dec=16.597007*u.deg, distance=(1000/20.468)*u.pc)
    # Star C
    c_c = SkyCoord(ra=53.237946*u.deg, dec=16.597164*u.deg, distance=(1000/20.205)*u.pc)

    sep_ab = c_a.separation_3d(c_b).pc
    sep_ac = c_a.separation_3d(c_c).pc
    sep_bc = c_b.separation_3d(c_c).pc

    print(f"Separation A-B: {sep_ab:.4f} pc")
    print(f"Separation A-C: {sep_ac:.4f} pc")
    print(f"Separation B-C: {sep_bc:.4f} pc")

if __name__ == "__main__":
    calculate_separation_hd()
