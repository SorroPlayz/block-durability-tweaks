package com.sorro.blockdurabilitytweaks.util;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.lang.reflect.Method;

public final class BreakUtil {
    private BreakUtil() {}

    private static final Method paperBreakBlock;

    static {
        Method m;
        try {
            m = Player.class.getMethod("breakBlock", Block.class);
        } catch (Throwable ignored) {
            m = null;
        }
        paperBreakBlock = m;
    }

    public static boolean breakAsPlayer(Player player, Block block) {
        BlockBreakEvent ev = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) return false;

        try {
            if (paperBreakBlock != null) {
                return (boolean) paperBreakBlock.invoke(player, block);
            }
        } catch (Throwable ignored) {}

        var tool = player.getInventory().getItemInMainHand();
        return block.breakNaturally(tool);
    }
}
