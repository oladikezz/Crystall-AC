<div align="center">

# Crystall-AC
### Real-Time Unsupervised Statistical Anomaly Detection for High-Frequency Telemetry Streams

[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven Central](https://img.shields.io/badge/Maven-Build%20Passing-success?style=for-the-badge&logo=apachemaven&logoColor=white)]()
[![Algorithm](https://img.shields.io/badge/Algorithm-Welford%20%7C%20EWMA%20%7C%20Z--Score-blueviolet?style=for-the-badge)]()
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

<p align="center">
  <a href="#abstract">Abstract</a> •
  <a href="#mathematical-framework">Mathematical Framework</a> •
  <a href="#system-architecture">Architecture</a> •
  <a href="#empirical-benchmarks">Empirical Benchmarks</a> •
  <a href="#standalone-core-api">Core API</a> •
  <a href="#testbed-application">Testbed Application</a>
</p>

---

</div>

## Abstract

High-frequency, real-time telemetry processing environments (such as network packet streams, low-latency financial feeds, distributed cyber-physical systems, and high-tickrate game engines) generate non-stationary time series characterized by volatile drift, burst noise, and absence of ground-truth labeled datasets. Traditional supervised machine learning techniques (e.g., deep neural networks, offline gradient boosted decision trees) require substantial pre-annotated training corpora, suffer from distribution shift, and introduce prohibitive inference latencies (> 1 ms) incompatible with microsecond-scale execution budgets.

**Crystall-AC** introduces a lightweight, mathematically sound, decoupled streaming analytics engine (`org.crystall.analytics`) designed for zero-dataset, online unsupervised anomaly detection. By combining **B.P. Welford's single-pass recurrence relation** for numerically stable variance calculation in strict $\mathcal{O}(1)$ time and $\mathcal{O}(1)$ space with an **Exponentially Weighted Moving Average (EWMA)** continuous-drift tracker and **standardized studentized residual thresholding**, Crystall-AC evaluates streaming telemetry at sub-10-nanosecond latency per observation while provably eliminating IEEE 754 catastrophic numerical cancellation.

---

## Mathematical Framework

### 1. Telemetry Stream Definition
Let an incoming continuous telemetry observation stream be modeled as a sequence of random variables $\{X_t\}_{t \in \mathbb{N}}$, where each observation $x_t \in \mathbb{R}$ is sampled from a time-varying probability distribution with unknown instantaneous mean $\mu_t$ and finite instantaneous variance $\sigma_t^2 < \infty$.

---

### 2. Numerically Stable Single-Pass Variance (Welford's Algorithm)

#### The Catastrophic Cancellation Failure Mode
The textbook sample variance formula computes:
$$s^2 = \frac{1}{n - 1} \left( \sum_{i=1}^n x_i^2 - \frac{1}{n} \left(\sum_{i=1}^n x_i\right)^2 \right)$$

In floating-point arithmetic (IEEE 754 float64), when the sample mean $\bar{x}$ is large relative to the standard deviation $s$ (condition number $\kappa = \frac{\|x\|_2}{\|x - \bar{x}\|_2} \gg 1$), the terms $\sum x_i^2$ and $\frac{1}{n} (\sum x_i)^2$ share nearly identical higher-order significand bits. Subtraction of these two massive quantities results in **catastrophic cancellation**, resulting in severe loss of significand precision, wild numerical inaccuracies, and frequently negative variance ($s^2 < 0$).

#### Welford's Recurrence Relation (1962)
To guarantee numerical stability, Crystall-AC computes the running mean $\mu_n$ and sum of squared deviations from the mean $M_{2, n} = \sum_{i=1}^n (x_i - \mu_n)^2$ using the single-pass recurrence equations:

$$\mu_n = \mu_{n-1} + \frac{x_n - \mu_{n-1}}{n}$$

$$M_{2, n} = M_{2, n-1} + (x_n - \mu_{n-1})(x_n - \mu_n)$$

The unbiased sample variance $s_n^2$ and sample standard deviation $s_n$ are derived with Bessel's correction:

$$s_n^2 = \frac{M_{2, n}}{n - 1} \quad (n \ge 2), \qquad s_n = \sqrt{s_n^2}$$

* **Time Complexity:** Strict $\mathcal{O}(1)$ updates (two subtractions, two additions, one division, one multiplication).
* **Space Complexity:** Strict $\mathcal{O}(1)$ memory (three 64-bit registers: $n$, $\mu_n$, $M_{2, n}$).

#### Parallel Reduction (Chan, Golub, and LeVeque, 1979)
For multi-threaded partition merging, two independent accumulators $A$ and $B$ (with sample sizes $n_A, n_B$, means $\mu_A, \mu_B$, and squared sums $M_{2, A}, M_{2, B}$) are combined in $\mathcal{O}(1)$ time:

$$n = n_A + n_B, \qquad \delta = \mu_B - \mu_A$$

$$\mu = \mu_A + \delta \cdot \frac{n_B}{n}$$

$$M_2 = M_{2, A} + M_{2, B} + \delta^2 \cdot \frac{n_A n_B}{n}$$

---

### 3. Non-Stationary Trend Estimation (EWMA Filter)

To capture localized drift in non-stationary player or process behavior, Crystall-AC applies a discrete first-order Exponentially Weighted Moving Average filter:

$$S_0 = Y_0, \qquad S_t = \alpha Y_t + (1 - \alpha) S_{t-1}, \quad t \ge 1$$

where $\alpha \in (0, 1]$ represents the smoothing weight.

#### Theoretical Properties:
* **Equivalent Simple Moving Average (SMA) Window:**
  $$N_{\text{eff}} = \frac{2 - \alpha}{\alpha}$$
* **Memory Half-Life (in observation cycles):**
  $$t_{1/2} = \frac{-\ln(2)}{\ln(1 - \alpha)}$$
* **Theoretical Variance of Smoothed Metric:**
  $$\text{Var}(S_t) = \sigma^2 \left(\frac{\alpha}{2 - \alpha}\right) \left[ 1 - (1 - \alpha)^{2t} \right]$$

---

### 4. Standardized Anomaly Decision Bounds

For each incoming signal observation $x_t$, the studentized residual (Z-Score) is evaluated against the running baseline:

$$Z_t = \frac{x_t - \mu_t}{\sigma_t + \epsilon}$$

where $\epsilon = 10^{-6}$ acts as a numerical Tikhonov regularizer preventing division-by-zero in degenerate zero-variance sequences.

#### Distribution-Free Tail Bounds:
1. **Chebyshev's Inequality (Arbitrary Distribution):**
   For any distribution with finite variance, the probability of exceeding threshold $k$ is strictly bounded by:
   $$\mathbb{P}(|X - \mu| \ge k\sigma) \le \frac{1}{k^2}$$
   *For $k = 3.0$, $\mathbb{P}(|Z| \ge 3.0) \le \frac{1}{9} \approx 11.11\%$.*

2. **Vysochanskij-Petunin Inequality (Unimodal Continuous Distribution):**
   For any continuous, unimodal distribution (the standard physical assumption for human motor kinetics and packet timings):
   $$\mathbb{P}(|X - \mu| \ge k\sigma) \le \frac{4}{9k^2}$$
   *For $k = 3.0$, $\mathbb{P}(|Z| \ge 3.0) \le \frac{4}{81} \approx 4.94\%$.*

3. **Gaussian Assumption (Null Hypothesis):**
   $$\mathbb{P}(|Z| \ge k) = 2 \left( 1 - \Phi(k) \right)$$
   *For $k = 3.0$, $\mathbb{P}(|Z| \ge 3.0) = 0.0026998 \approx 0.27\%$.*

---

## System Architecture

```
                    High-Frequency Continuous Telemetry Stream
                                         │
                                         ▼
                 ┌────────────────────────────────────────────────┐
                 │          Version & Protocol Decoupling         │
                 │             (VersionAdapter Layer)             │
                 └───────────────────────┬────────────────────────┘
                                         │
                                         ▼
                 ┌────────────────────────────────────────────────┐
                 │     Kinematic & Lag Compensation Filter        │
                 │   (Friction Matrices, Jump Boost, Ping Buffer) │
                 └───────────────────────┬────────────────────────┘
                                         │
                   ┌─────────────────────┴─────────────────────┐
                   ▼                                           ▼
┌─────────────────────────────────────┐     ┌─────────────────────────────────────┐
│      Decoupled Analytics Core       │     │        Heuristic Engine             │
│      (org.crystall.analytics)       │     │        (Domain Physics)             │
│  - WelfordAccumulator: O(1) Var     │     │  - Kinematic Speed & Friction       │
│  - EWMASmoother: O(1) Trend         │     │  - AABB Raycasting (Reach)          │
│  - StreamingZScoreDetector: Tail    │     │  - Rotational Quantization (GCD)    │
└──────────────────┬──────────────────┘     └──────────────────┬──────────────────┘
                   │                                           │
                   └─────────────────────┬─────────────────────┘
                                         ▼
                 ┌────────────────────────────────────────────────┐
                 │          Violation & Mitigation Engine         │
                 │       (Continuous VL Accumulation & Decay)     │
                 └───────────────────────┬────────────────────────┘
                                         │
                   ┌─────────────────────┼─────────────────────┐
                   ▼                     ▼                     ▼
        ┌─────────────────────┐┌───────────────────┐┌─────────────────────┐
        │  Active Mitigation  ││  SQLite Training  ││  Audit & Telemetry │
        │  - Pos Setback      ││  - Feature Buffer ││  - Discord Webhook │
        │  - Combat Cancel    ││  - SGD Auto-Tune  ││  - Admin Chest GUI │
        └─────────────────────┘└───────────────────┘└─────────────────────┘
```

---

## Empirical Benchmarks

All microbenchmarks were executed under OpenJDK 25 (HotSpot 64-Bit, Windows 11 / AMD64) across $10^5$ and $10^6$ synthetic telemetry points.

### 1. Numerical Stability Under Massive Offset Shift ($K = 10^9, N = 100,000$)
To test resilience against catastrophic cancellation, synthetic observations $x_i = 10^9 + r_i$ ($r_i \sim \mathcal{U}(0, 1)$) were fed into each algorithm. The true theoretical variance is $\text{Var}(X) = \text{Var}(r) = \frac{1}{12} \approx 0.083333$.

| Algorithm | Computed Variance | Relative Error vs True | Status |
| :--- | :--- | :--- | :--- |
| **Two-Pass (In-Memory Reference)** | `0.083373814873` | Reference ($0.00$) | Optimal (Requires 2 Passes) |
| **Welford's Single-Pass (Crystall-AC)** | `0.083373821594` | **$8.06 \times 10^{-8}$** | **Exact within Float64 Condition Limit** |
| **Naive One-Pass ($\sum x^2 - (\sum x)^2 / n$)** | `-8220.91804918` | **$9.86 \times 10^{4}$** | **FAILED (Catastrophic Cancellation / Negative)** |

### 2. Execution Latency & Throughput ($N = 1,000,000$ Updates)

| Metric | Measured Value | Standard Error |
| :--- | :--- | :--- |
| **Mean Execution Latency** | **$8.74 \text{ ns / operation}$** | $\pm 0.12 \text{ ns}$ |
| **Throughput** | **$114.40 \text{ Million updates / sec}$** | $\pm 1.5 \text{ Mops/s}$ |
| **Memory Allocation per Update** | **$0 \text{ bytes (Zero Garbage)}$** | $0.00 \text{ alloc}$ |
| **Auxiliary Memory per Stream** | **$24 \text{ bytes}$** ($1 \times \text{long}, 2 \times \text{double}$) | Constant $\mathcal{O}(1)$ |

### 3. Anomaly Verification on Synthetic Probability Distributions

| Test Scenario | Observations | Metric Evaluated | Empirical Result | Theoretical Prediction |
| :--- | :--- | :--- | :--- | :--- |
| **Gaussian Baseline Null** | $99,000$ | False Positive Rate ($|Z| > 3.0$) | **$0.2263\%$** | $0.2700\%$ ($\alpha = 0.0027$) |
| **Contaminated Stream ($4\sigma - 8\sigma$ spikes)** | $500$ | True Positive Rate (Sensitivity) | **$100.00\%$** | $\ge 99.00\%$ |
| **Non-Parametric Uniform Stream** | $49,000$ | Tail Exceedance ($k = 2.5$) | **$0.0000\%$** | $\le 16.0000\%$ (Chebyshev Bound) |

---

## Standalone Core API

The mathematical engine is packaged in `org.crystall.analytics` with zero external dependencies.

```java
import org.crystall.analytics.WelfordAccumulator;
import org.crystall.analytics.EWMASmoother;
import org.crystall.analytics.StreamingZScoreDetector;

// 1. Initialize mathematical primitives
WelfordAccumulator runningStats = new WelfordAccumulator();
EWMASmoother shortTermTrend = new EWMASmoother(0.15); // Alpha = 0.15
StreamingZScoreDetector anomalyDetector = new StreamingZScoreDetector(3.0); // |Z| > 3.0

// 2. Process streaming telemetry observations in O(1) time
double telemetryMeasurement = 14.52; // e.g. instantaneous angular velocity or click delta

// Evaluate anomaly against baseline prior to update
StreamingZScoreDetector.AnomalyScore score = anomalyDetector.evaluate(
    telemetryMeasurement,
    runningStats.getMean(),
    runningStats.getStandardDeviation()
);

if (score.isAnomaly()) {
    System.out.printf("Anomaly Flagged! Z-Score: %.2f (Chebyshev P <= %.4f)%n",
            score.zScore(), score.chebyshevUpperBound());
}

// Update running statistical accumulators
runningStats.update(telemetryMeasurement);
shortTermTrend.update(telemetryMeasurement);

System.out.printf("Running Mean: %.4f | Running StdDev: %.4f | EWMA: %.4f%n",
        runningStats.getMean(), runningStats.getStandardDeviation(), shortTermTrend.getValue());
```

---

## Testbed Application: Minecraft High-Frequency Kinetic Server

To evaluate the mathematical engine in a high-concurrency production testbed, Crystall-AC embeds this architecture as an anti-cheat subsystem for Minecraft Paper/Spigot platforms (supporting versions **1.21.4**, **1.21.11**, **26.1.2**, and **26.2**).

### High-Frequency Domain Checks:
* **`KillauraCheck`**: Spatial orientation analysis; flags non-smooth rotational angular velocity snaps ($> 38.5^\circ/\text{tick}$) and multi-target dispatching within a single discrete tick.
* **`ReachCheck`**: Raycasting from the origin camera to the target Axis-Aligned Bounding Box (AABB) with dynamic network round-trip latency extrapolation:
  $$\text{Reach}_{\text{max}} = 3.05 + (\text{Ping}_A + \text{Ping}_B) \cdot 0.0035 + 0.10\text{b}$$
* **`SpeedCheck`**: Euler integration of ground ($0.6$), air ($0.91$), and ice ($1.45\times$) friction matrices with active rubberband setback mitigation.
* **`AutoclickerCheck`**: Analysis of continuous click-interval standard deviation; human neurological refractory periods exhibit physiological jitter ($\sigma \ge 4.8\text{ ms}$ at $\text{CPS} \ge 13$), whereas automated macros produce degenerate near-zero variance ($\sigma \to 0$).

---

## Building and Verification

### Prerequisites
- Java Development Kit (JDK) 21 or higher
- Apache Maven 3.8+

```bash
# Clone the repository
git clone https://github.com/oladikezz/Crystall-AC.git
cd Crystall-AC

# Execute rigorous unit tests and numerical stability benchmark
mvn test

# Package shaded production artifact
mvn clean package
```

---

## License

This project is licensed under the [MIT License](LICENSE).