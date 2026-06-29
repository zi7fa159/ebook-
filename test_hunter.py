import unittest
from astropy.coordinates import SkyCoord
from supernova_hunter import SupernovaHunter
import pandas as pd

class TestSupernovaHunter(unittest.TestCase):
    def setUp(self):
        self.hunter = SupernovaHunter()

    def test_select_target(self):
        target = self.hunter.select_target()
        self.assertIn('name', target)
        self.assertIn('coord', target)
        self.assertIsInstance(target['coord'], SkyCoord)

    def test_get_cutout_url_sci(self):
        coord = SkyCoord(ra=210.80242917, dec=54.34875, unit='deg')
        row = {
            'field': 792,
            'ccdid': 7,
            'qid': 2,
            'filtercode': 'zg',
            'filefracday': 20260205526782
        }
        url = self.hunter.get_cutout_url(row, coord, product='sci')
        self.assertIn("products/sci/2026/0205/526782/", url)
        self.assertIn("ztf_20260205526782_000792_zg_c07_o_q2_sciimg.fits", url)

    def test_get_cutout_url_ref(self):
        coord = SkyCoord(ra=210.80242917, dec=54.34875, unit='deg')
        row = {
            'field': 792,
            'ccdid': 7,
            'qid': 2,
            'filtercode': 'zg'
        }
        url = self.hunter.get_cutout_url(row, coord, product='ref')
        self.assertIn("products/ref/000/field000792/zg/ccd07/q2/", url)
        self.assertIn("ztf_000792_zg_c07_q2_refimg.fits", url)

if __name__ == '__main__':
    unittest.main()
