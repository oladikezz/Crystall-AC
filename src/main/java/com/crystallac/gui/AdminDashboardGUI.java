package com.crystallac.gui;

import com.crystallac.CrystallAC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Main interactive admin dashboard chest GUI.
 */
public class AdminDashboardGUI {

    public static final String TITLE = "§b§lCrystall AC §8Dashboard";

    public static void open(Player player, CrystallAC plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Fill background with black glass panes
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }

        // Slot 10: Server Metrics
        double[] tps = Bukkit.getTPS();
        double currentTps = (tps != null && tps.length > 0) ? tps[0] : 20.0;
        long maxMem = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long usedMem = maxMem - freeMem;

        inv.setItem(10, createItem(Material.NETHER_STAR, "§a§lServer Status", Arrays.asList(
                "§7TPS: §e" + String.format("%.2f", currentTps),
                "§7Memory: §e" + usedMem + "MB / " + maxMem + "MB",
                "§7Online Players: §a" + Bukkit.getOnlinePlayers().size(),
                "§7Version Adapter: §b" + plugin.getVersionAdapter().getVersionName()
        )));

        // Slot 12: Checks Manager
        inv.setItem(12, createItem(Material.REPEATER, "§e§lChecks Configuration", Arrays.asList(
                "§7Configure active anti-cheat checks,",
                "§7toggle heuristics, and tune thresholds.",
                "",
                "§a▶ Click to manage checks"
        )));

        // Slot 14: Active Players List
        inv.setItem(14, createItem(Material.PLAYER_HEAD, "§6§lPlayer Inspector", Arrays.asList(
                "§7View active players, ping, CPS,",
                "§7and live violation levels.",
                "",
                "§a▶ Click to inspect players"
        )));

        // Slot 16: Statistical & ML Subsystem
        int sampleCount = plugin.getBanDatasetRepository().getTotalSampleCount();
        boolean mlActive = plugin.getModelTuner().isEnabled();
        inv.setItem(16, createItem(Material.BOOK, "§d§lStatistical & ML Engine", Arrays.asList(
                "§7Statistical Layer: §aActive (EWMA/Z-Score)",
                "§7ML Incremental Tuner: " + (mlActive ? "§aEnabled" : "§7Standby (<25 samples)"),
                "§7Stored Ban Samples: §e" + sampleCount + " samples",
                "§7SQLite Storage: §aConnected (data.db)"
        )));

        // Slot 26: Close
        inv.setItem(26, createItem(Material.BARRIER, "§c§lClose", null));

        player.openInventory(inv);
    }

    public static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
