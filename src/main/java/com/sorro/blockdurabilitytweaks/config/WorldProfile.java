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

    public double hardnessFor(Material mat) {
        return hardnessOverrides.getOrDefault(mat, hardnessMultiplier);
    }

    public double blastFor(Material mat) {
        return blastOverrides.getOrDefault(mat, blastMultiplier);
    }
}
