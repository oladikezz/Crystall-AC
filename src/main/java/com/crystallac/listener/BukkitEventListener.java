package com.crystallac.listener;

import com.crystallac.CrystallAC;
import com.crystallac.check.CheckManager;
import com.crystallac.data.PlayerData;
import com.crystallac.data.PlayerDataManager;
import com.crystallac.punishment.ShadowBanManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

/**
 * Intercepts high-level Bukkit events for movement, combat, interactions, blocks, and inventory states.
 */
public class BukkitEventListener implements Listener {

    private final CrystallAC plugin;
    private final PlayerDataManager dataManager;
    private final CheckManager checkManager;
    private final ShadowBanManager shadowBanManager;

    public BukkitEventListener(CrystallAC plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.checkManager = plugin.getCheckManager();
        this.shadowBanManager = plugin.getShadowBanManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        dataManager.registerPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        dataManager.unregisterPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerData data = dataManager.getPlayerData(player);
        if (data == null) return;

        data.updateMovement(event.getTo(), player.isOnGround());

        // Movement & Inventory checks
        checkManager.dispatchMovement(data);

        // Camera rotation check
        if (data.getDeltaYaw() > 0.0f || data.getDeltaPitch() > 0.0f) {
            checkManager.dispatchRotation(data);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        // Shadow Ban Enforcement
        if (shadowBanManager.isShadowBanned(attacker)) {
            event.setCancelled(true);
            return;
        }

        PlayerData data = dataManager.getPlayerData(attacker);
        if (data == null) return;

        Entity victim = event.getEntity();
        data.setAttacksThisTick(data.getAttacksThisTick() + 1);
        data.setLastAttackTime(System.currentTimeMillis());
        data.setLastAttackedEntity(victim);

        // Active mitigation: cancel damage if reach or killaura detects severe anomaly
        boolean cancelAttack = checkManager.dispatchAttack(data, victim);
        if (cancelAttack && plugin.getConfig().getBoolean("settings.cancel_illegal_attacks", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAnimation(PlayerAnimationEvent event) {
        if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
            PlayerData data = dataManager.getPlayerData(event.getPlayer());
            if (data != null) {
                data.setLastSwingTime(System.currentTimeMillis());
                checkManager.dispatchClick(data);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        PlayerData data = dataManager.getPlayerData(event.getPlayer());
        if (data != null) {
            checkManager.dispatchBlockPlace(data, event.getBlockPlaced(), event.getBlockAgainst(), event.getBlockAgainst().getFace(event.getBlockPlaced()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        PlayerData data = dataManager.getPlayerData(event.getPlayer());
        if (data != null) {
            checkManager.dispatchBlockBreak(data, event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            PlayerData data = dataManager.getPlayerData(player);
            if (data != null) {
                data.setInventoryOpen(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            PlayerData data = dataManager.getPlayerData(player);
            if (data != null) {
                data.setInventoryOpen(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        PlayerData data = dataManager.getPlayerData(event.getPlayer());
        if (data != null) {
            data.getExemptionManager().handleTeleport();
            data.setValidLocation(event.getTo());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        PlayerData data = dataManager.getPlayerData(event.getPlayer());
        if (data != null) {
            data.getExemptionManager().handleRespawn();
            data.setValidLocation(event.getRespawnLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent event) {
        PlayerData data = dataManager.getPlayerData(event.getPlayer());
        if (data != null) {
            data.getExemptionManager().handleKnockback();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRiptide(PlayerRiptideEvent event) {
        PlayerData data = dataManager.getPlayerData(event.getPlayer());
        if (data != null) {
            data.getExemptionManager().handleRiptide();
        }
    }
}
