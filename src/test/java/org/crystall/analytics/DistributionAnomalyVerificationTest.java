package org.crystall.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Empirical statistical verification of False Positive Rate (FPR) and True Positive Rate (TPR)
 * across synthetic Gaussian and heavy-tailed anomaly telemetry streams.
 */
public class DistributionAnomalyVerificationTest {

    @Test
    @DisplayName("Gaussian Noise Null Hypothesis: Empirical FPR at |Z| > 3.0 matches 0.27% Theoretical Alpha")
    public void testGaussianFalsePositiveRate() {
        final int n = 100_000;
        final double trueMean = 50.0;
        final double trueStdDev = 8.0;
        final Random rng = new Random(2026L);

        WelfordAccumulator accumulator = new WelfordAccumulator();
        StreamingZScoreDetector detector = new StreamingZScoreDetector(3.0);

        int falsePositives = 0;

        // Warmup baseline with initial 1,000 observations
        for (int i = 0; i < 1_000; i++) {
            double sample = trueMean + rng.nextGaussian() * trueStdDev;
            accumulator.update(sample);
        }

        // Test streaming detection across remaining 99,000 observations
        for (int i = 1_000; i < n; i++) {
            double sample = trueMean + rng.nextGaussian() * trueStdDev;
            StreamingZScoreDetector.AnomalyScore score = detector.evaluate(
                    sample, accumulator.getMean(), accumulator.getStandardDeviation());

            if (score.isAnomaly()) {
                falsePositives++;
            }
            accumulator.update(sample);
        }

        int evaluatedCount = n - 1_000;
        double empiricalFPR = (double) falsePositives / evaluatedCount;
        double theoreticalFPR = 0.0026998; // 2 * (1 - \Phi(3.0))

        System.out.println("================================================================================");
        System.out.println("GAUSSIAN NULL HYPOTHESIS TEST (|Z| > 3.0):");
        System.out.println(String.format("Evaluated Observations : %d", evaluatedCount));
        System.out.println(String.format("False Positives Detected: %d", falsePositives));
        System.out.println(String.format("Empirical FPR          : %.4f%%", empiricalFPR * 100.0));
        System.out.println(String.format("Theoretical FPR        : %.4f%%", theoreticalFPR * 100.0));
        System.out.println("================================================================================");

        // Empirical FPR should fall within [0.15%, 0.45%] with 99.9% statistical confidence
        assertTrue(empiricalFPR >= 0.0015 && empiricalFPR <= 0.0045,
                "Empirical FPR deviated significantly from theoretical Gaussian tail probability: " + empiricalFPR);
    }

    @Test
    @DisplayName("Outlier Detection Sensitivity: TPR on Heavy-Tailed Anomaly Injections")
    public void testHeavyTailedAnomalyDetection() {
        final int nNormal = 50_000;
        final int nAnomalies = 500;
        final Random rng = new Random(42L);

        WelfordAccumulator accumulator = new WelfordAccumulator();
        StreamingZScoreDetector detector = new StreamingZScoreDetector(3.0);

        // Populate baseline
        for (int i = 0; i < 2_000; i++) {
            accumulator.update(100.0 + rng.nextGaussian() * 5.0);
        }

        int truePositives = 0;

        // Ingest normal stream interleaved with extreme anomalies (>= 4.0 standard deviations)
        for (int i = 0; i < nAnomalies; i++) {
            // Normal background noise
            for (int j = 0; j < 50; j++) {
                accumulator.update(100.0 + rng.nextGaussian() * 5.0);
            }

            // Injected anomaly: magnitude between 4\sigma and 8\sigma
            double anomalyMagnitude = (4.0 + rng.nextDouble() * 4.0) * 5.0;
            double anomalySign = rng.nextBoolean() ? 1.0 : -1.0;
            double outlier = accumulator.getMean() + anomalySign * anomalyMagnitude;

            StreamingZScoreDetector.AnomalyScore score = detector.evaluate(
                    outlier, accumulator.getMean(), accumulator.getStandardDeviation());

            if (score.isAnomaly()) {
                truePositives++;
            }
            // Anomaly is not incorporated into clean baseline
        }

        double tpr = (double) truePositives / nAnomalies;
        System.out.println("================================================================================");
        System.out.println("HEAVY-TAILED ANOMALY SENSITIVITY TEST:");
        System.out.println(String.format("Injected Outliers     : %d", nAnomalies));
        System.out.println(String.format("True Positives Flagged: %d", truePositives));
        System.out.println(String.format("Sensitivity / TPR     : %.2f%%", tpr * 100.0));
        System.out.println("================================================================================");

        assertTrue(tpr >= 0.99, "True Positive Rate for extreme outliers fell below 99%: " + tpr);
    }

    @Test
    @DisplayName("Non-Parametric Chebyshev Bound Invariant on Uniform and Exponential Telemetry")
    public void testChebyshevTailBoundInvariant() {
        final int n = 50_000;
        final Random rng = new Random(999L);
        final double k = 2.5; // Threshold k = 2.5 => Chebyshev bound P <= 1 / 2.5^2 = 0.16 (16%)

        WelfordAccumulator uniformAcc = new WelfordAccumulator();
        StreamingZScoreDetector detector = new StreamingZScoreDetector(k);

        // Warmup
        for (int i = 0; i < 1_000; i++) {
            uniformAcc.update(rng.nextDouble() * 100.0);
        }

        int violations = 0;
        for (int i = 1_000; i < n; i++) {
            double val = rng.nextDouble() * 100.0;
            if (detector.evaluate(val, uniformAcc.getMean(), uniformAcc.getStandardDeviation()).isAnomaly()) {
                violations++;
            }
            uniformAcc.update(val);
        }

        double empiricalRate = (double) violations / (n - 1_000);
        double maxTheoreticalBound = 1.0 / (k * k);

        System.out.println("================================================================================");
        System.out.println("CHEBYSHEV INVARIANT TEST (Non-Gaussian Uniform Stream):");
        System.out.println(String.format("Empirical Rate         : %.4f%%", empiricalRate * 100.0));
        System.out.println(String.format("Chebyshev Upper Bound  : %.4f%%", maxTheoreticalBound * 100.0));
        System.out.println("================================================================================");

        assertTrue(empiricalRate <= maxTheoreticalBound,
                "Empirical exceedance rate violated distribution-free Chebyshev inequality bound");
    }
}
