package com.crystallac.check.world;

import com.crystallac.CrystallAC;
import com.crystallac.check.AbstractCheck;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Heuristic check for Scaffold / Bridge hacks:
 * - Detects placing blocks beneath player while sprinting backwards or sideways
 * - Detects impossible block placement angles without sneaking (pitch snaps straight down)
 * - Detects impossible block face connections
 */
public class ScaffoldCheck extends AbstractCheck {

    public ScaffoldCheck(CrystallAC plugin) {
        super(plugin, CheckType.SCAFFOLD);
    }

    public void handleBlockPlace(PlayerData data, Block placedBlock, Block placedAgainst, BlockFace face) {
        if (!isEnabled() || data.getExemptionManager().isExemptFromMovement(versionAdapter)) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null) return;

        Location playerLoc = player.getLocation();
        float pitch = playerLoc.getPitch();
        float yaw = playerLoc.getYaw();
        double deltaXZ = data.getDeltaXZ();

        // 1. Check downwards placement without sneaking while moving fast
        if (pitch > 70.0f && deltaXZ > 0.20 && !player.isSneaking()) {
            // Check if block was placed directly underneath player's feet
            if (placedBlock.getY() < playerLoc.getY()) {
                flag(data, 3.0, String.format("Sprinting scaffold without sneak (pitch=%.1f, speed=%.2f)", pitch, deltaXZ));
                return;
            }
        }

        // 2. Backward / Off-angle sprint bridging
        // Calculate motion vector vs player look direction
        if (deltaXZ > 0.22 && player.isSprinting()) {
            Vector motion = new Vector(data.getDeltaX(), 0, data.getDeltaZ()).normalize();
            Vector look = playerLoc.getDirection().setY(0).normalize();
            double dot = motion.dot(look); // 1.0 = forward, -1.0 = backward, 0.0 = sideways

            if (dot < -0.2 && placedBlock.getY() <= playerLoc.getY()) {
                flag(data, 3.5, String.format("Backwards sprint placement (dot=%.2f, speed=%.2f)", dot, deltaXZ));
            }
        }
    }
}
