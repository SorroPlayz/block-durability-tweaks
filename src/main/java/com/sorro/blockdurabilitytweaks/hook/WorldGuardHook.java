package com.sorro.blockdurabilitytweaks.hook;

import com.sorro.blockdurabilitytweaks.config.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class WorldGuardHook {

    private final Plugin plugin;
    private volatile MainConfig cfg;

    private boolean available;
    private Object regionContainer;
    private Method createQuery;
    private Method testBuild;
    private Method getApplicableRegions;
    private Method regionGetId;
    private Method regionGetPriority;

    public WorldGuardHook(Plugin plugin, MainConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        init();
    }

    public void reload(MainConfig cfg) {
        this.cfg = cfg;
        init();
    }

    private void init() {
        available = false;
        regionContainer = null;

        if (!cfg.respectWorldGuard() && !cfg.regionBindingEnabled()) return;
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) return;

        try {
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wg = wgClass.getMethod("getInstance").invoke(null);

            Object platform = wg.getClass().getMethod("getPlatform").invoke(wg);
            regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            Class<?> regionContainerClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionContainer");
            createQuery = regionContainerClass.getMethod("createQuery");

            Class<?> regionQueryClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionQuery");
            testBuild = regionQueryClass.getMethod("testBuild", Class.forName("com.sk89q.worldedit.util.Location"), Object.class);
            getApplicableRegions = regionQueryClass.getMethod("getApplicableRegions", Class.forName("com.sk89q.worldedit.util.Location"));

            Class<?> protectedRegionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion");
            regionGetId = protectedRegionClass.getMethod("getId");
            regionGetPriority = protectedRegionClass.getMethod("getPriority");

            available = true;
        } catch (Throwable t) {
            available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean allowed(Player player, Location loc) {
        return canBuild(player, loc);
    }

    public boolean canBuild(Player player, Location loc) {
        if (!cfg.respectWorldGuard() || !available || loc == null) return true;
        try {
            Object query = createQuery.invoke(regionContainer);

            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object weLoc = bukkitAdapter.getMethod("adapt", Location.class).invoke(null, loc);

            // Passing null as LocalPlayer keeps us dependency-free (best-effort).
            return (boolean) testBuild.invoke(query, weLoc, null);
        } catch (Throwable ignored) {
            return true;
        }
    }

    public String highestPriorityRegionIdAt(Location loc) {
        if (!available || loc == null) return null;
        try {
            Object query = createQuery.invoke(regionContainer);

            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object weLoc = bukkitAdapter.getMethod("adapt", Location.class).invoke(null, loc);

            Object set = getApplicableRegions.invoke(query, weLoc);
            if (set == null) return null;

            Iterable<?> iterable = (Iterable<?>) set;

            Object best = null;
            int bestPrio = Integer.MIN_VALUE;

            for (Object r : iterable) {
                int pr = (int) regionGetPriority.invoke(r);
                if (pr > bestPrio) {
                    bestPrio = pr;
                    best = r;
                }
            }
            if (best == null) return null;
            return (String) regionGetId.invoke(best);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
