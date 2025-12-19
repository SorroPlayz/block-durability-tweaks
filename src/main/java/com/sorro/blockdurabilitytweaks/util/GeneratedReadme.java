package com.sorro.blockdurabilitytweaks.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class GeneratedReadme {

    private GeneratedReadme() {}

    public static void ensureGenerated(JavaPlugin plugin) { write(plugin); }

    public static void write(JavaPlugin plugin) {
        File out = new File(plugin.getDataFolder(), "README-GENERATED.txt");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        String txt =
                "BlockDurabilityTweaks (1.20.1) - Stages 1-6\n" +
                "===========================================\n\n" +

                "Folder layout (auto-generated):\n" +
                "  plugins/BlockDurabilityTweaks/\n" +
                "    config.yml\n" +
                "    README-GENERATED.txt\n" +
                "    worlds/<world>/vanilla.yml\n" +
                "    worlds/<world>/profiles/<profile>.yml\n" +
                "    worlds/<world>/damage.yml (only if damage_memory.persistence.save_to_disk=true)\n\n" +

                "Vanilla-by-default:\n" +
                "  - Active profile is 'vanilla' unless you load something else.\n" +
                "  - vanilla profile has multipliers 1.0 and no overrides.\n\n" +

                "Commands:\n" +
                "  /blockdurability info\n" +
                "  /blockdurability reload\n" +
                "  /blockdurability gui\n" +
                "  /blockdurability world list [world]\n" +
                "  /blockdurability world load <world> <profile>\n" +
                "  /blockdurability world save <world> <profile>\n" +
                "  /blockdurability world reset <world>\n" +
                "  /blockdurability world vanilla <world>\n" +
                "  /blockdurability events list\n" +
                "  /blockdurability events run <name>\n" +
                "  /blockdurability events stop <name>\n\n" +

                "Feature toggles (config.yml -> features.*):\n" +
                "  per_tool_multipliers, per_biome_modifiers, gui_profile_selector, timed_profile_events, persistent_block_damage\n\n" +

                "HP mode:\n" +
                "  - Set features.mode: HP and hp_mode.enabled: true\n" +
                "  - Optional remembered damage uses damage_memory.*\n";

        try {
            Files.writeString(out.toPath(), txt, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed writing README-GENERATED.txt: " + e.getMessage());
        }
    }
}
