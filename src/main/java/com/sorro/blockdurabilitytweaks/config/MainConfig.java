package com.sorro.blockdurabilitytweaks.config;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.ZoneId;
import java.util.*;

public record MainConfig(
        // Core
        String mode,
        boolean explosionsEnabled,
        boolean miningEnabled,
        boolean interceptBreak,
        int animationTickRate,
        int animationRadius,
        boolean clearCracksOnAbort,
        boolean respectWorldGuard,

        // Stage 2: tools
        boolean perToolMultipliersEnabled,
        ToolMultipliers toolMultipliers,

        // Stage 3: biomes
        boolean perBiomeEnabled,
        BiomeMultipliers biomeMultipliers,

        // Stage 4: GUI
        boolean guiEnabled,
        String guiTitle,
        int guiRows,
        boolean guiRequirePerm,
        String guiPerm,

        // Stage 5: events
        boolean eventsEnabled,
        ZoneId eventsZone,
        Map<String, EventSchedule> schedules,

        // Stage 6: HP + memory
        boolean hpEnabled,
        HpConfig hpConfig,
        boolean damageMemoryEnabled,
        DamageMemoryConfig damageMemoryConfig,

        // Profiles
        Map<String, String> activeProfilePerWorld
) {
    public record ToolMultipliers(
            double def,
            Map<Material, Double> byTool,
            boolean efficiencyEnabled,
            double efficiencyPerLevel
    ) {
        public double forTool(Material tool, int efficiencyLevel) {
            double base = byTool.getOrDefault(tool, def);
            if (!efficiencyEnabled || efficiencyLevel <= 0) return base;
            return base * Math.pow(efficiencyPerLevel, efficiencyLevel);
        }
    }

    public record BiomeMultipliers(
            double def,
            Map<Biome, Double> byBiome,
            HpBiomeBehavior hpBehavior
    ) {
        public double forBiome(Biome biome) {
            return byBiome.getOrDefault(biome, def);
        }
    }

    public enum HpBiomeBehavior { MULTIPLY, DIVIDE }

    public record EventSchedule(
            String name,
            String world,
            String profile,
            String start,
            String end,
            Integer durationMinutes
    ) {}

    public record HpConfig(
            double hpPerHardness,
            int minHp,
            int maxHp,
            Map<Material, Integer> byBlockHp
    ) {}

    public record DamageMemoryConfig(
            String mode,
            int seconds,
            boolean saveToDisk,
            int saveIntervalSeconds,
            boolean cracksEnabled,
            int cracksRadius
    ) {}

    public static MainConfig from(FileConfiguration cfg) {
        String mode = cfg.getString("features.mode", "MULTIPLIER");

        boolean explosionsEnabled = cfg.getBoolean("features.explosions.enabled", true);
        boolean miningEnabled = cfg.getBoolean("features.mining.enabled", true);
        boolean intercept = cfg.getBoolean("features.mining.intercept_break", true);
        int tickRate = Math.max(1, cfg.getInt("features.mining.animation_tick_rate", 2));
        int radius = Math.max(1, cfg.getInt("features.mining.animation_radius", 64));
        boolean clear = cfg.getBoolean("features.mining.clear_cracks_on_abort", true);
        boolean respectWG = cfg.getBoolean("worldguard.respect_worldguard", true);

        boolean perToolEnabled = cfg.getBoolean("features.per_tool_multipliers", false)
                && cfg.getBoolean("tool_multipliers.enabled", false);

        double toolDef = cfg.getDouble("tool_multipliers.default", 1.0);
        Map<Material, Double> byTool = new EnumMap<>(Material.class);
        ConfigurationSection bt = cfg.getConfigurationSection("tool_multipliers.by_tool");
        if (bt != null) {
            for (String key : bt.getKeys(false)) {
                Material m = Material.matchMaterial(key);
                if (m != null) byTool.put(m, bt.getDouble(key, toolDef));
            }
        }
        boolean effEnabled = cfg.getBoolean("tool_multipliers.efficiency.enabled", true);
        double perLevel = cfg.getDouble("tool_multipliers.efficiency.per_level", 0.92);
        ToolMultipliers tm = new ToolMultipliers(toolDef, byTool, effEnabled, perLevel);

        boolean perBiomeEnabled = cfg.getBoolean("features.per_biome_modifiers", false)
                && cfg.getBoolean("biome_modifiers.enabled", false);

        double biomeDef = cfg.getDouble("biome_modifiers.default", 1.0);
        Map<Biome, Double> byBiome = new EnumMap<>(Biome.class);
        ConfigurationSection bb = cfg.getConfigurationSection("biome_modifiers.by_biome");
        if (bb != null) {
            for (String key : bb.getKeys(false)) {
                try {
                    Biome b = Biome.valueOf(key.toUpperCase(Locale.ROOT));
                    byBiome.put(b, bb.getDouble(key, biomeDef));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        HpBiomeBehavior hpBehavior = HpBiomeBehavior.DIVIDE;
        String beh = cfg.getString("biome_modifiers.hp_mode_damage_behavior", "DIVIDE");
        if ("MULTIPLY".equalsIgnoreCase(beh)) hpBehavior = HpBiomeBehavior.MULTIPLY;
        BiomeMultipliers bm = new BiomeMultipliers(biomeDef, byBiome, hpBehavior);

        boolean guiEnabled = cfg.getBoolean("features.gui_profile_selector", false)
                && cfg.getBoolean("gui.enabled", false);
        String guiTitle = cfg.getString("gui.title", "&bDurability Profiles");
        int guiRows = Math.max(1, Math.min(6, cfg.getInt("gui.rows", 6)));
        boolean guiRequirePerm = cfg.getBoolean("gui.require_permission", true);
        String guiPerm = cfg.getString("gui.permission", "blockdurabilitytweaks.gui.open");

        boolean eventsEnabled = cfg.getBoolean("features.timed_profile_events", false)
                && cfg.getBoolean("events.enabled", false);
        String tz = cfg.getString("events.timezone", "UTC");
        ZoneId zone;
        try { zone = ZoneId.of(tz); } catch (Exception e) { zone = ZoneId.of("UTC"); }

        Map<String, EventSchedule> schedules = new LinkedHashMap<>();
        ConfigurationSection schedSec = cfg.getConfigurationSection("events.schedules");
        if (schedSec != null) {
            for (String name : schedSec.getKeys(false)) {
                ConfigurationSection s = schedSec.getConfigurationSection(name);
                if (s == null) continue;
                schedules.put(name, new EventSchedule(
                        name,
                        s.getString("world", "world"),
                        s.getString("profile", "vanilla"),
                        s.getString("start", ""),
                        s.getString("end", null),
                        s.contains("duration_minutes") ? s.getInt("duration_minutes") : null
                ));
            }
        }

        boolean hpEnabled = cfg.getBoolean("features.mode", "MULTIPLIER").equalsIgnoreCase("HP")
                && cfg.getBoolean("hp_mode.enabled", false);

        double hpPerHardness = cfg.getDouble("hp_mode.defaults.hp_per_hardness", 50.0);
        int minHp = cfg.getInt("hp_mode.defaults.min_hp", 10);
        int maxHp = cfg.getInt("hp_mode.defaults.max_hp", 5000);
        Map<Material, Integer> hpByBlock = new EnumMap<>(Material.class);
        ConfigurationSection hbb = cfg.getConfigurationSection("hp_mode.by_block");
        if (hbb != null) {
            for (String key : hbb.getKeys(false)) {
                Material m = Material.matchMaterial(key);
                if (m != null && m.isBlock()) hpByBlock.put(m, hbb.getInt(key));
            }
        }
        HpConfig hpCfg = new HpConfig(hpPerHardness, minHp, maxHp, hpByBlock);

        boolean dmgMemEnabled = cfg.getBoolean("features.persistent_block_damage", false)
                && cfg.getBoolean("damage_memory.enabled", false);

        String memMode = cfg.getString("damage_memory.decay.mode", "REGEN");
        int memSeconds = cfg.getInt("damage_memory.decay.seconds", 120);
        boolean saveDisk = cfg.getBoolean("damage_memory.persistence.save_to_disk", false);
        int saveEvery = Math.max(10, cfg.getInt("damage_memory.persistence.save_interval_seconds", 60));
        boolean cracks = cfg.getBoolean("damage_memory.cracks.enabled", true);
        int crackRadius = Math.max(8, cfg.getInt("damage_memory.cracks.broadcast_radius", 64));
        DamageMemoryConfig dmc = new DamageMemoryConfig(memMode, memSeconds, saveDisk, saveEvery, cracks, crackRadius);

        Map<String, String> active = new HashMap<>();
        ConfigurationSection sec = cfg.getConfigurationSection("profiles.active_per_world");
        if (sec != null) {
            for (String world : sec.getKeys(false)) {
                active.put(world, sec.getString(world, "vanilla"));
            }
        }

        return new MainConfig(mode, explosionsEnabled, miningEnabled, intercept, tickRate, radius, clear, respectWG,
                perToolEnabled, tm,
                perBiomeEnabled, bm,
                guiEnabled, guiTitle, guiRows, guiRequirePerm, guiPerm,
                eventsEnabled, zone, schedules,
                hpEnabled, hpCfg, dmgMemEnabled, dmc,
                active);
    }

    public String activeProfileForWorld(String world) {
        return activeProfilePerWorld.getOrDefault(world, "vanilla");
    }
}
