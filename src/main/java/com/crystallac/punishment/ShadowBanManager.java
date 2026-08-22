package com.crystallac.punishment;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages shadow-banned players.
 * Quarantines suspected cheaters: cancels outgoing entity damage and hides them from legitimate players.
 */
public class ShadowBanManager {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> shadowBannedPlayers = new ConcurrentHashMap<>();

    public ShadowBanManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void shadowBan(Player player, int durationMinutes) {
        long expireTime = (durationMinutes <= 0) ? Long.MAX_VALUE : System.currentTimeMillis() + (durationMinutes * 60L * 1000L);
        shadowBannedPlayers.put(player.getUniqueId(), expireTime);

        boolean hide = plugin.getConfig().getBoolean("punishments.tier_3_shadow_ban.hide_from_tab_and_world", true);
        if (hide) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.getUniqueId().equals(player.getUniqueId()) && !online.hasPermission("crystallac.admin")) {
                    online.hidePlayer(plugin, player);
                }
            }
        }

        plugin.getLogger().info(String.format("[CrystallAC] Player %s has been SHADOW-BANNED for %d minutes.",
                player.getName(), durationMinutes));
    }

    public boolean isShadowBanned(Player player) {
        if (player == null) return false;
        Long expire = shadowBannedPlayers.get(player.getUniqueId());
        if (expire == null) return false;

        if (System.currentTimeMillis() > expire) {
            unShadowBan(player);
            return false;
        }
        return true;
    }

    public void unShadowBan(Player player) {
        shadowBannedPlayers.remove(player.getUniqueId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
    }

    public void cleanExpired() {
        long now = System.currentTimeMillis();
        shadowBannedPlayers.entrySet().removeIf(entry -> now > entry.getValue());
    }
}
