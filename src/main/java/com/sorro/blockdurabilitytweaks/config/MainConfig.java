package com.sorro.blockdurabilitytweaks.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public record MainConfig(
        boolean explosionsEnabled,
        boolean miningEnabled,
        boolean interceptBreak,
        int animationTickRate,
        int animationRadius,
        boolean clearCracksOnAbort,
        boolean respectWorldGuard,
        Map<String, String> activeProfilePerWorld
) {
    public static MainConfig from(FileConfiguration cfg) {
        boolean explosionsEnabled = cfg.getBoolean("features.explosions.enabled", true);

        boolean miningEnabled = cfg.getBoolean("features.mining.enabled", true);
        boolean intercept = cfg.getBoolean("features.mining.intercept_break", true);
        int tickRate = Math.max(1, cfg.getInt("features.mining.animation_tick_rate", 2));
        int radius = Math.max(1, cfg.getInt("features.mining.animation_radius", 64));
        boolean clear = cfg.getBoolean("features.mining.clear_cracks_on_abort", true);

        boolean respectWG = cfg.getBoolean("worldguard.respect_worldguard", true);

        Map<String, String> active = new HashMap<>();
        ConfigurationSection sec = cfg.getConfigurationSection("profiles.active_per_world");
        if (sec != null) {
            for (String world : sec.getKeys(false)) {
                active.put(world, sec.getString(world, "vanilla"));
            }
        }
        return new MainConfig(explosionsEnabled, miningEnabled, intercept, tickRate, radius, clear, respectWG, active);
    }

    public String activeProfileForWorld(String world) {
        return activeProfilePerWorld.getOrDefault(world, "vanilla");
    }

    public int animationTickRate() { return animationTickRate; }
    public int animationRadius() { return animationRadius; }
}
