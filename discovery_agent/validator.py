from astroquery.vizier import Vizier
from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u

class Validator:
    def __init__(self, radius_arcsec=15):
        self.radius = radius_arcsec * u.arcsec

    def cross_match_catalogs(self, ra, dec):
        coord = SkyCoord(ra=ra, dec=dec, unit=(u.deg, u.deg), frame='icrs')

        # 1. VSX Check - THIS IS THE PRIMARY SOURCE FOR KNOWN VARIABLES
        vsx_match = self._query_vizier(coord, 'B/vsx')
        if vsx_match: return "VSX Match"

        # 2. Simbad Check
        try:
            simbad_result = Simbad.query_region(coord, radius=5*u.arcsec) # Tight radius
            if simbad_result and len(simbad_result) > 0:
                # Filter for variable star types in Simbad if possible,
                # but better to just flag it as potentially known.
                return "Simbad Match"
        except Exception:
            pass

        return None

    def _query_vizier(self, coord, catalog):
        v = Vizier(columns=['*'], catalog=[catalog])
        try:
            result = v.query_region(coord, radius=self.radius)
            if result and catalog in result.keys() and len(result[catalog]) > 0:
                return result[catalog]
        except Exception:
            pass
        return None

    def is_new_discovery(self, ra, dec):
        match = self.cross_match_catalogs(ra, dec)
        return match is None
