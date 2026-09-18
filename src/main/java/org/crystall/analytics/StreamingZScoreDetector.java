package org.crystall.analytics;

import java.io.Serializable;

/**
 * Standardized streaming anomaly detector utilizing running studentized residuals (Z-score).
 *
 * <p>Mathematical Formulation:
 * <pre>
 *   Z = \frac{x - \mu}{\sigma + \epsilon}
 * </pre>
 * where \epsilon > 0 prevents numerical instability in near-zero variance regimes.
 *
 * <p>Theoretical Tail Bounds:
 * <ul>
 *   <li><b>Normal Distribution:</b> P(|Z| \ge k) = 2(1 - \Phi(k)). For k = 3.0, P \approx 0.0027 (0.27%).</li>
 *   <li><b>Chebyshev's Inequality (Arbitrary Distribution):</b> For any distribution with finite variance,
 *       P(|X - \mu| \ge k\sigma) \le \frac{1}{k^2}. For k = 3.0, P \le \frac{1}{9} \approx 0.111 (11.1%).</li>
 *   <li><b>Vysochanskij-Petunin Inequality (Unimodal Distribution):</b> For any unimodal distribution,
 *       P(|X - \mu| \ge k\sigma) \le \frac{4}{9k^2}. For k = 3.0, P \le \frac{4}{81} \approx 0.0494 (4.94%).</li>
 * </ul>
 */
public final class StreamingZScoreDetector implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum TailType {
        TWO_TAILED,
        UPPER_ONLY,
        LOWER_ONLY
    }

    private final double threshold;
    private final double epsilon;
    private final TailType tailType;

    public StreamingZScoreDetector(double threshold) {
        this(threshold, TailType.TWO_TAILED, 1e-6);
    }

    public StreamingZScoreDetector(double threshold, TailType tailType, double epsilon) {
        if (threshold <= 0.0) {
            throw new IllegalArgumentException("Anomaly threshold must be strictly positive. Received: " + threshold);
        }
        if (epsilon <= 0.0) {
            throw new IllegalArgumentException("Epsilon regularizer must be positive. Received: " + epsilon);
        }
        this.threshold = threshold;
        this.tailType = tailType;
        this.epsilon = epsilon;
    }

    /**
     * Evaluates a single observation against running parametric parameters.
     *
     * @param value Observed metric
     * @param mean Baseline mean \mu
     * @param stdDev Baseline standard deviation \sigma
     * @return Evaluation score record
     */
    public AnomalyScore evaluate(double value, double mean, double stdDev) {
        double effectiveStdDev = Math.max(stdDev, epsilon);
        double zScore = (value - mean) / effectiveStdDev;

        boolean anomalous;
        switch (tailType) {
            case UPPER_ONLY -> anomalous = zScore >= threshold;
            case LOWER_ONLY -> anomalous = zScore <= -threshold;
            case TWO_TAILED -> anomalous = Math.abs(zScore) >= threshold;
            default -> anomalous = false;
        }

        // Conservative distribution-free Chebyshev upper bound on probability of exceedance
        double chebyshevBound = (Math.abs(zScore) <= 1.0) ? 1.0 : Math.min(1.0, 1.0 / (zScore * zScore));

        return new AnomalyScore(zScore, threshold, anomalous, chebyshevBound);
    }

    public double getThreshold() {
        return threshold;
    }

    public TailType getTailType() {
        return tailType;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public record AnomalyScore(
            double zScore,
            double threshold,
            boolean isAnomaly,
            double chebyshevUpperBound
    ) implements Serializable {
        @Override
        public String toString() {
            return String.format("AnomalyScore[Z=%.4f, threshold=%.2f, anomaly=%b, P_chebyshev<=%.4f]",
                    zScore, threshold, isAnomaly, chebyshevUpperBound);
        }
    }
}
