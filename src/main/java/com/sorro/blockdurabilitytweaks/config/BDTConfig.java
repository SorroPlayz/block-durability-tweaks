package com.sorro.blockdurabilitytweaks.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public record BDTConfig(
        double blastMultiplier,
        double hardnessMultiplier,
        boolean explosionsEnabled,
        boolean miningEnabled,
        boolean interceptBreak,
        int animationTickRate,
        int animationRadius,
        boolean clearCracksOnAbort,
        boolean respectWorldGuard,

        Map<Material, Double> blastOverrides,
        Map<Material, Double> hardnessOverrides,

        Map<String, WorldOverrides> worldOverrides
) {

    public record WorldOverrides(
            double blastMultiplier,
            double hardnessMultiplier,
            Map<Material, Double> blastOverrides,
            Map<Material, Double> hardnessOverrides
    ) {}

    public static BDTConfig from(org.bukkit.configuration.file.FileConfiguration cfg, Logger log) {
        double blast = cfg.getDouble("multipliers.blast_resistance", 1.0);
        double hard = cfg.getDouble("multipliers.hardness", 1.0);

        boolean explosionsEnabled = cfg.getBoolean("explosions.enabled", true);

        boolean miningEnabled = cfg.getBoolean("mining.enabled", false);
        boolean interceptBreak = cfg.getBoolean("mining.intercept_break", true);
        int tickRate = Math.max(1, cfg.getInt("mining.animation_tick_rate", 2));
        int radius = Math.max(1, cfg.getInt("mining.animation_radius", 64));
        boolean clearOnAbort = cfg.getBoolean("mining.clear_cracks_on_abort", true);

        boolean respectWG = cfg.getBoolean("worldguard.respect_worldguard", true);

        Map<Material, Double> blastOverrides = new EnumMap<>(Material.class);
        Map<Material, Double> hardnessOverrides = new EnumMap<>(Material.class);

        readOverrides(cfg.getConfigurationSection("overrides"), blastOverrides, hardnessOverrides, log);

        // Per-world
        Map<String, WorldOverrides> worlds = new HashMap<>();
        ConfigurationSection worldsSec = cfg.getConfigurationSection("worlds");
        if (worldsSec != null) {
            for (String worldName : worldsSec.getKeys(false)) {
                ConfigurationSection wsec = worldsSec.getConfigurationSection(worldName);
                if (wsec == null) continue;

                double wBlast = blast;
                double wHard = hard;

                ConfigurationSection wm = wsec.getConfigurationSection("multipliers");
                if (wm != null) {
                    wBlast = wm.getDouble("blast_resistance", wBlast);
                    wHard = wm.getDouble("hardness", wHard);
                }

                Map<Material, Double> wBlastOverrides = new EnumMap<>(Material.class);
                Map<Material, Double> wHardOverrides = new EnumMap<>(Material.class);
                readOverrides(wsec.getConfigurationSection("overrides"), wBlastOverrides, wHardOverrides, log);

                worlds.put(worldName, new WorldOverrides(wBlast, wHard, wBlastOverrides, wHardOverrides));
            }
        }

        return new BDTConfig(
                blast, hard,
                explosionsEnabled,
                miningEnabled, interceptBreak,
                tickRate, radius, clearOnAbort,
                respectWG,
                blastOverrides, hardnessOverrides,
                worlds
        );
    }

    private static void readOverrides(ConfigurationSection overrides,
                                      Map<Material, Double> blastOut,
                                      Map<Material, Double> hardOut,
                                      Logger log) {
        if (overrides == null) return;
        for (String key : overrides.getKeys(false)) {
            Material mat = Material.matchMaterial(key);
            if (mat == null) {
                log.warning("Unknown material in overrides: " + key);
                continue;
            }
            double b = overrides.getDouble(key + ".blast_resistance", Double.NaN);
            if (!Double.isNaN(b)) blastOut.put(mat, b);

            double h = overrides.getDouble(key + ".hardness", Double.NaN);
            if (!Double.isNaN(h)) hardOut.put(mat, h);
        }
    }

    public boolean explosionsEnabled() { return explosionsEnabled; }
    public boolean miningEnabled() { return miningEnabled; }
    public boolean interceptBreak() { return interceptBreak; }
    public int animationTickRate() { return animationTickRate; }
    public int animationRadius() { return animationRadius; }
    public boolean clearCracksOnAbort() { return clearCracksOnAbort; }
    public boolean respectWorldGuard() { return respectWorldGuard; }

    public double effectiveBlastMultiplier(String worldName, Material mat) {
        WorldOverrides w = worldOverrides.get(worldName);
        if (w != null) {
            if (w.blastOverrides.containsKey(mat)) return w.blastOverrides.get(mat);
            return w.blastMultiplier;
        }
        return blastOverrides.getOrDefault(mat, blastMultiplier);
    }

    public double effectiveHardnessMultiplier(String worldName, Material mat) {
        WorldOverrides w = worldOverrides.get(worldName);
        if (w != null) {
            if (w.hardnessOverrides.containsKey(mat)) return w.hardnessOverrides.get(mat);
            return w.hardnessMultiplier;
        }
        return hardnessOverrides.getOrDefault(mat, hardnessMultiplier);
    }
}
