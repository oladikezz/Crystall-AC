package com.crystallac.command;

import com.crystallac.CrystallAC;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;
import com.crystallac.gui.AdminDashboardGUI;
import com.crystallac.gui.PlayerInspectorGUI;
import com.crystallac.ml.FeatureSnapshot;
import com.crystallac.statistical.PlayerBaselineTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command executor for all /ac administrator and moderation operations:
 * - /ac gui [player]
 * - /ac check <player>
 * - /ac stats <player>
 * - /ac freeze <player>
 * - /ac unfreeze <player>
 * - /ac spectate <player|stop>
 * - /ac ban <player> <reason>
 * - /ac reload
 * - /ac alerts
 */
public class AntiCheatCommand implements CommandExecutor, TabCompleter {

    private final CrystallAC plugin;

    public AntiCheatCommand(CrystallAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crystallac.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                AdminDashboardGUI.open(player, plugin);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "gui", "menu" -> handleGui(sender, args);
            case "check" -> handleCheck(sender, args);
            case "stats" -> handleStats(sender, args);
            case "freeze" -> handleFreeze(sender, args);
            case "unfreeze" -> handleUnfreeze(sender, args);
            case "spectate", "spec" -> handleSpectate(sender, args);
            case "ban" -> handleBan(sender, args);
            case "reload" -> handleReload(sender);
            case "alerts" -> handleAlerts(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleGui(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by in-game players.");
            return;
        }

        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null && target.isOnline()) {
                PlayerInspectorGUI.open(player, target, plugin);
            } else {
                sender.sendMessage(ChatColor.RED + "Player not found or offline.");
            }
        } else {
            AdminDashboardGUI.open(player, plugin);
        }
    }

    private void handleFreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ac freeze <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player not found or offline.");
            return;
        }

        Player staff = (sender instanceof Player p) ? p : null;
        plugin.getFreezeManager().freezePlayer(staff != null ? staff : target, target);
        if (staff == null) {
            sender.sendMessage(ChatColor.GREEN + "Frozen player " + target.getName());
        }
    }

    private void handleUnfreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ac unfreeze <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player not found or offline.");
            return;
        }

        Player staff = (sender instanceof Player p) ? p : null;
        plugin.getFreezeManager().unfreezePlayer(staff, target);
        if (staff == null) {
            sender.sendMessage(ChatColor.GREEN + "Unfrozen player " + target.getName());
        }
    }

    private void handleSpectate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can spectate.");
            return;
        }

        if (args.length < 2) {
            if (plugin.getSpectateManager().isSpectating(player)) {
                plugin.getSpectateManager().stopSpectating(player);
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /ac spectate <player|stop>");
            }
            return;
        }

        if (args[1].equalsIgnoreCase("stop")) {
            plugin.getSpectateManager().stopSpectating(player);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Target player not found or offline.");
            return;
        }

        plugin.getSpectateManager().startSpectating(player, target);
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ac check <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or offline.");
            return;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(target);
        if (data == null) {
            sender.sendMessage(ChatColor.RED + "No data profile available for this player.");
            return;
        }

        int ping = plugin.getVersionAdapter().getPing(target);
        sender.sendMessage(ChatColor.DARK_GRAY + "---------------- " + ChatColor.AQUA + ChatColor.BOLD + "CrystallAC Check: " + ChatColor.WHITE + target.getName() + ChatColor.DARK_GRAY + " ----------------");
        sender.sendMessage(ChatColor.GRAY + "Ping: " + ChatColor.GREEN + ping + "ms" + ChatColor.GRAY + " | Packet Balance: " + ChatColor.YELLOW + String.format("%.1f ms", data.getPacketBalanceMs()));
        sender.sendMessage(ChatColor.GRAY + "Current CPS: " + ChatColor.AQUA + String.format("%.1f", data.getCps()) + ChatColor.GRAY + " | Total VL: " + ChatColor.RED + String.format("%.1f", data.getTotalVL()));
        sender.sendMessage(ChatColor.DARK_GRAY + "Violations Breakdown:");

        for (CheckType check : CheckType.values()) {
            double vl = data.getVL(check);
            if (vl > 0.0) {
                sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + check.getDisplayName() + ": " + ChatColor.RED + String.format("%.1f VL", vl));
            }
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "----------------------------------------------------");
    }

    private void handleStats(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ac stats <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or offline.");
            return;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(target);
        if (data == null) {
            sender.sendMessage(ChatColor.RED + "No data profile available for this player.");
            return;
        }

        PlayerBaselineTracker tracker = data.getBaselineTracker();
        sender.sendMessage(ChatColor.DARK_GRAY + "------------- " + ChatColor.GOLD + ChatColor.BOLD + "Statistical Baseline: " + ChatColor.WHITE + target.getName() + ChatColor.DARK_GRAY + " -------------");
        sender.sendMessage(ChatColor.GRAY + "EWMA CPS: " + ChatColor.YELLOW + String.format("%.2f", tracker.getEwmaCps()) + ChatColor.GRAY + " (StdDev: " + String.format("%.2f", tracker.getCpsStdDev()) + " ms)");
        sender.sendMessage(ChatColor.GRAY + "EWMA Rotation Delta: " + ChatColor.YELLOW + String.format("%.2f°/tick", tracker.getEwmaRotationDelta()));
        sender.sendMessage(ChatColor.GRAY + "EWMA Reaction Latency: " + ChatColor.YELLOW + String.format("%.1f ms", tracker.getEwmaReactionTimeMs()));
        sender.sendMessage(ChatColor.GRAY + "EWMA Movement Speed: " + ChatColor.YELLOW + String.format("%.3f b/t", tracker.getEwmaMovementSpeed()));
        sender.sendMessage(ChatColor.GRAY + "Buffer Snapshots: " + ChatColor.AQUA + data.getFeatureBuffer().size() + " samples");
        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------------");
    }

    private void handleBan(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /ac ban <player> <cheat_type/reason>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or offline.");
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        PlayerData data = plugin.getDataManager().getPlayerData(target);

        if (data != null) {
            List<FeatureSnapshot> snapshots = data.getFeatureBuffer().getSnapshots();
            plugin.getBanDatasetRepository().saveLabeledBan(
                    target.getUniqueId(),
                    target.getName(),
                    reason,
                    sender.getName(),
                    snapshots
            );

            plugin.getModelTuner().evaluateAndRetrain();
        }

        // Discord webhook alert
        if (plugin.getDiscordWebhookManager() != null) {
            plugin.getDiscordWebhookManager().sendBanAlert(target, reason, sender.getName());
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + target.getName() + " [CrystallAC] Confirmed " + reason);
        sender.sendMessage(ChatColor.GREEN + "Player " + target.getName() + " banned and labeled feature dataset saved to SQLite.");
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getModelTuner().evaluateAndRetrain();
        sender.sendMessage(ChatColor.GREEN + "[CrystallAC] Configuration and check parameters reloaded successfully.");
    }

    private void handleAlerts(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only in-game players can toggle alerts.");
            return;
        }
        plugin.getViolationManager().toggleAlerts(player);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "=== CrystallAC Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/ac gui [player]" + ChatColor.GRAY + " - Open interactive Admin Dashboard / Inspector");
        sender.sendMessage(ChatColor.YELLOW + "/ac check <player>" + ChatColor.GRAY + " - Inspect real-time checks & VLs");
        sender.sendMessage(ChatColor.YELLOW + "/ac stats <player>" + ChatColor.GRAY + " - View unsupervised statistical baseline");
        sender.sendMessage(ChatColor.YELLOW + "/ac freeze <player>" + ChatColor.GRAY + " - Freeze player for screen-share / audit");
        sender.sendMessage(ChatColor.YELLOW + "/ac unfreeze <player>" + ChatColor.GRAY + " - Unfreeze player");
        sender.sendMessage(ChatColor.YELLOW + "/ac spectate <player|stop>" + ChatColor.GRAY + " - Spectate player with live Action Bar HUD");
        sender.sendMessage(ChatColor.YELLOW + "/ac ban <player> <reason>" + ChatColor.GRAY + " - Ban & record labeled training buffer");
        sender.sendMessage(ChatColor.YELLOW + "/ac reload" + ChatColor.GRAY + " - Reload config and recalculate thresholds");
        sender.sendMessage(ChatColor.YELLOW + "/ac alerts" + ChatColor.GRAY + " - Toggle on-screen staff alerts");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("gui", "check", "stats", "freeze", "unfreeze", "spectate", "ban", "reload", "alerts");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("check") || args[0].equalsIgnoreCase("stats") ||
                                 args[0].equalsIgnoreCase("ban") || args[0].equalsIgnoreCase("freeze") ||
                                 args[0].equalsIgnoreCase("unfreeze") || args[0].equalsIgnoreCase("spectate") ||
                                 args[0].equalsIgnoreCase("gui"))) {
            List<String> players = new ArrayList<>();
            if (args[0].equalsIgnoreCase("spectate")) {
                players.add("stop");
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                players.add(p.getName());
            }
            return players;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("ban")) {
            return Arrays.asList("Killaura", "Reach", "Speed", "Fly", "Autoclicker", "Aimbot", "Timer", "Scaffold", "FastPlace", "FastBreak");
        }
        return List.of();
    }
}
