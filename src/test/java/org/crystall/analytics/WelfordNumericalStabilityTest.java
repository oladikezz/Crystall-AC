package org.crystall.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rigorous numerical stability benchmark and verification suite for {@link WelfordAccumulator}.
 * Demonstrates catastrophic floating-point cancellation in naive textbook formulas and
 * validates Chan-Golub-LeVeque parallel reduction.
 */
public class WelfordNumericalStabilityTest {

    @Test
    @DisplayName("Catastrophic Cancellation: Welford vs Naive One-Pass on High-Offset Data")
    public void testCatastrophicCancellation() {
        // Construct a dataset with known variance, shifted by a massive offset
        // X_i = K + r_i, where K = 10^9 and r_i in [0, 1]
        // Theoretical Var(X + K) == Var(r)
        final double offset = 1e9;
        final int n = 100_000;
        final double[] data = new double[n];
        final Random rng = new Random(42L);

        for (int i = 0; i < n; i++) {
            data[i] = offset + rng.nextDouble();
        }

        // 1. Two-Pass Algorithm (Gold Standard for in-memory)
        double sum = 0.0;
        for (double v : data) sum += v;
        double trueMean = sum / n;

        double sumSqDiff = 0.0;
        for (double v : data) {
            double diff = v - trueMean;
            sumSqDiff += diff * diff;
        }
        double twoPassVariance = sumSqDiff / (n - 1);

        // 2. Naive One-Pass Formula: \sum x^2 - (\sum x)^2 / n
        double naiveSum = 0.0;
        double naiveSumSq = 0.0;
        for (double v : data) {
            naiveSum += v;
            naiveSumSq += v * v;
        }
        double naiveVariance = (naiveSumSq - (naiveSum * naiveSum) / n) / (n - 1);

        // 3. Welford's Single-Pass Algorithm
        WelfordAccumulator welford = new WelfordAccumulator();
        for (double v : data) {
            welford.update(v);
        }
        double welfordVariance = welford.getSampleVariance();

        // Print numerical findings
        System.out.println("================================================================================");
        System.out.println("NUMERICAL STABILITY BENCHMARK (Offset K = 10^9, N = " + n + "):");
        System.out.println(String.format("Two-Pass (True) Variance : %.12f", twoPassVariance));
        System.out.println(String.format("Welford Variance         : %.12f", welfordVariance));
        System.out.println(String.format("Naive One-Pass Variance  : %.12f", naiveVariance));

        double welfordRelativeError = Math.abs(welfordVariance - twoPassVariance) / twoPassVariance;
        double naiveRelativeError = Math.abs(naiveVariance - twoPassVariance) / twoPassVariance;

        System.out.println(String.format("Welford Relative Error   : %.4e", welfordRelativeError));
        System.out.println(String.format("Naive Relative Error     : %.4e", naiveRelativeError));
        System.out.println("================================================================================");

        // At K = 10^9, machine epsilon is 1e9 * 2^-52 ~= 2.22e-7.
        // Welford achieves near-optimal relative precision (~8e-8), whereas naive completely degenerates (-8220).
        assertTrue(welfordRelativeError < 1e-6, "Welford relative error exceeded IEEE-754 precision tolerance: " + welfordRelativeError);

        // Naive one-pass will experience massive loss of precision (relative error > 10% or negative variance)
        assertTrue(naiveRelativeError > 0.05 || Double.isNaN(naiveVariance) || naiveVariance < 0,
                "Naive formula unexpectedly succeeded on ill-conditioned data");
    }

    @Test
    @DisplayName("Chan-Golub-LeVeque Parallel Accumulator Reduction Verification")
    public void testParallelAccumulatorReduction() {
        final int n = 50_000;
        final Random rng = new Random(1337L);
        final double[] data = new double[n];
        for (int i = 0; i < n; i++) data[i] = rng.nextGaussian() * 15.0 + 100.0;

        // Partition data into two partitions A and B
        int mid = n / 2;
        WelfordAccumulator accA = new WelfordAccumulator();
        WelfordAccumulator accB = new WelfordAccumulator();
        WelfordAccumulator sequential = new WelfordAccumulator();

        for (int i = 0; i < mid; i++) {
            accA.update(data[i]);
            sequential.update(data[i]);
        }
        for (int i = mid; i < n; i++) {
            accB.update(data[i]);
            sequential.update(data[i]);
        }

        // Merge partition B into A
        accA.combine(accB);

        assertEquals(sequential.getCount(), accA.getCount());
        assertEquals(sequential.getMean(), accA.getMean(), 1e-10);
        assertEquals(sequential.getSampleVariance(), accA.getSampleVariance(), 1e-10);
    }

    @Test
    @DisplayName("Single-Pass Execution Latency Benchmark (1,000,000 updates)")
    public void testExecutionLatency() {
        final int iterations = 1_000_000;
        WelfordAccumulator accumulator = new WelfordAccumulator();

        // Warmup JIT
        for (int i = 0; i < 50_000; i++) {
            accumulator.update(i * 0.1);
        }
        accumulator.reset();

        // Timed Execution
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            accumulator.update(i * 0.005);
        }
        long durationNs = System.nanoTime() - startTime;

        double nsPerOp = (double) durationNs / iterations;
        double opsPerSec = (1_000_000_000.0 / durationNs) * iterations;

        System.out.println("================================================================================");
        System.out.println("LATENCY BENCHMARK (1,000,000 streaming updates):");
        System.out.println(String.format("Mean Latency per Update : %.2f ns/op", nsPerOp));
        System.out.println(String.format("Throughput              : %.2f million ops/sec", opsPerSec / 1e6));
        System.out.println("================================================================================");

        assertTrue(nsPerOp < 50.0, "Latency per update exceeds high-frequency telemetry threshold (50ns)");
    }
}
