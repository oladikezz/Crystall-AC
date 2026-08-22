package com.crystallac.statistical;

/**
 * Maintains server-wide aggregated baseline statistics across all active players.
 * Used for comparative anomaly detection (evaluating if player deviates from general server population).
 */
public class GlobalBaselineTracker {

    private double globalAvgCps = 8.5;
    private double globalCpsStdDev = 3.2;

    private double globalAvgRotDelta = 14.0;
    private double globalRotStdDev = 8.5;

    private double globalAvgReactionMs = 190.0;
    private double globalReactionStdDev = 45.0;

    private final double alpha = 0.05; // Slower adaptation rate for global metrics

    public synchronized void updateGlobalMetrics(PlayerBaselineTracker tracker) {
        if (tracker == null || tracker.getCpsCount() < 10) return;

        // Smoothly adjust global metrics using valid player baselines
        globalAvgCps = alpha * tracker.getEwmaCps() + (1.0 - alpha) * globalAvgCps;
        globalCpsStdDev = alpha * Math.max(0.5, tracker.getCpsStdDev()) + (1.0 - alpha) * globalCpsStdDev;

        globalAvgRotDelta = alpha * tracker.getEwmaRotationDelta() + (1.0 - alpha) * globalAvgRotDelta;
        globalRotStdDev = alpha * Math.max(1.0, tracker.getRotationStdDev()) + (1.0 - alpha) * globalRotStdDev;

        globalAvgReactionMs = alpha * tracker.getEwmaReactionTimeMs() + (1.0 - alpha) * globalAvgReactionMs;
        globalReactionStdDev = alpha * Math.max(5.0, tracker.getReactionStdDev()) + (1.0 - alpha) * globalReactionStdDev;
    }

    public double getGlobalAvgCps() { return globalAvgCps; }
    public double getGlobalCpsStdDev() { return globalCpsStdDev; }
    public double getGlobalAvgRotDelta() { return globalAvgRotDelta; }
    public double getGlobalRotStdDev() { return globalRotStdDev; }
    public double getGlobalAvgReactionMs() { return globalAvgReactionMs; }
    public double getGlobalReactionStdDev() { return globalReactionStdDev; }
}
