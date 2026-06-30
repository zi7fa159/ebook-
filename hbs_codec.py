import hashlib
import random

class HBSCodec:
    def __init__(self, anchor="ATGCATGC", micro_block_size=15, num_micro_blocks=8):
        self.anchor = anchor
        self.micro_block_size = micro_block_size
        self.num_micro_blocks = num_micro_blocks
        self.bits_per_nt = 2
        self.nt_map = {0: 'A', 1: 'C', 2: 'G', 3: 'T'}
        self.rev_nt_map = {'A': 0, 'C': 1, 'G': 2, 'T': 3}

    def _get_parity(self, bits):
        """Calculates a 2-bit parity from a list of bits."""
        s = sum(bits)
        return [ (s >> 1) & 1, s & 1 ]

    def encode(self, bitstream):
        bits_per_micro_block = self.micro_block_size * self.bits_per_nt
        bits_per_global_block = bits_per_micro_block * self.num_micro_blocks

        dna_output = []
        padding_needed = (bits_per_global_block - (len(bitstream) % bits_per_global_block)) % bits_per_global_block
        bitstream = list(bitstream) + [0] * padding_needed

        for i in range(0, len(bitstream), bits_per_global_block):
            dna_output.append(self.anchor)
            global_chunk = bitstream[i : i + bits_per_global_block]
            for j in range(0, len(global_chunk), bits_per_micro_block):
                micro_chunk = global_chunk[j : j + bits_per_micro_block]
                for k in range(0, len(micro_chunk), 2):
                    val = (micro_chunk[k] << 1) | micro_chunk[k+1]
                    dna_output.append(self.nt_map[val])
                p_bits = self._get_parity(micro_chunk)
                p_val = (p_bits[0] << 1) | p_bits[1]
                dna_output.append(self.nt_map[p_val])
        return "".join(dna_output)

    def _find_anchors(self, dna_seq):
        anchors = []
        for i in range(len(dna_seq) - len(self.anchor) + 1):
            window = dna_seq[i : i + len(self.anchor)]
            mismatches = sum(1 for a, b in zip(window, self.anchor) if a != b)
            if mismatches <= 1:
                anchors.append(i)

        if not anchors: return []
        expected_len = len(self.anchor) + self.num_micro_blocks * (self.micro_block_size + 1)
        filtered = [anchors[0]]
        for a in anchors[1:]:
            if a >= filtered[-1] + expected_len - 15:
                filtered.append(a)
        return filtered

    def decode(self, dna_seq):
        anchors = self._find_anchors(dna_seq)
        if not anchors: return []
        decoded_bits = []
        for i in range(len(anchors)):
            start_pos = anchors[i] + len(self.anchor)
            if i + 1 < len(anchors):
                end_pos = anchors[i+1]
            else:
                end_pos = start_pos + self.num_micro_blocks * (self.micro_block_size + 1)
                end_pos = min(end_pos, len(dna_seq))
            segment = dna_seq[start_pos:end_pos]
            decoded_bits.extend(self._decode_segment_dp(segment))
        return decoded_bits

    def _decode_segment_dp(self, segment):
        num_m = self.num_micro_blocks
        m_len = self.micro_block_size + 1
        max_drift = 12

        dp = {}
        dp[0] = {0: (0, None, [], 0)}

        for m_idx in range(num_m):
            next_dp = {}
            for drift, (score, prev_drift, p_bits, p_shift) in dp[m_idx].items():
                expected_start = m_idx * m_len + drift

                for shift in [-1, 0, 1, -2, 2]:
                    new_drift = drift + shift
                    if abs(new_drift) > max_drift: continue

                    pos = expected_start
                    if pos < 0 or pos + m_len > len(segment): continue

                    m_dna = segment[pos : pos + self.micro_block_size]
                    p_dna = segment[pos + self.micro_block_size]

                    try:
                        m_bits = []
                        for nt in m_dna:
                            val = self.rev_nt_map[nt]
                            m_bits.extend([(val >> 1) & 1, val & 1])

                        p_val_expected = self.rev_nt_map[p_dna]
                        p_bits_actual = self._get_parity(m_bits)
                        p_val_actual = (p_bits_actual[0] << 1) | p_bits_actual[1]

                        match_score = 10 if p_val_actual == p_val_expected else 0
                        total_score = score + match_score - abs(shift) * 2 - abs(new_drift) * 0.5

                        if new_drift not in next_dp or total_score > next_dp[new_drift][0]:
                            next_dp[new_drift] = (total_score, drift, m_bits, shift)
                    except KeyError:
                        continue
            if not next_dp:
                 next_dp[0] = (-1000, 0, [0]*(self.micro_block_size*2), 0)
            dp[m_idx + 1] = next_dp

        final_m = num_m
        if not dp[final_m]: return [0] * (num_m * self.micro_block_size * 2)
        best_drift = max(dp[final_m].keys(), key=lambda d: dp[final_m][d][0])

        all_bits = []
        curr_m = final_m
        curr_drift = best_drift
        while curr_m > 0:
            score, prev_drift, bits, shift = dp[curr_m][curr_drift]
            all_bits = bits + all_bits
            curr_drift = prev_drift
            curr_m -= 1
        return all_bits
