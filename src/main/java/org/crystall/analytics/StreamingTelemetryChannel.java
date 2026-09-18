package org.crystall.analytics;

import java.io.Serializable;

/**
 * Composite streaming telemetry processor combining:
 * <ul>
 *   <li>Long-term parametric baseline via {@link WelfordAccumulator} (O(1) time & space)</li>
 *   <li>Short-term non-stationary trend estimation via {@link EWMASmoother} (O(1) time & space)</li>
 *   <li>Standardized residual anomaly evaluation via {@link StreamingZScoreDetector}</li>
 * </ul>
 */
public final class StreamingTelemetryChannel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String channelName;
    private final WelfordAccumulator longTermAccumulator;
    private final EWMASmoother shortTermSmoother;
    private final StreamingZScoreDetector detector;

    public StreamingTelemetryChannel(String channelName, double ewmaAlpha, double anomalyThreshold) {
        this.channelName = channelName;
        this.longTermAccumulator = new WelfordAccumulator();
        this.shortTermSmoother = new EWMASmoother(ewmaAlpha);
        this.detector = new StreamingZScoreDetector(anomalyThreshold);
    }

    /**
     * Ingests a new continuous measurement, updates dual baselines, and evaluates anomaly status in O(1) time.
     *
     * @param measurement Raw incoming telemetry signal
     * @return Evaluated anomaly score against running baseline
     */
    public synchronized StreamingZScoreDetector.AnomalyScore ingest(double measurement) {
        // Evaluate against baseline *prior* to or *including* current point
        double mean = (longTermAccumulator.getCount() > 5L) ? longTermAccumulator.getMean() : shortTermSmoother.getValue();
        double stdDev = (longTermAccumulator.getCount() > 5L) ? longTermAccumulator.getStandardDeviation() : 1.0;

        StreamingZScoreDetector.AnomalyScore score = detector.evaluate(measurement, mean, stdDev);

        // Update running states in strict O(1)
        longTermAccumulator.update(measurement);
        shortTermSmoother.update(measurement);

        return score;
    }

    public String getChannelName() {
        return channelName;
    }

    public synchronized WelfordAccumulator getLongTermAccumulator() {
        return new WelfordAccumulator(longTermAccumulator);
    }

    public synchronized double getSmoothedValue() {
        return shortTermSmoother.getValue();
    }

    public synchronized double getRunningMean() {
        return longTermAccumulator.getMean();
    }

    public synchronized double getRunningStandardDeviation() {
        return longTermAccumulator.getStandardDeviation();
    }

    public synchronized long getObservationCount() {
        return longTermAccumulator.getCount();
    }

    public synchronized void reset() {
        longTermAccumulator.reset();
        shortTermSmoother.reset();
    }
}
