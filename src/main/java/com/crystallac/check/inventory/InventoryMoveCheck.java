package com.crystallac.check.inventory;

import com.crystallac.CrystallAC;
import com.crystallac.check.AbstractCheck;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * Heuristic check for InventoryMove / AutoTotem / ChestStealer:
 * In vanilla client, moving (sprinting/jumping) is impossible while an inventory container is open.
 */
public class InventoryMoveCheck extends AbstractCheck {

    public InventoryMoveCheck(CrystallAC plugin) {
        super(plugin, CheckType.INVENTORY);
    }

    public void handleMovement(PlayerData data) {
        if (!isEnabled() || data.getExemptionManager().isExemptFromMovement(versionAdapter)) {
            return;
        }

        Player player = data.getPlayer();
        if (player == null) return;

        // If player has an open container inventory and moves at full sprint speed
        if (data.isInventoryOpen()) {
            double deltaXZ = data.getDeltaXZ();
            if (deltaXZ > 0.24 && (player.isSprinting() || data.getDeltaY() > 0.1)) {
                flag(data, 2.5, String.format("Moving with open inventory (speed=%.2f, sprint=%b)", deltaXZ, player.isSprinting()));
            }
        }
    }
}
