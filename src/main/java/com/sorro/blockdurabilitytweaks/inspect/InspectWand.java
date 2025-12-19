package com.sorro.blockdurabilitytweaks.inspect;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class InspectWand {

    private InspectWand() {}

    public static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, "inspect_wand");
    }

    public static ItemStack create(Plugin plugin) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "BD Inspect Wand");
        meta.setLore(List.of(
                ChatColor.GRAY + "Right-click a block to inspect",
                ChatColor.DARK_GRAY + "BlockDurabilityTweaks"
        ));
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isWand(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.STICK) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key(plugin), PersistentDataType.BYTE);
    }
}
