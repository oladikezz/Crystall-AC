package com.crystallac.moderation;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player freezing for screen-shares and audit checks.
 */
public class FreezeManager {

    private final JavaPlugin plugin;
    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();

    public FreezeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startFreezeReminderTask();
    }

    public void freezePlayer(Player staff, Player target) {
        frozenPlayers.add(target.getUniqueId());

        String prefix = plugin.getConfig().getString("settings.prefix", "&b&lCrystallAC &8» &f");
        staff.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&aPlayer &e" + target.getName() + " &ahas been frozen."));

        target.sendTitle(
                ChatColor.RED + "" + ChatColor.BOLD + "YOU ARE FROZEN!",
                ChatColor.YELLOW + "Do not leave the server! Follow staff instructions.",
                10, 100, 20
        );

        sendInstructions(target);
    }

    public void unfreezePlayer(Player staff, Player target) {
        frozenPlayers.remove(target.getUniqueId());

        String prefix = plugin.getConfig().getString("settings.prefix", "&b&lCrystallAC &8» &f");
        if (staff != null) {
            staff.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&aPlayer &e" + target.getName() + " &ahas been unfrozen."));
        }

        target.sendTitle(
                ChatColor.GREEN + "" + ChatColor.BOLD + "UNFROZEN",
                ChatColor.WHITE + "You have been unfrozen. Thank you for your cooperation.",
                10, 60, 20
        );
        target.sendMessage(ChatColor.GREEN + "[CrystallAC] You have been unfrozen.");
    }

    public boolean isFrozen(Player player) {
        if (player == null) return false;
        return frozenPlayers.contains(player.getUniqueId());
    }

    private void sendInstructions(Player player) {
        player.sendMessage(ChatColor.RED + "====================================================");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "           YOU HAVE BEEN FROZEN BY AN ADMIN");
        player.sendMessage(ChatColor.YELLOW + " 1. Do NOT disconnect or you will be automatically banned.");
        player.sendMessage(ChatColor.YELLOW + " 2. Follow staff instructions in chat immediately.");
        player.sendMessage(ChatColor.YELLOW + " 3. Join the Discord screen-share channel if requested.");
        player.sendMessage(ChatColor.RED + "====================================================");
    }

    private void startFreezeReminderTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : frozenPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.sendTitle(
                            ChatColor.RED + "" + ChatColor.BOLD + "YOU ARE FROZEN!",
                            ChatColor.YELLOW + "Check chat for instructions!",
                            0, 40, 10
                    );
                }
            }
        }, 40L, 40L);
    }
}
