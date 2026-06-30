import pandas as pd
from astroquery.simbad import Simbad
import time
from astropy.coordinates import SkyCoord
import astropy.units as u

def cross_match_simbad():
    print("Loading Gaia candidates...")
    # Load with source_id as string to avoid precision loss
    df = pd.read_csv('gaia_candidates.csv', dtype={'source_id': str})

    uncatalogued = []

    # Configure Simbad
    Simbad.add_votable_fields('ids')

    print(f"Checking {len(df)} candidates in SIMBAD...")

    for index, row in df.iterrows():
        source_id = row['source_id']
        ra = float(row['ra'])
        dec = float(row['dec'])

        designation = f"Gaia DR3 {source_id}"

        try:
            result = Simbad.query_object(designation)

            is_known = False
            if result is not None and len(result) > 0:
                ids = result['ids'][0]
                if isinstance(ids, bytes):
                    ids = ids.decode('utf-8')

                id_list = ids.split('|')
                # Filter out Gaia and TIC
                other_ids = [i for i in id_list if not i.startswith('Gaia') and not i.startswith('TIC')]

                if len(other_ids) > 0:
                    is_known = True

            if not is_known:
                # Region check for objects without Gaia DR3 designation in SIMBAD
                # Use a larger radius for HPM stars (1 arcmin)
                result_pos = Simbad.query_region(SkyCoord(ra=ra, dec=dec, unit=(u.deg, u.deg), frame='icrs'), radius='1m')
                if result_pos is not None and len(result_pos) > 0:
                    # If something is found, we consider it known (conservative approach)
                    is_known = True

            if not is_known:
                print(f"Potential uncatalogued candidate found: {designation}")
                uncatalogued.append(row)

        except Exception as e:
            print(f"Error querying {designation}: {e}")

        if (index + 1) % 50 == 0:
            print(f"Processed {index + 1} / {len(df)}...")
            time.sleep(1)

    uncatalogued_df = pd.DataFrame(uncatalogued)
    print(f"Found {len(uncatalogued_df)} uncatalogued candidates.")

    # Save with float_format to avoid scientific notation if any, though source_id is string now
    uncatalogued_df.to_csv('uncatalogued_candidates.csv', index=False)
    print("Results saved to uncatalogued_candidates.csv")

if __name__ == "__main__":
    cross_match_simbad()
