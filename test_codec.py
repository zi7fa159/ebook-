import random
from hbs_codec import HBSCodec
from simulator import simulate_ids_channel

def test_noiseless():
    codec = HBSCodec()
    for i in range(10):
        # 240 bits is a multiple of bits_per_global_block for default params
        length = 240 * random.randint(1, 4)
        bits = [random.randint(0, 1) for _ in range(length)]
        dna = codec.encode(bits)
        decoded = codec.decode(dna)
        if bits != decoded[:len(bits)]:
            print(f"Failed noiseless test {i}, length {length}")
            return False
    print("Noiseless tests passed.")
    return True

def test_noisy_recovery():
    codec = HBSCodec()
    bits = [random.randint(0, 1) for _ in range(240)]
    dna = codec.encode(bits)

    # Insert a single deletion in the data part (after the anchor)
    # The first 8 nts are anchor
    noisy_dna = dna[:10] + dna[11:]
    decoded = codec.decode(noisy_dna)

    # Trim decoded to bits length
    decoded = decoded[:len(bits)]

    matches = sum(1 for b1, b2 in zip(bits, decoded) if b1 == b2)
    accuracy = matches/len(bits)
    print(f"Single deletion accuracy: {accuracy:.2%}")
    return accuracy > 0.80

if __name__ == "__main__":
    test_noiseless()
    test_noisy_recovery()
