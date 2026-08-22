package com.crystallac.command;

import com.crystallac.CrystallAC;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;
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
 * Command executor for all /ac administrator operations:
 * - /ac check <player>
 * - /ac stats <player>
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
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "check" -> handleCheck(sender, args);
            case "stats" -> handleStats(sender, args);
            case "ban" -> handleBan(sender, args);
            case "reload" -> handleReload(sender);
            case "alerts" -> handleAlerts(sender);
            default -> sendHelp(sender);
        }
        return true;
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
            // 1. Save labeled dataset of features for ML training
            plugin.getBanDatasetRepository().saveLabeledBan(
                    target.getUniqueId(),
                    target.getName(),
                    reason,
                    sender.getName(),
                    snapshots
            );

            // 2. Trigger retraining evaluation if dataset threshold is satisfied
            plugin.getModelTuner().evaluateAndRetrain();
        }

        // Execute ban
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
        sender.sendMessage(ChatColor.YELLOW + "/ac check <player>" + ChatColor.GRAY + " - Inspect real-time checks & VLs");
        sender.sendMessage(ChatColor.YELLOW + "/ac stats <player>" + ChatColor.GRAY + " - View unsupervised statistical baseline");
        sender.sendMessage(ChatColor.YELLOW + "/ac ban <player> <reason>" + ChatColor.GRAY + " - Ban & record labeled training buffer");
        sender.sendMessage(ChatColor.YELLOW + "/ac reload" + ChatColor.GRAY + " - Reload config and recalculate thresholds");
        sender.sendMessage(ChatColor.YELLOW + "/ac alerts" + ChatColor.GRAY + " - Toggle on-screen staff alerts");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("check", "stats", "ban", "reload", "alerts");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("check") || args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("ban"))) {
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                players.add(p.getName());
            }
            return players;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("ban")) {
            return Arrays.asList("Killaura", "Reach", "Speed", "Fly", "Autoclicker", "Aimbot", "Timer");
        }
        return List.of();
    }
}
