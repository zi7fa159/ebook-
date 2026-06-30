import os
import subprocess

scripts = [
    "query_gaia.py",
    "cross_match_simbad.py",
    "get_ir_photometry.py",
    "final_inspection.py"
]

print("Running end-to-end pipeline verification...")

for script in scripts:
    print(f"Executing {script}...")
    try:
        # We skip get_ir_photometry if the results already exist to save time/bandwidth
        if script == "get_ir_photometry.py" and os.path.exists("ucd_candidates.csv"):
            print(f"  Skipping {script} as output exists.")
            continue

        result = subprocess.run(["python", script], capture_output=True, text=True, timeout=600)
        if result.returncode == 0:
            print(f"  {script} completed successfully.")
        else:
            print(f"  {script} failed with error:\n{result.stderr}")
    except subprocess.TimeoutExpired:
        print(f"  {script} timed out.")
    except Exception as e:
        print(f"  An error occurred: {e}")

print("Pipeline verification finished.")
