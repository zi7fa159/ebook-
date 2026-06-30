import pandas as pd
import numpy as np

# Select best candidates for final validation
df = pd.read_csv("candidates_strict_with_ir.csv")

# SIPS J0145-3729B looks like a genuine T dwarf candidate companion
# 1RXS J065612.9+684122 looks like a nearby UCD

final_selection = df[df['source_id'].isin([4962392573709868928, 1103299758870807936])]
final_selection.to_csv("final_confirmed_candidates.csv", index=False)
