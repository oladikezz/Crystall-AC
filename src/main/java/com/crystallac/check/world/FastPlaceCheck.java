package com.crystallac.check.world;

import com.crystallac.CrystallAC;
import com.crystallac.check.AbstractCheck;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;

/**
 * Heuristic check for FastPlace / Auto-Build:
 * - Detects impossible block placement frequencies (< 50ms interval)
 * - Detects placing multiple blocks within a single tick
 */
public class FastPlaceCheck extends AbstractCheck {

    public FastPlaceCheck(CrystallAC plugin) {
        super(plugin, CheckType.FASTPLACE);
    }

    public void handleBlockPlace(PlayerData data) {
        if (!isEnabled() || data.getExemptionManager().isExemptFromMovement(versionAdapter)) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastTime = data.getLastBlockPlaceTime();
        data.setLastBlockPlaceTime(now);
        data.setBlockPlacesThisTick(data.getBlockPlacesThisTick() + 1);

        // 1. Multiple blocks in same tick
        if (data.getBlockPlacesThisTick() > 1) {
            flag(data, 3.0, String.format("Multi-place tick (count=%d)", data.getBlockPlacesThisTick()));
            return;
        }

        // 2. Micro-interval between placements (vanilla standard place cooldown is ~4 ticks = 200ms)
        if (lastTime > 0) {
            long interval = now - lastTime;
            if (interval < 45) {
                flag(data, 2.5, String.format("Fast block placement (interval=%d ms)", interval));
            }
        }
    }
}
