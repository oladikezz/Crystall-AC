package org.crystall.analytics;

import java.io.Serializable;

/**
 * First-order Exponentially Weighted Moving Average (EWMA) digital smoothing filter.
 *
 * <p>Mathematical Formulation:
 * <pre>
 *   S_0 = Y_0
 *   S_t = \alpha Y_t + (1 - \alpha) S_{t-1}, \quad t \ge 1
 * </pre>
 * where \alpha \in (0, 1] is the constant smoothing weight parameter.
 *
 * <p>Equivalence to continuous-time exponential decay:
 * The discrete weight \alpha corresponds to sampling period \Delta t and time constant \tau via:
 * <pre>
 *   \alpha = 1 - e^{-\Delta t / \tau}
 * </pre>
 *
 * <p>Key Theoretical Properties:
 * <ul>
 *   <li>Effective Window Size (equivalent simple moving average window): N_{eff} = (2 - \alpha) / \alpha</li>
 *   <li>Half-Life (number of observations for weight to decrease by 50%): t_{1/2} = -\ln(2) / \ln(1 - \alpha)</li>
 *   <li>Computational Complexity: Strict O(1) time and O(1) space per telemetry update</li>
 * </ul>
 */
public final class EWMASmoother implements Serializable {

    private static final long serialVersionUID = 1L;

    private final double alpha;
    private double value;
    private boolean initialized;
    private long observationCount;

    /**
     * Initializes the EWMA filter with the specified smoothing factor \alpha.
     *
     * @param alpha Smoothing factor \alpha in (0.0, 1.0]. Smaller \alpha gives more smoothing (longer memory).
     * @throws IllegalArgumentException If \alpha <= 0.0 or \alpha > 1.0
     */
    public EWMASmoother(double alpha) {
        if (Double.isNaN(alpha) || alpha <= 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("Smoothing factor alpha must satisfy 0 < alpha <= 1. Received: " + alpha);
        }
        this.alpha = alpha;
        reset();
    }

    /**
     * Initializes with specified alpha and default initial prior value.
     *
     * @param alpha Smoothing factor in (0.0, 1.0]
     * @param initialValue Prior baseline value
     */
    public EWMASmoother(double alpha, double initialValue) {
        this(alpha);
        this.value = initialValue;
        this.initialized = true;
    }

    /**
     * Ingests a new continuous measurement Y_t and updates the smoothed estimate in O(1) time.
     *
     * @param y Incoming continuous observation
     * @return Updated smoothed value S_t
     */
    public double update(double y) {
        observationCount++;
        if (!initialized) {
            value = y;
            initialized = true;
        } else {
            value = alpha * y + (1.0 - alpha) * value;
        }
        return value;
    }

    public double getValue() {
        return value;
    }

    public double getAlpha() {
        return alpha;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public long getObservationCount() {
        return observationCount;
    }

    /**
     * Returns the equivalent Simple Moving Average (SMA) window size: N_{eff} = (2 - \alpha) / \alpha.
     */
    public double getEffectiveWindowSize() {
        return (2.0 - alpha) / alpha;
    }

    /**
     * Returns the decay half-life in observation periods: t_{1/2} = -\ln(2) / \ln(1 - \alpha).
     */
    public double getHalfLife() {
        if (alpha >= 1.0) return 0.0;
        return -Math.log(2.0) / Math.log(1.0 - alpha);
    }

    public void reset() {
        this.value = 0.0;
        this.initialized = false;
        this.observationCount = 0L;
    }

    @Override
    public String toString() {
        return String.format("EWMASmoother[alpha=%.4f, value=%.6f, n=%d, N_eff=%.1f, t_half=%.1f]",
                alpha, value, observationCount, getEffectiveWindowSize(), getHalfLife());
    }
}
