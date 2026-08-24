package com.crystallac.gui;

import com.crystallac.CrystallAC;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles all click events and button logic for CrystallAC GUI menus.
 */
public class GUIListener implements Listener {

    private final CrystallAC plugin;

    public GUIListener(CrystallAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (title.equals(AdminDashboardGUI.TITLE)) {
            event.setCancelled(true);
            handleDashboardClick(player, event.getSlot(), event.getCurrentItem());
        } else if (title.equals(ChecksManagerGUI.TITLE)) {
            event.setCancelled(true);
            handleChecksManagerClick(player, event.getSlot(), event.getCurrentItem());
        } else if (title.startsWith(PlayerInspectorGUI.TITLE_PREFIX)) {
            event.setCancelled(true);
            String targetName = title.replace(PlayerInspectorGUI.TITLE_PREFIX, "").trim();
            handleInspectorClick(player, targetName, event.getSlot(), event.getCurrentItem());
        }
    }

    private void handleDashboardClick(Player player, int slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        if (slot == 12) { // Checks Manager
            ChecksManagerGUI.open(player, plugin);
        } else if (slot == 14) { // Player Inspector (open other online player or self)
            Player target = player;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getUniqueId().equals(player.getUniqueId())) {
                    target = p;
                    break;
                }
            }
            PlayerInspectorGUI.open(player, target, plugin);
        } else if (slot == 26) { // Close
            player.closeInventory();
        }
    }

    private void handleChecksManagerClick(Player player, int slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        if (slot == 31) { // Back
            AdminDashboardGUI.open(player, plugin);
            return;
        }

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            for (CheckType check : CheckType.values()) {
                if (check.getDisplayName().equalsIgnoreCase(name)) {
                    String configPath = "checks." + check.name().toLowerCase() + ".enabled";
                    boolean currentState = plugin.getConfig().getBoolean(configPath, true);
                    plugin.getConfig().set(configPath, !currentState);
                    plugin.saveConfig();

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("settings.prefix", "&b&lCrystallAC &8» &f") +
                            "&7Check &e" + check.getDisplayName() + " &7is now " + (!currentState ? "&aEnabled" : "&cDisabled")));

                    ChecksManagerGUI.open(player, plugin);
                    return;
                }
            }
        }
    }

    private void handleInspectorClick(Player staff, String targetName, int slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            staff.sendMessage(ChatColor.RED + "[CrystallAC] Player is no longer online.");
            staff.closeInventory();
            return;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(target);

        switch (slot) {
            case 10 -> { // Spectate
                staff.closeInventory();
                plugin.getSpectateManager().startSpectating(staff, target);
            }
            case 12 -> { // Freeze / Unfreeze
                if (plugin.getFreezeManager().isFrozen(target)) {
                    plugin.getFreezeManager().unfreezePlayer(staff, target);
                } else {
                    plugin.getFreezeManager().freezePlayer(staff, target);
                }
                PlayerInspectorGUI.open(staff, target, plugin);
            }
            case 14 -> { // Reset VL
                if (data != null) {
                    data.resetAllVL();
                    staff.sendMessage(ChatColor.GREEN + "[CrystallAC] Reset VL for " + target.getName());
                    PlayerInspectorGUI.open(staff, target, plugin);
                }
            }
            case 16 -> { // Quick Kick
                staff.closeInventory();
                target.kickPlayer(ChatColor.RED + "[CrystallAC] Kicked by administrator.");
                staff.sendMessage(ChatColor.YELLOW + "[CrystallAC] Kicked " + target.getName());
            }
            case 22 -> { // Quick Ban
                staff.closeInventory();
                Bukkit.dispatchCommand(staff, "ac ban " + target.getName() + " ManualAudit");
            }
        }
    }
}
