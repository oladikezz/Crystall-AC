package com.crystallac.ml;

import com.crystallac.check.CheckType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Optional incremental retraining module that fine-tunes heuristic check thresholds
 * based on verified moderator bans accumulated in SQLite.
 *
 * Stays dormant until the configured sample threshold (e.g. >= 25 samples) is met.
 */
public class IncrementalModelTuner {

    private final BanDatasetRepository repository;
    private final Logger logger;
    private final boolean enabled;
    private final int minSamples;
    private final double maxAdjustment;
    private final double learningRate;

    // Computed multiplier adjustments for heuristic thresholds (default 1.0 = unchanged)
    private final Map<CheckType, Double> thresholdMultipliers = new EnumMap<>(CheckType.class);

    public IncrementalModelTuner(FileConfiguration config, BanDatasetRepository repository, Logger logger) {
        this.repository = repository;
        this.logger = logger;
        this.enabled = config.getBoolean("incremental_ml.enabled", false);
        this.minSamples = config.getInt("incremental_ml.min_samples_to_retrain", 25);
        this.maxAdjustment = config.getDouble("incremental_ml.max_threshold_adjustment_percent", 0.15);
        this.learningRate = config.getDouble("incremental_ml.learning_rate", 0.01);

        for (CheckType type : CheckType.values()) {
            thresholdMultipliers.put(type, 1.0);
        }
    }

    /**
     * Checks if enough confirmed ban samples exist and retrains/calibrates heuristic thresholds.
     */
    public synchronized void evaluateAndRetrain() {
        if (!enabled) {
            return;
        }

        int totalSamples = repository.getTotalSampleCount();
        if (totalSamples < minSamples) {
            logger.info(String.format("[CrystallAC-ML] Incremental tuner dormant: %d/%d labeled ban samples gathered.",
                    totalSamples, minSamples));
            return;
        }

        logger.info(String.format("[CrystallAC-ML] Sufficient labeled samples (%d) found. Performing threshold calibration...", totalSamples));

        // Evaluate sample counts per check type and apply bounded sensitivity tuning
        for (CheckType check : CheckType.values()) {
            int samples = repository.getSampleCount(check.name());
            if (samples >= 5) {
                // Minor gradient adjustment: as confirmed bans accumulate, tighten threshold slightly within safety cap
                double adjustment = Math.min(maxAdjustment, samples * learningRate);
                // Multiplier < 1.0 makes strict threshold slightly tighter (e.g., 0.95 = 5% tighter)
                double calibratedMultiplier = Math.max(1.0 - maxAdjustment, 1.0 - adjustment);
                thresholdMultipliers.put(check, calibratedMultiplier);

                logger.info(String.format("[CrystallAC-ML] Calibrated %s threshold multiplier to %.3f based on %d ban samples.",
                        check.name(), calibratedMultiplier, samples));
            }
        }
    }

    public double getThresholdMultiplier(CheckType type) {
        return thresholdMultipliers.getOrDefault(type, 1.0);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
