package com.sorro.blockdurabilitytweaks.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public final class GeneratedReadme {
    private GeneratedReadme() {}

    public static void ensureGenerated(JavaPlugin plugin) {
        try {
            File f = new File(plugin.getDataFolder(), "README-GENERATED.txt");
            if (f.exists()) return;
            plugin.getDataFolder().mkdirs();

            String txt = ""
                    + "BlockDurabilityTweaks (1.20.1)\n"
                    + "===========================\n\n"
                    + "This plugin is VANILLA-BY-DEFAULT.\n"
                    + "Nothing changes unless you load a profile that changes values.\n\n"
                    + "Folder layout:\n"
                    + "  plugins/BlockDurabilityTweaks/\n"
                    + "    config.yml\n"
                    + "    worlds/<world>/vanilla.yml\n"
                    + "    worlds/<world>/profiles/<profile>.yml\n\n"
                    + "Commands:\n"
                    + "  /blockdurability info\n"
                    + "  /blockdurability reload\n"
                    + "  /blockdurability world list [world]\n"
                    + "  /blockdurability world load <world> <profile>\n"
                    + "  /blockdurability world save <world> <profile>\n"
                    + "  /blockdurability world reset <world>\n"
                    + "  /blockdurability world vanilla <world>\n\n"
                    + "Where to change blocks:\n"
                    + "  Edit a profile file, for example:\n"
                    + "    plugins/BlockDurabilityTweaks/worlds/world/profiles/rust_hardcore.yml\n\n"
                    + "Profile format:\n"
                    + "  multipliers:\n"
                    + "    hardness: 2.0\n"
                    + "    blast_resistance: 1.0\n"
                    + "  overrides:\n"
                    + "    STONE:\n"
                    + "      hardness: 3.0\n"
                    + "    OBSIDIAN:\n"
                    + "      hardness: 5.0\n"
                    + "      blast_resistance: 2.0\n\n"
                    + "Notes:\n"
                    + "  - Mining slowdown only applies when effective hardness > 1.0\n"
                    + "  - vanilla.yml is a reference dump (best-effort via reflection).\n";

            try (FileWriter fw = new FileWriter(f, StandardCharsets.UTF_8)) {
                fw.write(txt);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed generating README-GENERATED.txt: " + t.getMessage());
        }
    }
}
