package com.crystallac.check.movement;

import com.crystallac.CrystallAC;
import com.crystallac.check.AbstractCheck;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Heuristic check for Fly and Ground Spoof (NoFall) with active rubberband setback.
 */
public class FlyCheck extends AbstractCheck {

    public FlyCheck(CrystallAC plugin) {
        super(plugin, CheckType.FLY);
    }

    public void handleMovement(PlayerData data) {
        if (!isEnabled() || data.getExemptionManager().isExemptFromMovement(versionAdapter)) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null) return;

        double deltaY = data.getDeltaY();
        double lastDeltaY = data.getLastDeltaY();
        Location loc = player.getLocation();

        if (data.getExemptionManager().isNearClimbable(loc) || data.getExemptionManager().isNearSlime(loc)) {
            return;
        }

        boolean flagged = false;

        // 1. Upward acceleration check
        double maxUpward = getConfiguredThreshold("max_upward_acceleration", 0.42);
        PotionEffect jumpBoost = player.getPotionEffect(PotionEffectType.JUMP_BOOST);
        if (jumpBoost != null) {
            maxUpward += (jumpBoost.getAmplifier() + 1) * 0.1;
        }

        if (deltaY > maxUpward + 0.05) {
            flag(data, 3.0, String.format("Illegal upward deltaY=%.3f (max=%.3f)", deltaY, maxUpward));
            flagged = true;
        }

        // 2. Mid-air Hover / Flight
        boolean verifyGravity = plugin.getConfig().getBoolean("checks.fly.verify_gravity", true);
        if (!flagged && verifyGravity && !data.isServerOnGround() && !data.isClientOnGround()) {
            if (deltaY >= 0.0 && lastDeltaY <= 0.0 && deltaY < 0.35) {
                flag(data, 2.5, String.format("Mid-air hover/glide (deltaY=%.3f, lastDeltaY=%.3f)", deltaY, lastDeltaY));
                flagged = true;
            }
        }

        // 3. Ground Spoof / NoFall detection
        boolean groundSpoofCheck = plugin.getConfig().getBoolean("checks.fly.ground_spoof_detection", true);
        if (!flagged && groundSpoofCheck) {
            if (data.isClientOnGround() && !data.isServerOnGround() && loc.getY() % 1.0 > 0.0001) {
                flag(data, 2.0, String.format("Ground spoof (clientOnGround=true, serverOnGround=false, y=%.3f)", loc.getY()));
                flagged = true;
            }
        }

        if (flagged && plugin.getConfig().getBoolean("settings.setbacks_enabled", true)) {
            data.triggerSetback();
        }
    }
}
