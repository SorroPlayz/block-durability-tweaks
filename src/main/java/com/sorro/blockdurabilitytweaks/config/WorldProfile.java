package com.sorro.blockdurabilitytweaks.config;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

public class WorldProfile {

    public final String worldName;
    public final String profileName;

    public double hardnessMultiplier = 1.0;
    public double blastMultiplier = 1.0;

    public final Map<Material, Double> hardnessOverrides = new EnumMap<>(Material.class);
    public final Map<Material, Double> blastOverrides = new EnumMap<>(Material.class);

    public WorldProfile(String worldName, String profileName) {
        this.worldName = worldName;
        this.profileName = profileName;
    }

    /** Returns multiplier for hardness (not absolute vanilla value). */
    public double hardnessFor(Material m) {
        return hardnessOverrides.getOrDefault(m, hardnessMultiplier);
    }

    /** Returns multiplier for blast resistance (not absolute vanilla value). */
    public double blastFor(Material m) {
        return blastOverrides.getOrDefault(m, blastMultiplier);
    }
}
