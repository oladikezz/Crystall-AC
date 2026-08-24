package com.crystallac.gui;

import com.crystallac.CrystallAC;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Interactive inspection GUI for auditing and moderating an individual player.
 */
public class PlayerInspectorGUI {

    public static final String TITLE_PREFIX = "§8Inspect: §b";

    public static void open(Player staff, Player target, CrystallAC plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PREFIX + target.getName());

        ItemStack glass = AdminDashboardGUI.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }

        PlayerData data = plugin.getDataManager().getPlayerData(target);
        int ping = plugin.getVersionAdapter().getPing(target);
        double cps = (data != null) ? data.getCps() : 0.0;
        double totalVl = (data != null) ? data.getTotalVL() : 0.0;
        boolean isFrozen = plugin.getFreezeManager().isFrozen(target);

        // Slot 4: Player Skull
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            meta.setDisplayName("§e§l" + target.getName());
            List<String> lore = new ArrayList<>();
            lore.add("§7Ping: §a" + ping + "ms");
            lore.add("§7Current CPS: §e" + String.format("%.1f", cps));
            lore.add("§7Total VL: §c" + String.format("%.1f", totalVl));
            lore.add("§7Status: " + (isFrozen ? "§c[FROZEN]" : "§a[Active]"));
            lore.add("");
            lore.add("§8Violations breakdown:");
            if (data != null) {
                for (CheckType c : CheckType.values()) {
                    double vl = data.getVL(c);
                    if (vl > 0.0) {
                        lore.add(" §7- §f" + c.getDisplayName() + ": §c" + String.format("%.1f VL", vl));
                    }
                }
            }
            meta.setLore(lore);
            skull.setItemMeta(meta);
        }
        inv.setItem(4, skull);

        // Slot 10: Spectate
        inv.setItem(10, AdminDashboardGUI.createItem(Material.ENDER_EYE, "§b§lSpectate Player", Arrays.asList(
                "§7Enter silent spectate mode",
                "§7with real-time Action Bar HUD.",
                "",
                "§a▶ Click to Spectate"
        )));

        // Slot 12: Freeze / Unfreeze
        Material freezeMat = isFrozen ? Material.PACKED_ICE : Material.ICE;
        inv.setItem(12, AdminDashboardGUI.createItem(freezeMat, (isFrozen ? "§c§lUnfreeze Player" : "§b§lFreeze Player"), Arrays.asList(
                "§7Lock player in place for",
                "§7screen-share and audit.",
                "",
                "§a▶ Click to toggle Freeze"
        )));

        // Slot 14: Reset VL
        inv.setItem(14, AdminDashboardGUI.createItem(Material.WATER_BUCKET, "§e§lReset Violations (VL)", Arrays.asList(
                "§7Clear all accumulated VL points",
                "§7for this player session.",
                "",
                "§a▶ Click to Reset VL"
        )));

        // Slot 16: Quick Kick
        inv.setItem(16, AdminDashboardGUI.createItem(Material.IRON_SWORD, "§6§lQuick Kick", Arrays.asList(
                "§7Disconnect player immediately.",
                "",
                "§c▶ Click to Kick"
        )));

        // Slot 22: Quick Ban
        inv.setItem(22, AdminDashboardGUI.createItem(Material.DIAMOND_AXE, "§4§lQuick Ban & Archive", Arrays.asList(
                "§7Ban player and save feature",
                "§7snapshot to SQLite ML dataset.",
                "",
                "§4▶ Click to Ban"
        )));

        staff.openInventory(inv);
    }
}
