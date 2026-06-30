import pandas as pd
from astroquery.vizier import Vizier
from astropy.coordinates import SkyCoord
import astropy.units as u
import time

def validate_candidates():
    print("Loading uncatalogued candidates...")
    df = pd.read_csv('uncatalogued_candidates.csv', dtype={'source_id': str})

    validated = []

    # Configure Vizier
    v = Vizier(columns=['*'])

    print(f"Retrieving multi-wavelength data for {len(df)} candidates...")

    for index, row in df.iterrows():
        ra = float(row['ra'])
        dec = float(row['dec'])
        coord = SkyCoord(ra=ra, dec=dec, unit=(u.deg, u.deg), frame='icrs')

        entry = row.to_dict()

        # AllWISE
        try:
            res_wise = v.query_region(coord, radius='30s', catalog='II/328/allwise')
            if res_wise and len(res_wise) > 0:
                best_match = res_wise[0][0]
                entry['w1mag'] = best_match['W1mag']
                entry['w2mag'] = best_match['W2mag']
                entry['allwise_id'] = str(best_match['AllWISE'])
                entry['wise_cc_flags'] = str(best_match['ccf'])
            else:
                entry['w1mag'] = None
                entry['w2mag'] = None
                entry['allwise_id'] = None
                entry['wise_cc_flags'] = None
        except Exception as e:
            print(f"Error querying AllWISE for index {index}: {e}")

        # 2MASS
        try:
            res_2mass = v.query_region(coord, radius='30s', catalog='II/246/out')
            if res_2mass and len(res_2mass) > 0:
                best_match = res_2mass[0][0]
                entry['jmag'] = best_match['Jmag']
                entry['hmag'] = best_match['Hmag']
                entry['kmag'] = best_match['Kmag']
                entry['twomass_id'] = str(best_match['2MASS'])
            else:
                entry['jmag'] = None
                entry['hmag'] = None
                entry['kmag'] = None
                entry['twomass_id'] = None
        except Exception as e:
            print(f"Error querying 2MASS for index {index}: {e}")

        validated.append(entry)

        if (index + 1) % 10 == 0:
            print(f"Processed {index + 1} / {len(df)}...")
            time.sleep(0.5)

    validated_df = pd.DataFrame(validated)

    # Simple color calculations
    validated_df['G-W2'] = pd.to_numeric(validated_df['phot_g_mean_mag']) - pd.to_numeric(validated_df['w2mag'])
    validated_df['J-K'] = pd.to_numeric(validated_df['jmag']) - pd.to_numeric(validated_df['kmag'])

    print(f"Validation complete. Saving {len(validated_df)} results.")
    validated_df.to_csv('validated_candidates.csv', index=False)

if __name__ == "__main__":
    validate_candidates()
