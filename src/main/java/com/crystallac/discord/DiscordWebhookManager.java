package com.crystallac.discord;

import com.crystallac.check.CheckType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends rich Discord Embed webhooks asynchronously on cheat detections, shadow bans, and bans.
 */
public class DiscordWebhookManager {

    private final JavaPlugin plugin;
    private final HttpClient httpClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public DiscordWebhookManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void sendViolationAlert(Player player, CheckType check, double vl, String details, int ping) {
        if (!isEnabled()) return;

        double minVl = plugin.getConfig().getDouble("discord_webhook.min_vl_to_send", 15.0);
        if (vl < minVl) return;

        String webhookUrl = plugin.getConfig().getString("discord_webhook.webhook_url", "");
        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.contains("YOUR_WEBHOOK_URL")) {
            return;
        }

        String uuid = player.getUniqueId().toString();
        String name = player.getName();
        String loc = String.format("X: %.1f, Y: %.1f, Z: %.1f (%s)",
                player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(),
                player.getWorld().getName());

        double tps = getRecentTps();

        String jsonPayload = String.format("""
        {
          "embeds": [
            {
              "title": "🚨 Anti-Cheat Flag: %s",
              "color": 15158332,
              "thumbnail": {
                "url": "https://crafatar.com/avatars/%s?size=128&overlay"
              },
              "fields": [
                {"name": "Player", "value": "`%s`", "inline": true},
                {"name": "Check", "value": "`%s`", "inline": true},
                {"name": "Violation Level", "value": "`%.1f VL`", "inline": true},
                {"name": "Ping", "value": "`%d ms`", "inline": true},
                {"name": "Server TPS", "value": "`%.2f`", "inline": true},
                {"name": "Location", "value": "`%s`", "inline": false},
                {"name": "Debug Details", "value": "```%s```", "inline": false}
              ],
              "footer": {
                "text": "Crystall AC • %s"
              }
            }
          ]
        }
        """, name, uuid, name, check.getDisplayName(), vl, ping, tps, loc, escapeJson(details), dateFormat.format(new Date()));

        executePost(webhookUrl, jsonPayload);
    }

    public void sendBanAlert(Player player, String reason, String bannedBy) {
        if (!isEnabled()) return;
        String webhookUrl = plugin.getConfig().getString("discord_webhook.webhook_url", "");
        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.contains("YOUR_WEBHOOK_URL")) return;

        String uuid = player.getUniqueId().toString();
        String name = player.getName();

        String jsonPayload = String.format("""
        {
          "embeds": [
            {
              "title": "🔨 Player Banned: %s",
              "color": 10038562,
              "thumbnail": {
                "url": "https://crafatar.com/avatars/%s?size=128&overlay"
              },
              "fields": [
                {"name": "Player", "value": "`%s`", "inline": true},
                {"name": "Banned By", "value": "`%s`", "inline": true},
                {"name": "Reason", "value": "`%s`", "inline": false}
              ],
              "footer": {
                "text": "Crystall AC Ban Audit • %s"
              }
            }
          ]
        }
        """, name, uuid, name, bannedBy, escapeJson(reason), dateFormat.format(new Date()));

        executePost(webhookUrl, jsonPayload);
    }

    private void executePost(String webhookUrl, String json) {
        executor.submit(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                plugin.getLogger().warning("[CrystallAC] Failed to dispatch Discord Webhook: " + e.getMessage());
            }
        });
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("discord_webhook.enabled", false);
    }

    private double getRecentTps() {
        try {
            double[] tps = Bukkit.getTPS();
            if (tps != null && tps.length > 0) return tps[0];
        } catch (Throwable ignored) {}
        return 20.0;
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    public void shutdown() {
        executor.shutdown();
    }
}
