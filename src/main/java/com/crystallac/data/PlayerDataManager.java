package com.crystallac.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages concurrent lifecycle of PlayerData instances across online players.
 */
public class PlayerDataManager {

    private final Map<UUID, PlayerData> dataMap = new ConcurrentHashMap<>();
    private final double ewmaAlpha;
    private final int featureBufferSeconds;

    public PlayerDataManager(FileConfiguration config) {
        this.ewmaAlpha = config.getDouble("statistical_layer.ewma_alpha", 0.15);
        this.featureBufferSeconds = config.getInt("incremental_ml.feature_buffer_duration_seconds", 300);
    }

    public PlayerData registerPlayer(Player player) {
        return dataMap.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerData(uuid, ewmaAlpha, featureBufferSeconds));
    }

    public void unregisterPlayer(Player player) {
        dataMap.remove(player.getUniqueId());
    }

    public PlayerData getPlayerData(Player player) {
        if (player == null) return null;
        return dataMap.get(player.getUniqueId());
    }

    public PlayerData getPlayerData(UUID uuid) {
        return dataMap.get(uuid);
    }

    public Collection<PlayerData> getAllPlayerData() {
        return dataMap.values();
    }

    public void tickAll() {
        for (PlayerData data : dataMap.values()) {
            data.getExemptionManager().tick();
            data.setAttacksThisTick(0); // Reset tick-based counters
        }
    }
}
