package com.crystallac.check.combat;

import com.crystallac.CrystallAC;
import com.crystallac.check.AbstractCheck;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Heuristic check for Killaura:
 * - Multi-target attacks within a single tick
 * - Instantaneous rotational snaps directly onto target hitboxes
 * - Attacks against targets outside vanilla field-of-view (behind player)
 * - Attacks without prior camera adjustment / arm swing
 */
public class KillauraCheck extends AbstractCheck {

    public KillauraCheck(CrystallAC plugin) {
        super(plugin, CheckType.KILLAURA);
    }

    public void handleAttack(PlayerData data, Entity target) {
        if (!isEnabled() || data.getExemptionManager().isExemptFromCombat(versionAdapter)) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null || target == null) return;

        // 1. Check Multi-Target in single tick
        int attacks = data.getAttacksThisTick();
        int maxAllowedTargets = plugin.getConfig().getInt("checks.killaura.max_targets_per_tick", 1);
        if (attacks > maxAllowedTargets) {
            flag(data, 4.0, String.format("Multi-Target (hits=%d, max=%d)", attacks, maxAllowedTargets));
            return;
        }

        // 2. Field-of-View (FOV) / Behind-back attack check
        Location eyeLoc = player.getEyeLocation();
        Vector toTarget = target.getLocation().toVector().subtract(eyeLoc.toVector()).normalize();
        Vector direction = eyeLoc.getDirection().normalize();

        double dot = direction.dot(toTarget); // dot product in [-1.0, 1.0]
        double angleDegrees = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));

        if (angleDegrees > 105.0) {
            flag(data, 5.0, String.format("Hit outside FOV (angle=%.1f deg)", angleDegrees));
            return;
        }

        // 3. Instantaneous Angle Snap onto unseen/new target
        double snapThreshold = getConfiguredThreshold("angle_snap_threshold", 38.5);
        if (data.getLastAttackedEntity() != null && !data.getLastAttackedEntity().getUniqueId().equals(target.getUniqueId())) {
            float deltaYaw = data.getDeltaYaw();
            float deltaPitch = data.getDeltaPitch();
            double totalRot = Math.hypot(deltaYaw, deltaPitch);

            if (totalRot > snapThreshold && angleDegrees < 12.0) {
                flag(data, 3.5, String.format("Target Snap (rotDelta=%.1f deg, fov=%.1f deg)", totalRot, angleDegrees));
            }
        }

        // 4. Swing order check
        boolean requireSwing = plugin.getConfig().getBoolean("checks.killaura.require_rotation_on_swing", true);
        if (requireSwing) {
            long timeSinceSwing = System.currentTimeMillis() - data.getLastSwingTime();
            if (timeSinceSwing > 400 && data.getDeltaYaw() < 0.001 && data.getDeltaPitch() < 0.001) {
                flag(data, 2.0, String.format("Hit without swing/rotation (delay=%dms)", timeSinceSwing));
            }
        }
    }
}
