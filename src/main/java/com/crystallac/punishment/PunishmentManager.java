package com.crystallac.punishment;

import com.crystallac.check.CheckType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles multi-tiered progressive enforcement:
 * Tier 1: WARN
 * Tier 2: KICK
 * Tier 3: SHADOW_BAN
 * Tier 4: BAN
 */
public class PunishmentManager {

    private final JavaPlugin plugin;
    private final ShadowBanManager shadowBanManager;

    public PunishmentManager(JavaPlugin plugin, ShadowBanManager shadowBanManager) {
        this.plugin = plugin;
        this.shadowBanManager = shadowBanManager;
    }

    public void evaluatePunishment(Player player, CheckType check, double totalVl) {
        FileConfiguration config = plugin.getConfig();

        double warnThreshold = config.getDouble("punishments.tier_1_warn.vl_threshold", 10.0);
        double kickThreshold = config.getDouble("punishments.tier_2_kick.vl_threshold", 25.0);
        double shadowBanThreshold = config.getDouble("punishments.tier_3_shadow_ban.vl_threshold", 45.0);
        double banThreshold = config.getDouble("punishments.tier_4_ban.vl_threshold", 70.0);

        if (totalVl >= banThreshold && config.getBoolean("punishments.tier_4_ban.enabled", true)) {
            executeBan(player, check, totalVl);
        } else if (totalVl >= shadowBanThreshold && config.getBoolean("punishments.tier_3_shadow_ban.enabled", true)) {
            executeShadowBan(player, check, totalVl);
        } else if (totalVl >= kickThreshold && config.getBoolean("punishments.tier_2_kick.enabled", true)) {
            executeKick(player, check, totalVl);
        } else if (totalVl >= warnThreshold) {
            executeWarn(player, check, totalVl);
        }
    }

    private void executeWarn(Player player, CheckType check, double vl) {
        FileConfiguration config = plugin.getConfig();
        if (config.getBoolean("punishments.tier_1_warn.message_player", false)) {
            String msg = config.getString("punishments.tier_1_warn.player_message", "&c[!] Anomaly detected.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }
    }

    private void executeKick(Player player, CheckType check, double vl) {
        String kickMsg = plugin.getConfig().getString("punishments.tier_2_kick.kick_message",
                "&c[CrystallAC] Disconnected for suspected unfair gameplay (VL: %vl%).")
                .replace("%vl%", String.format("%.1f", vl))
                .replace("%check%", check.getDisplayName());

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMsg));
            }
        });
    }

    private void executeShadowBan(Player player, CheckType check, double vl) {
        if (!shadowBanManager.isShadowBanned(player)) {
            int duration = plugin.getConfig().getInt("punishments.tier_3_shadow_ban.duration_minutes", 10);
            shadowBanManager.shadowBan(player, duration);
        }
    }

    private void executeBan(Player player, CheckType check, double vl) {
        String banCmd = plugin.getConfig().getString("punishments.tier_4_ban.ban_command",
                "ban %player% [CrystallAC] Unfair Advantage detected: %check% (VL: %vl%)")
                .replace("%player%", player.getName())
                .replace("%check%", check.getDisplayName())
                .replace("%vl%", String.format("%.1f", vl));

        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), banCmd);
        });
    }
}
