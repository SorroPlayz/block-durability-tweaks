package com.sorro.blockdurabilitytweaks.explosion;

import com.sorro.blockdurabilitytweaks.config.MainConfig;
import com.sorro.blockdurabilitytweaks.config.ProfileManager;
import com.sorro.blockdurabilitytweaks.config.WorldProfile;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;

import java.util.Iterator;
import java.util.Random;

public class ExplosionHandler implements Listener {

    private final Plugin plugin;
    private volatile MainConfig cfg;
    private volatile ProfileManager profiles;
    private final Random rng = new Random();

    public ExplosionHandler(Plugin plugin, MainConfig cfg, ProfileManager profiles) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.profiles = profiles;
    }

    public void reload(MainConfig cfg, ProfileManager profiles) {
        this.cfg = cfg;
        this.profiles = profiles;
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        if (!cfg.explosionsEnabled()) return;

        // Best-effort: higher blast multiplier => fewer blocks break.
        Iterator<Block> it = e.blockList().iterator();
        while (it.hasNext()) {
            Block b = it.next();
            WorldProfile p = profiles.getActive(b.getWorld().getName());

            double mult = Math.max(0.0, p.blastFor(b.getType()));
            if (mult <= 1.0) continue;

            // keep block with probability (1 - 1/mult)
            double breakChance = 1.0 / mult;
            if (rng.nextDouble() > breakChance) {
                it.remove();
            }
        }
    }
}
