# Design Specification: Hierarchical Bit-Level Synchronizers (HBS)

## 1. Overview
Hierarchical Bit-Level Synchronizers (HBS) is a coding scheme designed to provide fast and robust resynchronization for DNA storage channels subject to insertions, deletions, and substitutions (IDS). It acts as a "synchronization layer" that sits between the raw bitstream and a standard outer error-correcting code (e.g., Reed-Solomon).

## 2. Encoding Architecture

### 2.1 Block Structure
Data is divided into **Segments** of length $L_s$. Each segment is further divided into **Micro-blocks** of length $L_m$.

### 2.2 Markers
1.  **Global Anchors (G-Anchors):**
    - Purpose: Establish a reliable "fix" on the sequence after significant drift.
    - Format: A fixed 8-nucleotide sequence (e.g., `ATGCATGC`) with high autocorrelation distance.
    - Placement: Every $L_g$ nucleotides.
2.  **Local Parity Markers (L-Parity):**
    - Purpose: Identify small shifts (±1 or ±2 nucleotides) within a segment.
    - Format: A 2-bit hash (1 nucleotide) of the preceding micro-block bits.
    - Placement: Every $L_m$ nucleotides.

### 2.3 Bit-to-Nucleotide Mapping
HBS uses a simple map (e.g., 00->A, 01->C, 10->G, 11->T) for the data bits, ensuring that markers are distinguishable or handled by the alignment logic.

## 3. Decoding Algorithm (Coarse-to-Fine)

### Step 1: Global Anchor Search
The decoder scans the received sequence $\mathbf{y}$ for the G-Anchors using a sliding window with a small Levenshtein distance threshold (allowing 1-2 errors in the anchor itself).
- **Output:** A list of "anchor points" in $\mathbf{y}$ that correspond to expected positions in $\mathbf{x}$.

### Step 2: Segment Isolation
For each pair of matched G-Anchors, the decoder extracts the intervening sequence (a "Segment"). Because indels are relatively rare, the length of this segment should be $L_s \pm \Delta$, where $\Delta$ is small.

### Step 3: Micro-block Alignment (Shift Detection)
Within each segment, the decoder iterates through micro-blocks. For each micro-block, it tests multiple shift hypotheses:
- **Hypothesis $H_0$:** No indel (offset 0).
- **Hypothesis $H_{+1}$:** One insertion (offset +1).
- **Hypothesis $H_{-1}$:** One deletion (offset -1).
- **Evaluation:** The decoder calculates the L-Parity for the data bits under each hypothesis and compares it to the observed L-Parity in $\mathbf{y}$.

### Step 4: Traceback & Reconstruction
The decoder uses a Viterbi-like dynamic programming approach over the hypotheses to find the most likely sequence of shifts that minimizes parity mismatches and length deviations.

### Step 5: Conversion to Erasures
If a micro-block cannot be reliably aligned or has too many parity mismatches, it is marked as an **Erasure** (all zeros or a special flag) to be handled by the outer RS code.

## 4. Key Parameters
- $L_g$ (Anchor Interval): 128 nucleotides.
- $L_m$ (Parity Interval): 16 nucleotides.
- Anchor Length: 8 nucleotides.
- Parity Length: 1 nucleotide.
- **Redundancy:** $\approx (8/128) + (1/16) = 6.25\% + 6.25\% = 12.5\%$.

## 5. Advantages over HEDGES
1.  **Deterministic Latency:** Unlike HEDGES' greedy search which can stall, HBS uses fixed-window alignment and local parity checks, leading to $O(n)$ decoding time.
2.  **Parallelizability:** Each segment between G-Anchors can be decoded independently.
3.  **Simplicity:** Does not require complex hashing or stack-based search.
