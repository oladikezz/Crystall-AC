package com.crystallac.check.combat;

import com.crystallac.CrystallAC;
import com.crystallac.check.AbstractCheck;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * Heuristic check for Reach with active attack cancellation support.
 */
public class ReachCheck extends AbstractCheck {

    public ReachCheck(CrystallAC plugin) {
        super(plugin, CheckType.REACH);
    }

    public boolean handleAttack(PlayerData data, Entity target) {
        if (!isEnabled() || data.getExemptionManager().isExemptFromCombat(versionAdapter)) {
            return false;
        }

        Player player = data.getPlayer();
        if (player == null || target == null) return false;

        Location eyeLocation = player.getEyeLocation();
        BoundingBox targetBox = versionAdapter.getBoundingBox(target);

        double closestX = Math.max(targetBox.getMinX(), Math.min(eyeLocation.getX(), targetBox.getMaxX()));
        double closestY = Math.max(targetBox.getMinY(), Math.min(eyeLocation.getY(), targetBox.getMaxY()));
        double closestZ = Math.max(targetBox.getMinZ(), Math.min(eyeLocation.getZ(), targetBox.getMaxZ()));

        Vector closestPoint = new Vector(closestX, closestY, closestZ);
        double distance = eyeLocation.toVector().distance(closestPoint);

        int attackerPing = versionAdapter.getPing(player);
        int victimPing = (target instanceof Player victim) ? versionAdapter.getPing(victim) : 0;
        double pingMultiplier = plugin.getConfig().getDouble("checks.reach.ping_compensation_multiplier", 0.0035);
        double pingBuffer = Math.min(0.40, (attackerPing + victimPing) * pingMultiplier);

        double baseMaxReach = getConfiguredThreshold("base_max_reach", 3.05);
        double hitboxExpansion = plugin.getConfig().getDouble("checks.reach.hitbox_expansion", 0.10);

        double maxAllowedDistance = baseMaxReach + pingBuffer + hitboxExpansion;

        data.captureSnapshot(distance);

        if (distance > maxAllowedDistance) {
            double excess = distance - maxAllowedDistance;
            double vl = Math.min(10.0, excess * 6.0 + 1.0);
            flag(data, vl, String.format("dist=%.2f blocks (max=%.2f, ping=%dms)", distance, maxAllowedDistance, attackerPing));
            return true; // Cancel attack
        }
        return false;
    }
}
