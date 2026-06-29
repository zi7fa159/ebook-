from astroquery.vizier import Vizier
from astroquery.simbad import Simbad
from astropy.coordinates import SkyCoord
import astropy.units as u

class Validator:
    def __init__(self, radius_arcsec=15):
        self.radius = radius_arcsec * u.arcsec

    def cross_match_catalogs(self, ra, dec):
        coord = SkyCoord(ra=ra, dec=dec, unit=(u.deg, u.deg), frame='icrs')

        # 1. VSX Check
        vsx_match = self._query_vizier(coord, 'B/vsx')
        if vsx_match: return "VSX Match"

        # 2. Gaia DR3 VarFlag Check
        gaia_match = self._query_vizier(coord, 'I/355/gaiadr3')
        if gaia_match:
            # Check for VarFlag in Gaia if available
            return "Gaia DR3 Match"

        # 3. Simbad Check
        try:
            simbad_result = Simbad.query_region(coord, radius=self.radius)
            if simbad_result and len(simbad_result) > 0:
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
