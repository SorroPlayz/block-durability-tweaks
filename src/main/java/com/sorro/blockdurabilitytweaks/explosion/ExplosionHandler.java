package com.sorro.blockdurabilitytweaks.explosion;

import com.sorro.blockdurabilitytweaks.config.BDTConfig;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;
import java.util.List;

public class ExplosionHandler implements Listener {

    private final BDTConfig config;

    public ExplosionHandler(BDTConfig config) {
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        adjust(e.blockList(), e.getLocation().getWorld().getName());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        adjust(e.blockList(), e.getBlock().getWorld().getName());
    }

    private void adjust(List<Block> blocks, String worldName) {
        Iterator<Block> it = blocks.iterator();
        while (it.hasNext()) {
            Block b = it.next();
            Material mat = b.getType();
            if (mat.isAir()) continue;

            double mult = config.effectiveBlastMultiplier(worldName, mat);
            if (mult == 1.0) continue;

            // Higher mult => more survive (removed from break list)
            if (mult > 1.0) {
                double surviveChance = 1.0 - (1.0 / mult); // 2 => 0.5, 4 => 0.75
                surviveChance = Math.max(0.0, Math.min(1.0, surviveChance));
                if (Math.random() < surviveChance) it.remove();
            }
            // mult < 1.0 => keep vanilla list (not trying to force "extra break")
        }
    }
}
