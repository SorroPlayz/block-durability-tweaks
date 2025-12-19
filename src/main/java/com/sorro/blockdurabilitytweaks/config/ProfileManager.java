package com.sorro.blockdurabilitytweaks.config;

import com.sorro.blockdurabilitytweaks.util.VanillaStatsDumper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileManager {

    private final JavaPlugin plugin;
    private final MainConfig main;
    private final File baseDir;

    private final Map<String, WorldProfile> activeProfiles = new ConcurrentHashMap<>();

    public ProfileManager(JavaPlugin plugin, MainConfig main) {
        this.plugin = plugin;
        this.main = main;
        this.baseDir = new File(plugin.getDataFolder(), "worlds");
    }

    public void ensureAllLoaded() {
        if (!baseDir.exists()) baseDir.mkdirs();

        for (World w : Bukkit.getWorlds()) {
            ensureWorldFiles(w.getName());
            loadProfile(w.getName(), main.activeProfileForWorld(w.getName()));
        }
    }

    public void ensureWorldFiles(String worldName) {
        File wdir = worldDir(worldName);
        File profilesDir = new File(wdir, "profiles");
        if (!profilesDir.exists()) profilesDir.mkdirs();

        File vanillaRef = new File(wdir, "vanilla.yml");
        if (!vanillaRef.exists()) {
            VanillaStatsDumper.dumpVanilla(plugin, vanillaRef);
        }

        File vanillaProfile = profileFile(worldName, "vanilla");
        if (!vanillaProfile.exists()) {
            YamlConfiguration y = new YamlConfiguration();
            y.set("info.description", "Vanilla (no changes). Multipliers 1.0, no overrides.");
            y.set("multipliers.hardness", 1.0);
            y.set("multipliers.blast_resistance", 1.0);
            y.createSection("overrides");
            try { y.save(vanillaProfile); } catch (IOException e) {
                plugin.getLogger().warning("Failed to write vanilla profile for " + worldName + ": " + e.getMessage());
            }
        }
    }

    public WorldProfile getActive(String worldName) {
        return activeProfiles.getOrDefault(worldName, new WorldProfile(worldName, "vanilla"));
    }

    public Set<String> listProfiles(String worldName) {
        File profilesDir = new File(worldDir(worldName), "profiles");
        if (!profilesDir.exists()) return Set.of();
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        File[] files = profilesDir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                String n = f.getName();
                names.add(n.substring(0, n.length() - 4));
            }
        }
        return names;
    }

    public boolean loadProfile(String worldName, String profileName) {
        ensureWorldFiles(worldName);

        File f = profileFile(worldName, profileName);
        if (!f.exists()) return false;

        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        WorldProfile p = new WorldProfile(worldName, profileName);
        p.hardnessMultiplier = y.getDouble("multipliers.hardness", 1.0);
        p.blastMultiplier = y.getDouble("multipliers.blast_resistance", 1.0);

        var ov = y.getConfigurationSection("overrides");
        if (ov != null) {
            for (String key : ov.getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat == null || !mat.isBlock()) continue;

                double h = ov.getDouble(key + ".hardness", Double.NaN);
                if (!Double.isNaN(h)) p.hardnessOverrides.put(mat, h);

                double b = ov.getDouble(key + ".blast_resistance", Double.NaN);
                if (!Double.isNaN(b)) p.blastOverrides.put(mat, b);
            }
        }

        activeProfiles.put(worldName, p);
        plugin.getLogger().info("Loaded profile '" + profileName + "' for world '" + worldName + "'.");
        return true;
    }

    public boolean saveProfileFromActive(String worldName, String profileName) {
        WorldProfile active = getActive(worldName);
        ensureWorldFiles(worldName);

        File f = profileFile(worldName, profileName);
        YamlConfiguration y = new YamlConfiguration();

        y.set("info.description", "Profile saved from currently active in-memory settings.");
        y.set("multipliers.hardness", active.hardnessMultiplier);
        y.set("multipliers.blast_resistance", active.blastMultiplier);

        var ov = y.createSection("overrides");
        for (Material m : active.hardnessOverrides.keySet()) {
            ov.set(m.name() + ".hardness", active.hardnessOverrides.get(m));
        }
        for (Material m : active.blastOverrides.keySet()) {
            ov.set(m.name() + ".blast_resistance", active.blastOverrides.get(m));
        }

        try {
            y.save(f);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save profile " + profileName + " for " + worldName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean resetToVanilla(String worldName) {
        return loadProfile(worldName, "vanilla");
    }

    public boolean regenerateVanilla(String worldName) {
        ensureWorldFiles(worldName);
        File vanillaRef = new File(worldDir(worldName), "vanilla.yml");
        return VanillaStatsDumper.dumpVanilla(plugin, vanillaRef);
    }

    private File worldDir(String worldName) {
        return new File(baseDir, worldName);
    }

    private File profileFile(String worldName, String profileName) {
        return new File(new File(worldDir(worldName), "profiles"), profileName + ".yml");
    }
}
