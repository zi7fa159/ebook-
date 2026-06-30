from astroquery.gaia import Gaia

# List columns in the Gaia DR3 source table
tables = Gaia.load_tables(only_names=True)
for table in tables:
    if table.name == 'gaia_source':
        print(f"Columns in {table.name}:")
        # For Gaia, we can use metadata
        meta = Gaia.load_table('gaia_source')
        for col in meta.columns:
            print(col.name)
