package com.crystallac.statistical;

/**
 * Tracks real-time statistical baselines for an individual player using EWMA
 * (Exponentially Weighted Moving Average) and Welford's algorithm for running variance.
 * Requires zero pre-existing offline datasets.
 */
public class PlayerBaselineTracker {

    private final double alpha; // Smoothing factor (typically 0.15)
    
    // EWMA state variables
    private double ewmaCps = 8.0;
    private double ewmaCpsStdDev = 15.0;
    private double ewmaRotationDelta = 12.0;
    private double ewmaReactionTimeMs = 180.0;
    private double ewmaMovementSpeed = 0.25;

    // Welford's running statistics for CPS
    private int cpsCount = 0;
    private double cpsMean = 0.0;
    private double cpsM2 = 0.0;

    // Welford's running statistics for Rotation Delta
    private int rotCount = 0;
    private double rotMean = 0.0;
    private double rotM2 = 0.0;

    // Welford's running statistics for Reaction Time
    private int reactionCount = 0;
    private double reactionMean = 0.0;
    private double reactionM2 = 0.0;

    public PlayerBaselineTracker(double alpha) {
        this.alpha = alpha;
    }

    public synchronized void recordCpsSample(double cps, double intervalStdDev) {
        // Update EWMA
        ewmaCps = alpha * cps + (1.0 - alpha) * ewmaCps;
        ewmaCpsStdDev = alpha * intervalStdDev + (1.0 - alpha) * ewmaCpsStdDev;

        // Update Welford
        cpsCount++;
        double delta = cps - cpsMean;
        cpsMean += delta / cpsCount;
        double delta2 = cps - cpsMean;
        cpsM2 += delta * delta2;
    }

    public synchronized void recordRotationSample(double deltaAngle) {
        ewmaRotationDelta = alpha * deltaAngle + (1.0 - alpha) * ewmaRotationDelta;

        rotCount++;
        double delta = deltaAngle - rotMean;
        rotMean += delta / rotCount;
        double delta2 = deltaAngle - rotMean;
        rotM2 += delta * delta2;
    }

    public synchronized void recordReactionSample(double reactionTimeMs) {
        ewmaReactionTimeMs = alpha * reactionTimeMs + (1.0 - alpha) * ewmaReactionTimeMs;

        reactionCount++;
        double delta = reactionTimeMs - reactionMean;
        reactionMean += delta / reactionCount;
        double delta2 = reactionTimeMs - reactionMean;
        reactionM2 += delta * delta2;
    }

    public synchronized void recordMovementSpeed(double speed) {
        ewmaMovementSpeed = alpha * speed + (1.0 - alpha) * ewmaMovementSpeed;
    }

    public double getCpsVariance() {
        return cpsCount < 2 ? 1.0 : cpsM2 / (cpsCount - 1);
    }

    public double getCpsStdDev() {
        return Math.sqrt(getCpsVariance());
    }

    public double getRotationVariance() {
        return rotCount < 2 ? 1.0 : rotM2 / (rotCount - 1);
    }

    public double getRotationStdDev() {
        return Math.sqrt(getRotationVariance());
    }

    public double getReactionVariance() {
        return reactionCount < 2 ? 100.0 : reactionM2 / (reactionCount - 1);
    }

    public double getReactionStdDev() {
        return Math.sqrt(getReactionVariance());
    }

    // Getters
    public double getEwmaCps() { return ewmaCps; }
    public double getEwmaCpsStdDev() { return ewmaCpsStdDev; }
    public double getEwmaRotationDelta() { return ewmaRotationDelta; }
    public double getEwmaReactionTimeMs() { return ewmaReactionTimeMs; }
    public double getEwmaMovementSpeed() { return ewmaMovementSpeed; }
    public int getCpsCount() { return cpsCount; }
    public int getRotCount() { return rotCount; }
}
