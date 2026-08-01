# fn_cle_ml_experiment.py
import random
import math
import json

def generate_sensor_anomaly_data():
    """Generate mock motor vibration data: 3-axis acceleration (frequency components).
    Normal class (0): centered around [1.0, 0.5, 0.2]
    Anomaly class (1): centered around [2.5, 1.8, 1.2]
    """
    random.seed(42)
    num_samples = 1000
    data = []
    labels = []

    for _ in range(num_samples):
        label = 1 if random.random() < 0.2 else 0
        labels.append(label)
        if label == 0:
            features = [
                random.gauss(1.0, 0.1),
                random.gauss(0.5, 0.1),
                random.gauss(0.2, 0.1)
            ]
        else:
            features = [
                random.gauss(2.5, 0.2),
                random.gauss(1.8, 0.2),
                random.gauss(1.2, 0.2)
            ]
        data.append(features)

    return data, labels

class FNCLEMLClassifier:
    """A 2-Layer Neural Classifier implementing Frozen Feature Extractor and Sparse Delta Updates in Pure Python."""
    def __init__(self, input_dim=3, hidden_dim=8, output_dim=1):
        random.seed(42)
        # Base weights represent static factory parameters (frozen)
        # Dimensions: input_dim x hidden_dim
        self.w_base_1 = [[random.gauss(0.0, 0.1) for _ in range(hidden_dim)] for _ in range(input_dim)]
        # Dimensions: hidden_dim x output_dim
        self.w_base_2 = [[random.gauss(0.0, 0.1)] for _ in range(hidden_dim)]

        # Trainable deltas (only hidden-to-output layer is trained/personalizable)
        self.w_delta_2 = [[0.0] for _ in range(hidden_dim)]
        self.hidden_dim = hidden_dim
        self.input_dim = input_dim

    def forward(self, x):
        # Layer 1: Frozen base features
        # z1 = x * w_base_1
        self.z1 = [0.0] * self.hidden_dim
        for j in range(self.hidden_dim):
            self.z1[j] = sum(x[i] * self.w_base_1[i][j] for i in range(self.input_dim))

        # ReLU activation
        self.a1 = [max(0.0, val) for val in self.z1]

        # Layer 2: Resolved Virtual Weights (Base + Delta)
        # z2 = a1 * (w_base_2 + w_delta_2)
        self.resolved_w2 = [self.w_base_2[j][0] + self.w_delta_2[j][0] for j in range(self.hidden_dim)]
        self.z2 = sum(self.a1[j] * self.resolved_w2[j] for j in range(self.hidden_dim))

        # Sigmoid activation
        # To avoid overflow, cap self.z2
        z = max(-100.0, min(100.0, self.z2))
        self.a2 = 1.0 / (1.0 + math.exp(-z))
        return self.a2

    def train_step(self, x, y, lr=0.01):
        # 1. Forward pass
        pred = self.forward(x)

        # 2. Backward pass (Gradient calculation for trainable Layer 2 weights)
        # Loss: binary cross entropy
        loss = - (y * math.log(pred + 1e-15) + (1 - y) * math.log(1 - pred + 1e-15))

        # Gradient w.r.t z2
        dz2 = pred - y

        # Gradient w.r.t w_delta_2 and update sequentially
        for j in range(self.hidden_dim):
            dw_delta_2_j = self.a1[j] * dz2
            self.w_delta_2[j][0] -= lr * dw_delta_2_j

        return loss

def run_experiment():
    print("--- Running Pure Python FN-CLE Machine Learning Validation ---")
    data, labels = generate_sensor_anomaly_data()

    # Split into train/test
    split = 800
    train_x, train_y = data[:split], labels[:split]
    test_x, test_y = data[split:], labels[split:]

    model = FNCLEMLClassifier()

    # 1. Test baseline accuracy (untrained/factory settings)
    correct_before = 0
    for x, y in zip(test_x, test_y):
        pred = model.forward(x)
        pred_label = 1 if pred >= 0.5 else 0
        if pred_label == y:
            correct_before += 1
    acc_before = correct_before / len(test_y)
    print(f"Accuracy before personalization: {acc_before * 100:.2f}%")

    # 2. Train/Personalize model locally on training stream
    epochs = 15
    losses = []
    for epoch in range(epochs):
        epoch_losses = []
        for x, y in zip(train_x, train_y):
            loss = model.train_step(x, y, lr=0.05)
            epoch_losses.append(loss)
        losses.append(sum(epoch_losses) / len(epoch_losses))

    # 3. Test final accuracy (after training hidden-to-output layer weights)
    correct_after = 0
    for x, y in zip(test_x, test_y):
        pred = model.forward(x)
        pred_label = 1 if pred >= 0.5 else 0
        if pred_label == y:
            correct_after += 1
    acc_after = correct_after / len(test_y)
    print(f"Accuracy after personalization: {acc_after * 100:.2f}%")

    # Analyze weight footprint
    total_weights = (model.input_dim * model.hidden_dim) + model.hidden_dim
    trainable_weights = model.hidden_dim
    sparse_fraction = trainable_weights / total_weights

    results = {
        "Accuracy_Before": round(acc_before, 4),
        "Accuracy_After": round(acc_after, 4),
        "Improvement": round(acc_after - acc_before, 4),
        "Total_Parameters": total_weights,
        "Trainable_Parameters": trainable_weights,
        "Sparsity_Ratio": round(sparse_fraction, 4),
        "Final_Loss": round(losses[-1], 6)
    }

    print(json.dumps(results, indent=4))

    with open("ml_results.json", "w") as mf:
        json.dump(results, mf, indent=4)

    print("Machine learning validation complete. Results saved.")

if __name__ == "__main__":
    run_experiment()
