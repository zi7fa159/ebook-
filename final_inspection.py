import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# Load UCD candidates
df = pd.read_csv("ucd_candidates.csv")

# Final filtering for most significant discoveries
# 1. Very high PM
# 2. Significant colors
# 3. Minimal previous studies (nbref <= 1)
discoveries = df[df['nbref'] <= 1].sort_values('pm', ascending=False)

print(f"Final inspection of {len(discoveries)} candidates...")

# Plot color-color diagram for context
plt.figure(figsize=(8, 6))
plt.scatter(df['W1_W2'], df['G_W2'], c='gray', alpha=0.5, label='All UCD candidates')
plt.scatter(discoveries['W1_W2'], discoveries['G_W2'], c='red', marker='*', s=100, label='Top Discovery Candidates')
plt.xlabel('W1 - W2')
plt.ylabel('G - W2')
plt.title('Color-Color Diagram')
plt.legend()
plt.savefig('plots/final_color_color.png')
plt.close()

# Calculate photometric distance estimate (very rough for UCDs)
# Absolute G mag for L dwarfs is roughly 15-20.
# distance = 10^((m - M + 5) / 5)
discoveries['dist_pc'] = 1 / (discoveries['parallax'] / 1000)

print(discoveries[['source_id', 'simbad_name', 'pm', 'W1_W2', 'G_W2', 'dist_pc', 'nbref']])
discoveries.to_csv("final_discovery_candidates.csv", index=False)
