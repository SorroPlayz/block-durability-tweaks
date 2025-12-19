package com.sorro.blockdurabilitytweaks.util;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class BreakUtil {
    private BreakUtil() {}

    public static void breakLikePlayer(Player player, Block block) {
        if (player == null || block == null) return;

        if (player.getGameMode() == GameMode.CREATIVE) {
            block.setType(org.bukkit.Material.AIR, false);
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        // Spigot API only has breakNaturally() and breakNaturally(ItemStack)
        block.breakNaturally(tool);
    }
}
