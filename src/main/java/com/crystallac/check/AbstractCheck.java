package com.crystallac.check;

import com.crystallac.CrystallAC;
import com.crystallac.adapter.VersionAdapter;
import com.crystallac.data.PlayerData;
import com.crystallac.ml.IncrementalModelTuner;
import com.crystallac.punishment.ViolationManager;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Base class for all heuristic anti-cheat checks.
 */
public abstract class AbstractCheck {

    protected final CrystallAC plugin;
    protected final CheckType checkType;
    protected final VersionAdapter versionAdapter;
    protected final ViolationManager violationManager;
    protected final IncrementalModelTuner modelTuner;

    public AbstractCheck(CrystallAC plugin, CheckType checkType) {
        this.plugin = plugin;
        this.checkType = checkType;
        this.versionAdapter = plugin.getVersionAdapter();
        this.violationManager = plugin.getViolationManager();
        this.modelTuner = plugin.getModelTuner();
    }

    public CheckType getCheckType() {
        return checkType;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("checks." + checkType.name().toLowerCase() + ".enabled", true);
    }

    /**
     * Gets a configured double value, adjusted by the incremental model tuner if active.
     */
    protected double getConfiguredThreshold(String path, double defaultValue) {
        FileConfiguration config = plugin.getConfig();
        double baseVal = config.getDouble("checks." + checkType.name().toLowerCase() + "." + path, defaultValue);
        double multiplier = (modelTuner != null) ? modelTuner.getThresholdMultiplier(checkType) : 1.0;
        return baseVal * multiplier;
    }

    /**
     * Flags a violation for the player.
     *
     * @param data Player's data context
     * @param vl Amount of VL to add
     * @param details Descriptive debug information
     */
    protected void flag(PlayerData data, double vl, String details) {
        violationManager.handleViolation(data, checkType, vl, details);
    }
}
