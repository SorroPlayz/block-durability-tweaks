package com.sorro.blockdurabilitytweaks.util;

import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

public record BlockKey(UUID worldId, int x, int y, int z) {
    public static BlockKey of(World w, int x, int y, int z) {
        return new BlockKey(w.getUID(), x, y, z);
    }
    @Override public int hashCode() { return Objects.hash(worldId, x, y, z); }
}
