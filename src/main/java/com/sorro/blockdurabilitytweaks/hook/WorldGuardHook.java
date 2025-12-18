package com.sorro.blockdurabilitytweaks.hook;

import com.sorro.blockdurabilitytweaks.config.BDTConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Lightweight WorldGuard hook via reflection.
 * - If WorldGuard is present and respect_worldguard=true:
 *     - We only apply mining/breaking if WorldGuardPlugin#canBuild(player, location) returns true
 *       OR player has permission blockdurabilitytweaks.worldguard.bypass
 * - If WorldGuard isn't present (or method not found), we assume allowed.
 */
public final class WorldGuardHook {

    private final JavaPlugin plugin;
    private volatile BDTConfig cfg;

    private final Plugin wgPlugin;
    private final Method canBuildMethod;

    public WorldGuardHook(JavaPlugin plugin, BDTConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;

        Plugin p = Bukkit.getPluginManager().getPlugin("WorldGuard");
        this.wgPlugin = p;

        Method m = null;
        if (p != null) {
            try {
                // com.sk89q.worldguard.bukkit.WorldGuardPlugin has canBuild(Player, Location)
                m = p.getClass().getMethod("canBuild", Player.class, Location.class);
            } catch (Throwable t) {
                plugin.getLogger().warning("WorldGuard detected, but couldn't reflect canBuild(Player, Location). Falling back to 'allowed'.");
            }
        }
        this.canBuildMethod = m;
    }

    public void reload(BDTConfig cfg) {
        this.cfg = cfg;
    }

    public boolean isInstalled() {
        return wgPlugin != null;
    }

    public boolean allowed(Player player, Location loc) {
        if (!cfg.respectWorldGuard()) return true;
        if (wgPlugin == null || canBuildMethod == null) return true;

        if (player.hasPermission("blockdurabilitytweaks.worldguard.bypass")) return true;

        try {
            Object res = canBuildMethod.invoke(wgPlugin, player, loc);
            if (res instanceof Boolean b) return b;
        } catch (Throwable t) {
            // If WG errors, default allow to avoid hard-locking mining
            plugin.getLogger().warning("WorldGuard hook error: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return true;
        }
        return true;
    }
}
