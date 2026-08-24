package com.crystallac.gui;

import com.crystallac.CrystallAC;
import com.crystallac.check.CheckType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Interactive GUI for toggling checks on and off in real time.
 */
public class ChecksManagerGUI {

    public static final String TITLE = "§b§lCrystall AC §8Checks Manager";

    public static void open(Player player, CrystallAC plugin) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE);

        ItemStack glass = AdminDashboardGUI.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, glass);
        }

        int slot = 10;
        for (CheckType check : CheckType.values()) {
            boolean enabled = plugin.getConfig().getBoolean("checks." + check.name().toLowerCase() + ".enabled", true);
            Material mat = enabled ? Material.LIME_CONCRETE : Material.RED_CONCRETE;

            inv.setItem(slot, createCheckItem(mat, check, enabled));
            slot++;
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
        }

        // Slot 31: Back to Dashboard
        inv.setItem(31, AdminDashboardGUI.createItem(Material.ARROW, "§e§l◀ Back to Dashboard", null));

        player.openInventory(inv);
    }

    private static ItemStack createCheckItem(Material mat, CheckType check, boolean enabled) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((enabled ? "§a§l" : "§c§l") + check.getDisplayName());
            meta.setLore(Arrays.asList(
                    "§7Category: §e" + check.getCategory().name(),
                    "§7Status: " + (enabled ? "§aEnabled" : "§cDisabled"),
                    "",
                    "§e▶ Click to " + (enabled ? "disable" : "enable")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}
