import numpy as np
from astroquery.imcce import Skybot
from astropy.coordinates import SkyCoord
from astropy.time import Time
import astropy.units as u
import re

def validate_discoveries(filename):
    with open(filename, 'r') as f:
        content = f.read()

    # Split by "Candidate discovery"
    entries = content.split("Candidate discovery at ")
    for entry in entries[1:]:
        lines = entry.strip().split('\n')
        header = lines[0]
        dets = []
        for line in lines[1:]:
            m = re.search(r"MJD=([\d.]+) RA=([\d.]+) Dec=([\d.]+)", line)
            if m:
                dets.append({'mjd': float(m.group(1)), 'ra': float(m.group(2)), 'dec': float(m.group(3))})

        if len(dets) < 3: continue

        # Check middle detection
        mid = dets[len(dets)//2]
        field = SkyCoord(mid['ra']*u.deg, mid['dec']*u.deg)
        epoch = Time(mid['mjd'], format='mjd')

        try:
            res = Skybot.cone_search(field, 20*u.arcsec, epoch)
            if len(res) > 0:
                # print(f"Entry at {mid['ra']}, {mid['dec']} is KNOWN: {res[0]['Name']}")
                pass
            else:
                print(f"STILL UNKNOWN: Entry at RA={mid['ra']}, Dec={mid['dec']}, MJD={mid['mjd']}")
                for d in dets:
                    print(f"  MJD={d['mjd']:.5f} RA={d['ra']:.5f} Dec={d['dec']:.5f}")
        except Exception as e:
            print(f"Skybot query failed: {e}")

if __name__ == "__main__":
    validate_discoveries("discoveries.log")
