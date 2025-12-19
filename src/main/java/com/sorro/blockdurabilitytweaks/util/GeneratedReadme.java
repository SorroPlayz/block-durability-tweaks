package com.sorro.blockdurabilitytweaks.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class GeneratedReadme {

    private GeneratedReadme() {}

    /** Backwards-compatible alias if older code calls ensureGenerated(...) */
    public static void ensureGenerated(JavaPlugin plugin) {
        write(plugin);
    }

    public static void write(JavaPlugin plugin) {
        File out = new File(plugin.getDataFolder(), "README-GENERATED.txt");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        String txt =
                "BlockDurabilityTweaks (1.20.1) - Stage 1+2\n" +
                "=========================================\n\n" +

                "Folder layout (auto-generated):\n" +
                "  plugins/BlockDurabilityTweaks/\n" +
                "    config.yml\n" +
                "    README-GENERATED.txt\n" +
                "    worlds/\n" +
                "      <world>/\n" +
                "        vanilla.yml\n" +
                "        profiles/\n" +
                "          vanilla.yml\n" +
                "          <your_profile>.yml\n\n" +

                "Vanilla-by-default:\n" +
                "  - Active profile is 'vanilla' unless you load something else.\n" +
                "  - vanilla profile has multipliers 1.0 and no overrides.\n\n" +

                "Commands:\n" +
                "  /blockdurability info\n" +
                "  /blockdurability reload\n" +
                "  /blockdurability world list [world]\n" +
                "  /blockdurability world load <world> <profile>\n" +
                "  /blockdurability world save <world> <profile>\n" +
                "  /blockdurability world reset <world>\n" +
                "  /blockdurability world vanilla <world>\n\n" +

                "Per-tool multipliers (Stage 2):\n" +
                "  - Toggle on in config.yml:\n" +
                "      features.per_tool_multipliers: true\n" +
                "      tool_multipliers.enabled: true\n" +
                "  - Configure by tool Material names:\n" +
                "      tool_multipliers.by_tool.NETHERITE_PICKAXE: 1.10\n" +
                "      tool_multipliers.by_tool.IRON_PICKAXE: 1.30\n" +
                "  - Optional Efficiency scaling:\n" +
                "      tool_multipliers.efficiency.enabled: true\n" +
                "      tool_multipliers.efficiency.per_level: 0.92\n\n" +

                "Notes:\n" +
                "  - Mining slowdown is applied only when final multiplier > 1.0.\n" +
                "  - Explosion blast resistance is emulated by filtering which blocks break.\n";

        try {
            Files.writeString(out.toPath(), txt, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed writing README-GENERATED.txt: " + e.getMessage());
        }
    }
}
