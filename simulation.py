import numpy as np

class WirelessNetwork:
    def __init__(self, N, K, p_success):
        self.N = N
        self.K = K
        self.p_success = p_success
        self.ages = np.ones(N, dtype=int)

    def step(self, scheduled_indices):
        for i in range(self.N):
            if i in scheduled_indices:
                if np.random.rand() < self.p_success[i]:
                    self.ages[i] = 1
                else:
                    self.ages[i] += 1
            else:
                self.ages[i] += 1
        return self.ages.copy()

def whittle_index(h, p):
    return (h * (p * h + 2 - p)) / 2.0

def run_simulation(N, K, p_success, T=10000):
    policies = ["Random", "Greedy", "Max-Weight", "Whittle"]
    results = {pol: 0 for pol in policies}

    for pol in policies:
        env = WirelessNetwork(N, K, p_success)
        total_age = 0

        for t in range(T):
            if pol == "Random":
                scheduled = np.random.choice(N, K, replace=False)
            elif pol == "Greedy":
                scheduled = np.argsort(env.ages)[::-1][:K]
            elif pol == "Max-Weight":
                # Age * Success Probability
                weights = env.ages * p_success
                scheduled = np.argsort(weights)[::-1][:K]
            elif pol == "Whittle":
                weights = [whittle_index(env.ages[i], p_success[i]) for i in range(N)]
                scheduled = np.argsort(weights)[::-1][:K]

            ages = env.step(scheduled)
            total_age += np.mean(ages)

        results[pol] = total_age / T

    return results

if __name__ == "__main__":
    N = 10
    K = 3
    # Heterogeneous success probabilities
    p_success = np.linspace(0.1, 0.9, N)

    print(f"Simulation: N={N}, K={K}")
    print(f"Success probabilities: {p_success}")

    results = run_simulation(N, K, p_success)

    with open("simulation_results.txt", "w") as f:
        f.write(f"Simulation: N={N}, K={K}\n")
        f.write(f"Success probabilities: {p_success}\n\n")
        for pol, avg_age in results.items():
            line = f"{pol}: Average Age = {avg_age:.4f}"
            print(line)
            f.write(line + "\n")
