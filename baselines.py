import random

class WatermarkCodec:
    """
    A simple baseline codec that interleaves a watermark sequence for alignment.
    """
    def __init__(self, watermark="AT"):
        self.watermark = watermark
        self.nt_map = {0: 'A', 1: 'C', 2: 'G', 3: 'T'}
        self.rev_nt_map = {'A': 0, 'C': 1, 'G': 2, 'T': 3}

    def encode(self, bitstream):
        dna_output = []
        # Pad to even bits
        if len(bitstream) % 2 != 0:
            bitstream = bitstream + [0]

        for i in range(0, len(bitstream), 2):
            val = (bitstream[i] << 1) | bitstream[i+1]
            dna_output.append(self.nt_map[val])
            dna_output.append(self.watermark)
        return "".join(dna_output)

    def decode(self, dna_seq):
        """
        Decodes by looking for the watermark. If watermark is shifted, it tries to re-align.
        """
        decoded_bits = []
        i = 0
        while i < len(dna_seq):
            # Try to find data nucleotide followed by watermark
            # data_nt is at i, watermark is at i+1
            if i + len(self.watermark) < len(dna_seq):
                data_nt = dna_seq[i]
                # Check if watermark matches
                if dna_seq[i+1 : i+1+len(self.watermark)] == self.watermark:
                    # Match
                    val = self.rev_nt_map.get(data_nt, 0)
                    decoded_bits.extend([(val >> 1) & 1, val & 1])
                    i += 1 + len(self.watermark)
                else:
                    # Misalignment - search for next watermark
                    next_wm = dna_seq.find(self.watermark, i)
                    if next_wm != -1:
                        # Assume data_nt is just before the next watermark
                        data_nt = dna_seq[next_wm - 1]
                        val = self.rev_nt_map.get(data_nt, 0)
                        decoded_bits.extend([(val >> 1) & 1, val & 1])
                        i = next_wm + len(self.watermark)
                    else:
                        break
            else:
                break
        return decoded_bits

if __name__ == "__main__":
    codec = WatermarkCodec()
    original_bits = [random.randint(0, 1) for _ in range(100)]
    dna = codec.encode(original_bits)
    decoded = codec.decode(dna)
    print(f"Watermark Encoded length: {len(dna)}")
    print(f"Match: {original_bits == decoded[:len(original_bits)]}")
