package com.sorro.blockdurabilitytweaks.util;

import org.bukkit.Material;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Best-effort vanilla stats reader.
 * Uses reflection to access CraftBukkit's CraftMagicNumbers -> NMS Block fields.
 * If unavailable (different server implementation), values return NaN.
 */
public final class VanillaStatsUtil {

    private static Method craftGetBlock;
    private static Field nmsDestroyTime;
    private static Field nmsExplosionResistance;
    private static boolean initTried = false;

    private VanillaStatsUtil() {}

    private static synchronized void init() {
        if (initTried) return;
        initTried = true;

        try {
            Class<?> craftMagic = Class.forName("org.bukkit.craftbukkit.util.CraftMagicNumbers");
            craftGetBlock = craftMagic.getMethod("getBlock", Material.class);

            Object anyBlock = craftGetBlock.invoke(null, Material.STONE);
            if (anyBlock == null) return;

            Class<?> blockClazz = anyBlock.getClass();
            nmsDestroyTime = findField(blockClazz, "destroyTime");
            nmsExplosionResistance = findField(blockClazz, "explosionResistance");
            if (nmsDestroyTime != null) nmsDestroyTime.setAccessible(true);
            if (nmsExplosionResistance != null) nmsExplosionResistance.setAccessible(true);
        } catch (Throwable ignored) {
            craftGetBlock = null;
            nmsDestroyTime = null;
            nmsExplosionResistance = null;
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (Throwable ignored) { }
            c = c.getSuperclass();
        }
        return null;
    }

    public static VanillaStats getVanillaStats(Material material) {
        if (material == null || !material.isBlock()) return new VanillaStats(Float.NaN, Float.NaN);
        init();
        if (craftGetBlock == null) return new VanillaStats(Float.NaN, Float.NaN);

        try {
            Object nmsBlock = craftGetBlock.invoke(null, material);
            if (nmsBlock == null) return new VanillaStats(Float.NaN, Float.NaN);

            float hardness = Float.NaN;
            float blast = Float.NaN;

            if (nmsDestroyTime != null) hardness = nmsDestroyTime.getFloat(nmsBlock);
            if (nmsExplosionResistance != null) blast = nmsExplosionResistance.getFloat(nmsBlock);

            return new VanillaStats(hardness, blast);
        } catch (Throwable t) {
            return new VanillaStats(Float.NaN, Float.NaN);
        }
    }
}
