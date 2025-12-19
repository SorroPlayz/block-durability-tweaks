package com.sorro.blockdurabilitytweaks.gui;

import com.sorro.blockdurabilitytweaks.BlockDurabilityTweaks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ProfileGui implements Listener {

    private final BlockDurabilityTweaks plugin;
    private final Map<UUID, String> openWorld = new HashMap<>();

    public ProfileGui(BlockDurabilityTweaks plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {
        if (!plugin.mainConfig().guiEnabled()) {
            p.sendMessage(ChatColor.RED + "GUI is disabled in config.");
            return;
        }
        if (plugin.mainConfig().guiRequirePerm() && !p.hasPermission(plugin.mainConfig().guiPerm())) {
            p.sendMessage(ChatColor.RED + "No permission.");
            return;
        }

        String world = p.getWorld().getName();
        Set<String> profiles = plugin.profiles().listProfiles(world);

        int size = plugin.mainConfig().guiRows() * 9;
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.translateAlternateColorCodes('&', plugin.mainConfig().guiTitle()));

        List<String> list = new ArrayList<>(profiles);
        list.sort(String.CASE_INSENSITIVE_ORDER);

        int i = 0;
        for (String prof : list) {
            if (i >= size) break;
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + prof);
            meta.setLore(List.of(ChatColor.GRAY + "Click to load for world:", ChatColor.WHITE + world));
            item.setItemMeta(meta);
            inv.setItem(i++, item);
        }

        openWorld.put(p.getUniqueId(), world);
        p.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        UUID id = p.getUniqueId();
        if (!openWorld.containsKey(id)) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (!clicked.hasItemMeta() || clicked.getItemMeta().getDisplayName() == null) return;

        String prof = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        String world = openWorld.get(id);

        boolean ok = plugin.profiles().loadProfile(world, prof);
        p.sendMessage(ok ? ChatColor.GREEN + "Loaded profile '" + prof + "' for " + world : ChatColor.RED + "Failed to load profile.");
        p.closeInventory();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        openWorld.remove(e.getPlayer().getUniqueId());
    }
}
