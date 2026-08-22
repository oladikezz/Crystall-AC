package com.crystallac.statistical;

/**
 * Calculates statistical Z-scores and flags anomalous outliers based on
 * personal and global baselines.
 */
public class AnomalyDetector {

    private final double zScoreThreshold;
    private final GlobalBaselineTracker globalBaseline;

    public AnomalyDetector(double zScoreThreshold, GlobalBaselineTracker globalBaseline) {
        this.zScoreThreshold = zScoreThreshold;
        this.globalBaseline = globalBaseline;
    }

    /**
     * Calculates personal Z-score: Z = (x - mu) / sigma
     */
    public double calculatePersonalZScore(double value, double mean, double stdDev) {
        if (stdDev <= 0.0001) return 0.0;
        return (value - mean) / stdDev;
    }

    /**
     * Calculates combined anomaly score for combat actions.
     * Evaluates CPS spike, rotation jitter, and reaction time.
     *
     * @param tracker Player's baseline tracker
     * @param currentCps Current click speed
     * @param currentCpsStdDev Current click interval standard deviation
     * @param currentRotDelta Current rotation delta
     * @return Anomaly evaluation result
     */
    public AnomalyResult evaluateCombatAnomaly(PlayerBaselineTracker tracker,
                                              double currentCps,
                                              double currentCpsStdDev,
                                              double currentRotDelta) {
        if (tracker == null) {
            return new AnomalyResult(false, 0.0, 1.0, "No baseline");
        }

        // Z-score for CPS
        double zCps = calculatePersonalZScore(currentCps, tracker.getEwmaCps(), tracker.getCpsStdDev());
        
        // Z-score for Low click jitter (Autoclickers show unnaturally low variance)
        double zLowJitter = (tracker.getEwmaCpsStdDev() - currentCpsStdDev) / (tracker.getCpsStdDev() + 0.1);

        // Z-score for Rotation Jump
        double zRot = calculatePersonalZScore(currentRotDelta, tracker.getEwmaRotationDelta(), tracker.getRotationStdDev());

        // Peak Z-score
        double maxZ = Math.max(Math.abs(zCps), Math.max(zLowJitter, Math.abs(zRot)));

        boolean isAnomalous = maxZ >= zScoreThreshold;
        double multiplier = isAnomalous ? 1.5 + Math.min(1.0, (maxZ - zScoreThreshold) * 0.2) : 1.0;

        String description = String.format("zCPS=%.2f, zJitter=%.2f, zRot=%.2f (maxZ=%.2f)", zCps, zLowJitter, zRot, maxZ);
        return new AnomalyResult(isAnomalous, maxZ, multiplier, description);
    }

    public static class AnomalyResult {
        private final boolean anomalous;
        private final double maxZScore;
        private final double vlMultiplier;
        private final String details;

        public AnomalyResult(boolean anomalous, double maxZScore, double vlMultiplier, String details) {
            this.anomalous = anomalous;
            this.maxZScore = maxZScore;
            this.vlMultiplier = vlMultiplier;
            this.details = details;
        }

        public boolean isAnomalous() { return anomalous; }
        public double getMaxZScore() { return maxZScore; }
        public double getVlMultiplier() { return vlMultiplier; }
        public String getDetails() { return details; }
    }
}
