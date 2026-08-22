package com.crystallac.check.combat;

import com.crystallac.CrystallAC;
import com.crystallac.check.AbstractCheck;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;

/**
 * Heuristic check for Autoclickers & Macros:
 * - Absolute maximum CPS thresholds
 * - Statistical timing consistency (standard deviation jitter analysis)
 * - Identical repeated click intervals (fixed ms delays)
 */
public class AutoclickerCheck extends AbstractCheck {

    public AutoclickerCheck(CrystallAC plugin) {
        super(plugin, CheckType.AUTOCLICKER);
    }

    public void handleClick(PlayerData data) {
        if (!isEnabled() || data.getExemptionManager().isExemptFromCombat(versionAdapter)) {
            return;
        }

        data.recordClick();

        double currentCps = data.getCps();
        double maxCps = getConfiguredThreshold("max_cps", 18.0);

        // 1. Hard CPS limit
        if (currentCps > maxCps) {
            flag(data, 2.5, String.format("High CPS (cps=%.1f, max=%.1f)", currentCps, maxCps));
            return;
        }

        // 2. Standard deviation consistency test (only meaningful when player is clicking fast >= 13 CPS)
        if (currentCps >= 13.0) {
            double stdDev = data.getClickIntervalStdDev();
            double minDeviation = plugin.getConfig().getDouble("checks.autoclicker.min_deviation_ms", 4.8);

            if (stdDev < minDeviation) {
                flag(data, 3.0, String.format("Low click jitter/macro (cps=%.1f, stdDev=%.2f ms, min=%.2f ms)",
                        currentCps, stdDev, minDeviation));
            }
        }
    }
}
