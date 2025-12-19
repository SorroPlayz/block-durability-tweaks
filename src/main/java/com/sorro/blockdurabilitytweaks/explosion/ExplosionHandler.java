package com.sorro.blockdurabilitytweaks.explosion;

import com.sorro.blockdurabilitytweaks.BlockDurabilityTweaks;
import com.sorro.blockdurabilitytweaks.config.WorldProfile;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;
import java.util.List;

public class ExplosionHandler implements Listener {

    private final BlockDurabilityTweaks plugin;

    public ExplosionHandler(BlockDurabilityTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (!plugin.mainConfig().explosionsEnabled()) return;
        adjust(e.blockList(), e.getLocation().getWorld().getName());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (!plugin.mainConfig().explosionsEnabled()) return;
        adjust(e.blockList(), e.getBlock().getWorld().getName());
    }

    private void adjust(List<Block> blocks, String worldName) {
        WorldProfile prof = plugin.profiles().getActive(worldName);

        Iterator<Block> it = blocks.iterator();
        while (it.hasNext()) {
            Block b = it.next();
            Material mat = b.getType();
            if (mat.isAir()) continue;

            double mult = prof.blastFor(mat);
            if (mult == 1.0) continue;

            if (mult > 1.0) {
                double surviveChance = 1.0 - (1.0 / mult);
                surviveChance = Math.max(0.0, Math.min(1.0, surviveChance));
                if (Math.random() < surviveChance) it.remove();
            }
        }
    }
}
