from astroquery.simbad import Simbad
def check_czernik():
    result = Simbad.query_object("Czernik 2")
    print(result)
if __name__ == "__main__":
    check_czernik()
