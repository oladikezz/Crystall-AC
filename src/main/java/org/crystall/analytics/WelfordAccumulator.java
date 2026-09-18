package org.crystall.analytics;

import java.io.Serializable;

/**
 * Numerically stable single-pass running mean and variance accumulator
 * implementing B.P. Welford's recurrence relation (1962), generalized by
 * Chan, Golub, and LeVeque (1979) for parallel reduction.
 *
 * <p>Computational Complexity:
 * <ul>
 *   <li>Time Complexity: Strict O(1) per update</li>
 *   <li>Space Complexity: Strict O(1) auxiliary memory</li>
 * </ul>
 *
 * <p>Recurrence Formulations:
 * <pre>
 *   \mu_n = \mu_{n-1} + \frac{x_n - \mu_{n-1}}{n}
 *   M_{2, n} = M_{2, n-1} + (x_n - \mu_{n-1})(x_n - \mu_n)
 *   s_n^2 = \frac{M_{2, n}}{n - 1} \quad (n \ge 2)
 * </pre>
 *
 * This eliminates the catastrophic numerical cancellation failure mode of
 * the naive textbook one-pass formula \sum x^2 - (\sum x)^2 / n in IEEE 754
 * double-precision arithmetic.
 */
public final class WelfordAccumulator implements Serializable {

    private static final long serialVersionUID = 1L;

    private long count;
    private double mean;
    private double m2; // Sum of squared differences from the mean: \sum (x_i - \bar{x})^2

    public WelfordAccumulator() {
        reset();
    }

    public WelfordAccumulator(WelfordAccumulator other) {
        this.count = other.count;
        this.mean = other.mean;
        this.m2 = other.m2;
    }

    /**
     * Ingests a new continuous telemetry observation in O(1) time.
     *
     * @param x Incoming scalar observation
     */
    public void update(double x) {
        count++;
        double delta = x - mean;
        mean += delta / count;
        double delta2 = x - mean;
        m2 += delta * delta2;
    }

    /**
     * Merges another WelfordAccumulator into this instance in O(1) time
     * using the parallel algorithm derived by Chan, Golub, and LeVeque (1979).
     *
     * @param other The accumulator to merge
     */
    public void combine(WelfordAccumulator other) {
        if (other == null || other.count == 0) {
            return;
        }
        if (this.count == 0) {
            this.count = other.count;
            this.mean = other.mean;
            this.m2 = other.m2;
            return;
        }

        long combinedCount = this.count + other.count;
        double delta = other.mean - this.mean;

        // Pooled mean
        this.mean += delta * ((double) other.count / combinedCount);

        // Pooled M2
        this.m2 += other.m2 + delta * delta * ((double) this.count * other.count / combinedCount);
        this.count = combinedCount;
    }

    public void reset() {
        this.count = 0L;
        this.mean = 0.0;
        this.m2 = 0.0;
    }

    public long getCount() {
        return count;
    }

    public double getMean() {
        return mean;
    }

    /**
     * Returns the unbiased sample variance s^2 with Bessel's correction (n - 1 denominator).
     *
     * @return Sample variance s^2, or 0.0 if count < 2
     */
    public double getSampleVariance() {
        return (count < 2L) ? 0.0 : m2 / (count - 1L);
    }

    /**
     * Returns the population variance \sigma^2 (n denominator).
     *
     * @return Population variance \sigma^2, or 0.0 if count == 0
     */
    public double getPopulationVariance() {
        return (count == 0L) ? 0.0 : m2 / count;
    }

    /**
     * Returns the sample standard deviation s = \sqrt{s^2}.
     *
     * @return Sample standard deviation
     */
    public double getStandardDeviation() {
        return Math.sqrt(getSampleVariance());
    }

    public double getSumOfSquares() {
        return m2;
    }

    @Override
    public String toString() {
        return String.format("WelfordAccumulator[n=%d, mean=%.6f, stdDev=%.6f, M2=%.6f]",
                count, mean, getStandardDeviation(), m2);
    }
}
