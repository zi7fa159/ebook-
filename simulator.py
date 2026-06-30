import random

def simulate_ids_channel(sequence, pi, pd, ps):
    """
    Simulates a DNA channel with Insertions, Deletions, and Substitutions.

    Args:
        sequence (str): Input nucleotide sequence (A, C, G, T).
        pi (float): Probability of insertion.
        pd (float): Probability of deletion.
        ps (float): Probability of substitution.

    Returns:
        str: Noisy sequence.
    """
    nucleotides = ['A', 'C', 'G', 'T']
    output = []
    for nt in sequence:
        r = random.random()
        if r < pd:
            # Deletion: don't append anything
            pass
        elif r < pd + ps:
            # Substitution: append a different nucleotide
            others = [n for n in nucleotides if n != nt]
            output.append(random.choice(others))
        else:
            # Correct: append the nucleotide
            output.append(nt)

        # Insertion: independently decide if an insertion occurs after this position
        if random.random() < pi:
            output.append(random.choice(nucleotides))

    return "".join(output)

def levenshtein_distance(s1, s2):
    """Calculates the Levenshtein distance between two strings."""
    if len(s1) < len(s2):
        return levenshtein_distance(s2, s1)

    if len(s2) == 0:
        return len(s1)

    previous_row = range(len(s2) + 1)
    for i, c1 in enumerate(s1):
        current_row = [i + 1]
        for j, c2 in enumerate(s2):
            insertions = previous_row[j + 1] + 1
            deletions = current_row[j] + 1
            substitutions = previous_row[j] + (c1 != c2)
            current_row.append(min(insertions, deletions, substitutions))
        previous_row = current_row

    return previous_row[-1]

if __name__ == "__main__":
    test_seq = "ATGC" * 25 # 100 nts
    pi, pd, ps = 0.02, 0.02, 0.01 # 5% total error
    noisy = simulate_ids_channel(test_seq, pi, pd, ps)
    dist = levenshtein_distance(test_seq, noisy)
    print(f"Original length: {len(test_seq)}")
    print(f"Noisy length: {len(noisy)}")
    print(f"Edit Distance: {dist}")
    print(f"Error Rate: {dist/len(test_seq):.2%}")
