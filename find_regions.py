import numpy as np
from astroquery.ipac.irsa import Irsa
from astropy.table import vstack
import time

def find_best_regions():
    # Query for image metadata to find clusters of observations
    query = """
    SELECT ra, dec, obsmjd
    FROM ptf.ptf_procimg
    WHERE ra BETWEEN 180.0 AND 210.0
      AND dec BETWEEN -10.0 AND 10.0
      AND obsmjd > 55000
    ORDER BY obsmjd
    """
    print("Querying for image metadata...")
    res = Irsa.query_tap(query).to_table()
    print(f"Found {len(res)} images.")

    # Group by night and field
    nights = {}
    for row in res:
        night = int(row['obsmjd'])
        # Round RA/Dec to 0.1 deg to group nearby fields
        field = (round(row['ra'], 1), round(row['dec'], 1))
        key = (night, field)
        if key not in nights:
            nights[key] = []
        nights[key].append(row['obsmjd'])

    eligible = []
    for (night, field), times in nights.items():
        if len(times) >= 3:
            times.sort()
            if (times[-1] - times[0]) * 1440.0 > 15.0: # at least 15 mins
                eligible.append({'ra': field[0], 'dec': field[1], 'night': night, 'count': len(times), 'times': times})

    # Sort by count descending
    eligible.sort(key=lambda x: x['count'], reverse=True)
    return eligible

if __name__ == "__main__":
    best = find_best_regions()
    print(f"Found {len(best)} eligible region-nights.")
    for b in best[:20]:
        print(f"RA={b['ra']}, Dec={b['dec']}, Night={b['night']}, Count={b['count']}")
