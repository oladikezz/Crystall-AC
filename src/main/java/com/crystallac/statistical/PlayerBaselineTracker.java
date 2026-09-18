package com.crystallac.statistical;

import org.crystall.analytics.EWMASmoother;
import org.crystall.analytics.WelfordAccumulator;

/**
 * Tracks real-time statistical baselines for an individual entity stream
 * by wrapping decoupled {@link WelfordAccumulator} and {@link EWMASmoother} instances.
 */
public class PlayerBaselineTracker {

    private final double alpha;

    // EWMA smoothers
    private final EWMASmoother ewmaCps;
    private final EWMASmoother ewmaCpsStdDev;
    private final EWMASmoother ewmaRotationDelta;
    private final EWMASmoother ewmaReactionTimeMs;
    private final EWMASmoother ewmaMovementSpeed;

    // Welford running accumulators
    private final WelfordAccumulator cpsAccumulator = new WelfordAccumulator();
    private final WelfordAccumulator rotAccumulator = new WelfordAccumulator();
    private final WelfordAccumulator reactionAccumulator = new WelfordAccumulator();

    public PlayerBaselineTracker(double alpha) {
        this.alpha = alpha;
        this.ewmaCps = new EWMASmoother(alpha, 8.0);
        this.ewmaCpsStdDev = new EWMASmoother(alpha, 15.0);
        this.ewmaRotationDelta = new EWMASmoother(alpha, 12.0);
        this.ewmaReactionTimeMs = new EWMASmoother(alpha, 180.0);
        this.ewmaMovementSpeed = new EWMASmoother(alpha, 0.25);
    }

    public synchronized void recordCpsSample(double cps, double intervalStdDev) {
        ewmaCps.update(cps);
        ewmaCpsStdDev.update(intervalStdDev);
        cpsAccumulator.update(cps);
    }

    public synchronized void recordRotationSample(double deltaAngle) {
        ewmaRotationDelta.update(deltaAngle);
        rotAccumulator.update(deltaAngle);
    }

    public synchronized void recordReactionSample(double reactionTimeMs) {
        ewmaReactionTimeMs.update(reactionTimeMs);
        reactionAccumulator.update(reactionTimeMs);
    }

    public synchronized void recordMovementSpeed(double speed) {
        ewmaMovementSpeed.update(speed);
    }

    public synchronized double getCpsVariance() {
        return cpsAccumulator.getCount() < 2 ? 1.0 : cpsAccumulator.getSampleVariance();
    }

    public synchronized double getCpsStdDev() {
        return Math.max(0.5, cpsAccumulator.getStandardDeviation());
    }

    public synchronized double getRotationVariance() {
        return rotAccumulator.getCount() < 2 ? 1.0 : rotAccumulator.getSampleVariance();
    }

    public synchronized double getRotationStdDev() {
        return Math.max(1.0, rotAccumulator.getStandardDeviation());
    }

    public synchronized double getReactionVariance() {
        return reactionAccumulator.getCount() < 2 ? 100.0 : reactionAccumulator.getSampleVariance();
    }

    public synchronized double getReactionStdDev() {
        return Math.max(5.0, reactionAccumulator.getStandardDeviation());
    }

    // Getters
    public double getEwmaCps() { return ewmaCps.getValue(); }
    public double getEwmaCpsStdDev() { return ewmaCpsStdDev.getValue(); }
    public double getEwmaRotationDelta() { return ewmaRotationDelta.getValue(); }
    public double getEwmaReactionTimeMs() { return ewmaReactionTimeMs.getValue(); }
    public double getEwmaMovementSpeed() { return ewmaMovementSpeed.getValue(); }
    public int getCpsCount() { return (int) cpsAccumulator.getCount(); }
    public int getRotCount() { return (int) rotAccumulator.getCount(); }

    public WelfordAccumulator getCpsAccumulator() { return cpsAccumulator; }
    public WelfordAccumulator getRotAccumulator() { return rotAccumulator; }
    public WelfordAccumulator getReactionAccumulator() { return reactionAccumulator; }
}
