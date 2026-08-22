package com.crystallac.data;

import com.crystallac.adapter.VersionAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Manages exemptions to eliminate false positives caused by vanilla mechanics,
 * network jitter, lag spikes, knockbacks, teleports, and environmental blocks.
 */
public class ExemptionManager {

    private final PlayerData playerData;
    private int teleportTicks = 0;
    private int respawnTicks = 0;
    private int knockbackTicks = 0;
    private int riptideTicks = 0;
    private int flightTicks = 0;

    public ExemptionManager(PlayerData playerData) {
        this.playerData = playerData;
    }

    public void tick() {
        if (teleportTicks > 0) teleportTicks--;
        if (respawnTicks > 0) respawnTicks--;
        if (knockbackTicks > 0) knockbackTicks--;
        if (riptideTicks > 0) riptideTicks--;
        if (flightTicks > 0) flightTicks--;
    }

    public void handleTeleport() {
        this.teleportTicks = 20;
    }

    public void handleRespawn() {
        this.respawnTicks = 40;
    }

    public void handleKnockback() {
        this.knockbackTicks = 15;
    }

    public void handleRiptide() {
        this.riptideTicks = 30;
    }

    public boolean isExemptFromMovement(VersionAdapter adapter) {
        Player player = playerData.getPlayer();
        if (player == null || !player.isOnline()) return true;

        // Bypass permission
        if (player.hasPermission("crystallac.bypass")) return true;

        // Creative or spectator modes
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE ||
            player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return true;
        }

        // Active server-side flight / allowFlight
        if (player.getAllowFlight() || player.isFlying()) {
            return true;
        }

        // Recent teleport or respawn
        if (teleportTicks > 0 || respawnTicks > 0) {
            return true;
        }

        // Gliding with Elytra or Riptide Trident
        if (adapter.isGliding(player) || adapter.isRiptiding(player) || riptideTicks > 0) {
            return true;
        }

        // Vehicle riding (boat, minecart, horse)
        if (player.isInsideVehicle()) {
            return true;
        }

        // Knockback velocity
        if (knockbackTicks > 0) {
            return true;
        }

        // Liquid immersion or swimming
        if (adapter.isSwimming(player) || isSubmergedInLiquid(player)) {
            return true;
        }

        // Server TPS lag exemption (if server TPS drops below 18.0)
        double currentTps = getRecentTps();
        if (currentTps > 0 && currentTps < 18.0) {
            return true;
        }

        // Extreme ping spikes (> 400ms)
        if (adapter.getPing(player) > 400) {
            return true;
        }

        return false;
    }

    public boolean isExemptFromCombat(VersionAdapter adapter) {
        Player player = playerData.getPlayer();
        if (player == null || !player.isOnline()) return true;

        if (player.hasPermission("crystallac.bypass")) return true;
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE ||
            player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return true;
        }

        if (teleportTicks > 0 || respawnTicks > 0) return true;

        return false;
    }

    public boolean isNearClimbable(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    Material mat = b.getType();
                    if (mat == Material.LADDER || mat == Material.VINE || 
                        mat == Material.SCAFFOLDING || mat == Material.TWISTING_VINES ||
                        mat == Material.WEEPING_VINES) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isNearIce(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 0; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    Material mat = b.getType();
                    if (mat == Material.ICE || mat == Material.PACKED_ICE || mat == Material.BLUE_ICE || mat == Material.FROSTED_ICE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isNearSlime(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 0; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    if (b.getType() == Material.SLIME_BLOCK || b.getType() == Material.HONEY_BLOCK) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isNearCobweb(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        Block b = loc.getBlock();
        return b.getType() == Material.COBWEB || loc.clone().add(0, 1, 0).getBlock().getType() == Material.COBWEB;
    }

    private boolean isSubmergedInLiquid(Player player) {
        Location loc = player.getLocation();
        Block b = loc.getBlock();
        Block bHead = loc.clone().add(0, 1.6, 0).getBlock();
        return b.isLiquid() || bHead.isLiquid();
    }

    private double getRecentTps() {
        try {
            double[] tps = Bukkit.getTPS();
            if (tps != null && tps.length > 0) {
                return tps[0];
            }
        } catch (Throwable ignored) {}
        return 20.0;
    }
}
