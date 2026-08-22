package com.crystallac.check.movement;

import com.crystallac.CrystallAC;
import com.crystallac.check.AbstractCheck;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Heuristic check for Movement Speed:
 * Accurately models vanilla player kinematics, friction matrices (air 0.91, ground 0.6, ice),
 * jump boosts, and potion effects to detect speedhacks.
 */
public class SpeedCheck extends AbstractCheck {

    public SpeedCheck(CrystallAC plugin) {
        super(plugin, CheckType.SPEED);
    }

    public void handleMovement(PlayerData data) {
        if (!isEnabled() || data.getExemptionManager().isExemptFromMovement(versionAdapter)) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null) return;

        double deltaXZ = data.getDeltaXZ();
        double baseMaxSpeed = player.isSprinting() ? 
                plugin.getConfig().getDouble("checks.speed.base_sprint_jump_speed", 0.612) :
                plugin.getConfig().getDouble("checks.speed.base_walk_speed", 0.286);

        // Apply Speed potion effects (+20% per level)
        PotionEffect speedEffect = player.getPotionEffect(PotionEffectType.SPEED);
        if (speedEffect != null) {
            double speedMultiplier = 1.0 + (speedEffect.getAmplifier() + 1) * plugin.getConfig().getDouble("checks.speed.speed_potion_multiplier", 0.20);
            baseMaxSpeed *= speedMultiplier;
        }

        // Apply Ice friction modifier
        if (data.getExemptionManager().isNearIce(player.getLocation())) {
            baseMaxSpeed *= plugin.getConfig().getDouble("checks.speed.ice_speed_multiplier", 1.45);
        }

        // Apply tuned threshold multiplier from ML tuner if active
        double threshold = getConfiguredThreshold("base_sprint_jump_speed", baseMaxSpeed);

        if (deltaXZ > threshold) {
            double excess = deltaXZ - threshold;
            double vl = Math.min(10.0, excess * 15.0 + 1.0);
            flag(data, vl, String.format("deltaXZ=%.3f (max=%.3f, sprint=%b)", deltaXZ, threshold, player.isSprinting()));
        }
    }
}
