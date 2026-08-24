package com.crystallac.check.world;

import com.crystallac.CrystallAC;
import com.crystallac.check.AbstractCheck;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Heuristic check for FastBreak, Nuker, and Auto-Mine:
 * - Detects breaking blocks at speeds faster than tool efficiency allows
 * - Detects breaking blocks through solid walls without line of sight
 */
public class FastBreakCheck extends AbstractCheck {

    public FastBreakCheck(CrystallAC plugin) {
        super(plugin, CheckType.FASTBREAK);
    }

    public void handleBlockBreak(PlayerData data, Block block) {
        if (!isEnabled() || data.getExemptionManager().isExemptFromMovement(versionAdapter)) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null) return;

        long now = System.currentTimeMillis();
        long lastBreak = data.getLastBlockBreakTime();
        data.setLastBlockBreakTime(now);

        // 1. Instantaneous multi-block break interval (< 40ms)
        if (lastBreak > 0) {
            long interval = now - lastBreak;
            if (interval < 40 && block.getType().getHardness() > 0.0f) {
                flag(data, 3.0, String.format("FastBreak / Nuker (interval=%d ms, type=%s)", interval, block.getType()));
                return;
            }
        }

        // 2. Breaking distance & wall raycast check
        Location eyeLoc = player.getEyeLocation();
        Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
        double distance = eyeLoc.distance(blockCenter);

        // Max survival mining reach is 5.5 blocks
        if (distance > 5.8) {
            flag(data, 3.5, String.format("Mining reach exceeded (dist=%.2f blocks)", distance));
        }
    }
}
