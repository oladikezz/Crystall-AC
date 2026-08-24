package com.crystallac.punishment;

import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;
import com.crystallac.data.PlayerDataManager;
import com.crystallac.discord.DiscordWebhookManager;
import com.crystallac.statistical.AnomalyDetector;
import com.crystallac.storage.AuditLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates violation level (VL) accrual, statistical scaling, staff alert broadcasting,
 * Discord webhooks dispatch, decay processing, and punishment execution.
 */
public class ViolationManager {

    private final JavaPlugin plugin;
    private final PlayerDataManager dataManager;
    private final PunishmentManager punishmentManager;
    private final AuditLogger auditLogger;
    private final AnomalyDetector anomalyDetector;
    private final DiscordWebhookManager discordWebhookManager;
    private final Set<UUID> alertSubscribers = ConcurrentHashMap.newKeySet();

    public ViolationManager(JavaPlugin plugin,
                            PlayerDataManager dataManager,
                            PunishmentManager punishmentManager,
                            AuditLogger auditLogger,
                            AnomalyDetector anomalyDetector,
                            DiscordWebhookManager discordWebhookManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.punishmentManager = punishmentManager;
        this.auditLogger = auditLogger;
        this.anomalyDetector = anomalyDetector;
        this.discordWebhookManager = discordWebhookManager;

        startDecayTask();
    }

    public void handleViolation(PlayerData data, CheckType check, double baseVl, String details) {
        Player player = data.getPlayer();
        if (player == null || !player.isOnline()) return;

        double finalVl = baseVl;
        String anomalyTag = "";

        if (plugin.getConfig().getBoolean("statistical_layer.enabled", true)) {
            AnomalyDetector.AnomalyResult anomaly = anomalyDetector.evaluateCombatAnomaly(
                    data.getBaselineTracker(),
                    data.getCps(),
                    data.getClickIntervalStdDev(),
                    data.getDeltaYaw()
            );

            if (anomaly.isAnomalous()) {
                finalVl *= anomaly.getVlMultiplier();
                anomalyTag = String.format(" &e[Anomaly Z: %.1f]", anomaly.getMaxZScore());
            }
        }

        data.addVL(check, finalVl);
        double totalVl = data.getVL(check);

        // Audit Logging
        auditLogger.logViolation(player, check, totalVl, details + (anomalyTag.isEmpty() ? "" : " | " + anomalyTag));

        // Staff Alerts
        broadcastAlert(player, check, totalVl, details + anomalyTag);

        // Discord Webhook
        if (discordWebhookManager != null) {
            discordWebhookManager.sendViolationAlert(player, check, totalVl, details, player.getPing());
        }

        // Evaluate Punishments
        punishmentManager.evaluatePunishment(player, check, totalVl);
    }

    private void broadcastAlert(Player player, CheckType check, double vl, String details) {
        String prefix = plugin.getConfig().getString("settings.prefix", "&b&lCrystallAC &8» &f");
        String message = ChatColor.translateAlternateColorCodes('&', String.format(
                "%s&e%s &7flagged &c%s &7(VL: &f%.1f&7) &8[&7%s&8]",
                prefix, player.getName(), check.getDisplayName(), vl, details
        ));

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("crystallac.alerts") && isAlertsEnabled(staff)) {
                staff.sendMessage(message);
            }
        }
    }

    public boolean isAlertsEnabled(Player player) {
        return !alertSubscribers.contains(player.getUniqueId());
    }

    public void toggleAlerts(Player player) {
        UUID uuid = player.getUniqueId();
        if (alertSubscribers.contains(uuid)) {
            alertSubscribers.remove(uuid);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("settings.prefix", "&b&lCrystallAC &8» &f") + "&aCheat alerts enabled."));
        } else {
            alertSubscribers.add(uuid);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("settings.prefix", "&b&lCrystallAC &8» &f") + "&cCheat alerts disabled."));
        }
    }

    private void startDecayTask() {
        long decaySeconds = plugin.getConfig().getLong("punishments.decay_interval_seconds", 2L);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (PlayerData data : dataManager.getAllPlayerData()) {
                for (CheckType check : CheckType.values()) {
                    double decay = plugin.getConfig().getDouble("checks." + check.name().toLowerCase() + ".decay_amount", 1.0);
                    data.decayVL(check, decay);
                }
            }
        }, decaySeconds * 20L, decaySeconds * 20L);
    }
}
