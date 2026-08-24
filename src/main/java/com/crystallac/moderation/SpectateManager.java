package com.crystallac.moderation;

import com.crystallac.CrystallAC;
import com.crystallac.data.PlayerData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages staff spectating with real-time HUD (Action Bar) overlay:
 * Displays target CPS, Ping, Delta angles, Reach, and VL in real-time.
 */
public class SpectateManager {

    private final CrystallAC plugin;
    private final Map<UUID, UUID> activeSpectators = new ConcurrentHashMap<>();
    private BukkitTask hudTask;

    public SpectateManager(CrystallAC plugin) {
        this.plugin = plugin;
        startHudTask();
    }

    public void startSpectating(Player staff, Player target) {
        activeSpectators.put(staff.getUniqueId(), target.getUniqueId());
        staff.setGameMode(GameMode.SPECTATOR);
        staff.teleport(target.getLocation());

        String prefix = plugin.getConfig().getString("settings.prefix", "&b&lCrystallAC &8» &f");
        staff.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&aNow spectating &e" + target.getName() + "&a. HUD activated."));
    }

    public void stopSpectating(Player staff) {
        activeSpectators.remove(staff.getUniqueId());
        staff.setGameMode(GameMode.SURVIVAL);

        String prefix = plugin.getConfig().getString("settings.prefix", "&b&lCrystallAC &8» &f");
        staff.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cSpectate mode stopped."));
    }

    public boolean isSpectating(Player staff) {
        return activeSpectators.containsKey(staff.getUniqueId());
    }

    private void startHudTask() {
        this.hudTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            activeSpectators.forEach((staffUuid, targetUuid) -> {
                Player staff = Bukkit.getPlayer(staffUuid);
                Player target = Bukkit.getPlayer(targetUuid);

                if (staff != null && staff.isOnline() && target != null && target.isOnline()) {
                    PlayerData data = plugin.getDataManager().getPlayerData(target);
                    if (data != null) {
                        int ping = plugin.getVersionAdapter().getPing(target);
                        double cps = data.getCps();
                        double vl = data.getTotalVL();
                        float dYaw = data.getDeltaYaw();
                        float dPitch = data.getDeltaPitch();

                        String hudText = String.format(
                                "§b§l[CrystallAC HUD] §fTarget: §e%s §7| §fCPS: §a%.1f §7| §fPing: §a%dms §7| §fRotΔ: §6%.1f° §7| §fVL: §c%.1f",
                                target.getName(), cps, ping, Math.hypot(dYaw, dPitch), vl
                        );

                        staff.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(hudText));
                    }
                } else if (staff != null && staff.isOnline() && (target == null || !target.isOnline())) {
                    staff.sendMessage(ChatColor.RED + "[CrystallAC] Spectate target went offline.");
                    stopSpectating(staff);
                }
            });
        }, 5L, 5L);
    }

    public void shutdown() {
        if (hudTask != null) {
            hudTask.cancel();
        }
    }
}
