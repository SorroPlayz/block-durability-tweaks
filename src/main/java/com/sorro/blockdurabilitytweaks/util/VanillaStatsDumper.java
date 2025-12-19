package com.sorro.blockdurabilitytweaks.util;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

public final class VanillaStatsDumper {

    private VanillaStatsDumper() {}

    public static boolean dumpVanilla(JavaPlugin plugin, File outFile) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("info.generated_at", Instant.now().toString());
        y.set("info.note", "Vanilla reference values read via reflection. If a value is NaN, your server build didn't expose it.");

        for (Material m : Material.values()) {
            if (!m.isBlock()) continue;
            VanillaStats s = VanillaStatsUtil.getVanillaStats(m);
            y.set("blocks." + m.name() + ".hardness", (double) s.hardness());
            y.set("blocks." + m.name() + ".blast_resistance", (double) s.blastResistance());
        }

        try {
            outFile.getParentFile().mkdirs();
            y.save(outFile);
            plugin.getLogger().info("Wrote vanilla reference stats: " + outFile.getPath());
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write vanilla reference: " + e.getMessage());
            return false;
        }
    }
}
