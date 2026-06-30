import pandas as pd
import numpy as np

df = pd.read_csv("candidates_strict_with_ir.csv")
cand = df[df['source_id'] == 4962392573709868928].iloc[0]
print("SIPS J0145-3729B:")
print(f"M_G: {cand['M_G']}")
print(f"W1-W2: {cand['W1_W2']}")
print(f"Parallax: {cand['parallax']}")
print(f"PM: {cand['pm']}")

cand2 = df[df['source_id'] == 1103299758870807936].iloc[0]
print("\n1RXS J065612.9+684122:")
print(f"M_G: {cand2['M_G']}")
print(f"W1-W2: {cand2['W1_W2']}")
print(f"Parallax: {cand2['parallax']}")
print(f"PM: {cand2['pm']}")
