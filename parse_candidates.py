import re
import json

def parse_final_candidates(filename):
    with open(filename, 'r') as f:
        content = f.read()

    entries = content.split("Candidate discovery at ")
    candidates = []
    seen_tracks = set()

    for entry in entries[1:]:
        lines = entry.strip().split('\n')
        dets = []
        for line in lines[1:]:
            m = re.search(r"MJD=([\d.]+) RA=([\d.]+) Dec=([\d.]+)", line)
            if m:
                dets.append({
                    'mjd': float(m.group(1)),
                    'ra': float(m.group(2)),
                    'dec': float(m.group(3))
                })

        if len(dets) >= 3:
            track_key = tuple(sorted([(d['mjd'], d['ra']) for d in dets]))
            if track_key not in seen_tracks:
                candidates.append(dets)
                seen_tracks.add(track_key)

    return candidates

if __name__ == "__main__":
    candidates = parse_final_candidates("discoveries.log")
    print(f"Parsed {len(candidates)} unique candidates from discoveries.log.")
    with open("parsed_candidates.json", "w") as f:
        json.dump(candidates, f, indent=2)
