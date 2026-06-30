import time
import random
from hbs_codec import HBSCodec
from baselines import WatermarkCodec
from simulator import simulate_ids_channel

def benchmark():
    error_rates = [0.01, 0.05, 0.10]
    num_trials = 20
    data_len = 2400 # bits

    hbs = HBSCodec()
    wm = WatermarkCodec()

    results = []

    for er in error_rates:
        # Equal probability for I, D, S
        pi = pd = ps = er / 3.0

        hbs_acc_sum = 0
        wm_acc_sum = 0
        hbs_time_sum = 0
        wm_time_sum = 0

        for _ in range(num_trials):
            bits = [random.randint(0, 1) for _ in range(data_len)]

            # Benchmark HBS
            t0 = time.time()
            hbs_dna = hbs.encode(bits)
            hbs_noisy = simulate_ids_channel(hbs_dna, pi, pd, ps)
            hbs_decoded = hbs.decode(hbs_noisy)
            hbs_time_sum += time.time() - t0

            # Calc Accuracy
            m = min(len(bits), len(hbs_decoded))
            hbs_matches = sum(1 for i in range(m) if bits[i] == hbs_decoded[i])
            hbs_acc_sum += hbs_matches / data_len

            # Benchmark Watermark
            t0 = time.time()
            wm_dna = wm.encode(bits)
            wm_noisy = simulate_ids_channel(wm_dna, pi, pd, ps)
            wm_decoded = wm.decode(wm_noisy)
            wm_time_sum += time.time() - t0

            m = min(len(bits), len(wm_decoded))
            wm_matches = sum(1 for i in range(m) if bits[i] == wm_decoded[i])
            wm_acc_sum += wm_matches / data_len

        res = {
            "error_rate": er,
            "hbs_accuracy": hbs_acc_sum / num_trials,
            "hbs_time": hbs_time_sum / num_trials,
            "wm_accuracy": wm_acc_sum / num_trials,
            "wm_time": wm_time_sum / num_trials,
            "hbs_redundancy": (len(hbs_dna)/ (data_len/2)) - 1,
            "wm_redundancy": (len(wm_dna)/ (data_len/2)) - 1
        }
        results.append(res)

    with open("benchmarks.txt", "w") as f:
        f.write(f"{'Error':<10} | {'HBS Acc':<10} | {'WM Acc':<10} | {'HBS Time':<10} | {'WM Time':<10} | {'HBS Red':<10}\n")
        f.write("-" * 75 + "\n")
        for r in results:
            f.write(f"{r['error_rate']:<10.2%} | {r['hbs_accuracy']:<10.2%} | {r['wm_accuracy']:<10.2%} | {r['hbs_time']:<10.4f} | {r['wm_time']:<10.4f} | {r['hbs_redundancy']:<10.2%}\n")

if __name__ == "__main__":
    benchmark()
