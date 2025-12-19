package com.sorro.blockdurabilitytweaks.hp;

import com.sorro.blockdurabilitytweaks.config.MainConfig;
import com.sorro.blockdurabilitytweaks.util.BlockKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DamageStore {

    public static class DamageState {
        public int maxHp;
        public int hp;
        public long lastTouchedMs;
        public Material type;

        public DamageState(Material type, int maxHp, int hp, long lastTouchedMs) {
            this.type = type;
            this.maxHp = maxHp;
            this.hp = hp;
            this.lastTouchedMs = lastTouchedMs;
        }
    }

    private final JavaPlugin plugin;
    private volatile MainConfig cfg;
    private final Map<BlockKey, DamageState> map = new ConcurrentHashMap<>();

    public DamageStore(JavaPlugin plugin, MainConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    public void reload(MainConfig cfg) {
        this.cfg = cfg;
    }

    public DamageState get(BlockKey key) { return map.get(key); }
    public void put(BlockKey key, DamageState s) { map.put(key, s); }
    public void remove(BlockKey key) { map.remove(key); }
    public Collection<Map.Entry<BlockKey, DamageState>> entries() { return map.entrySet(); }

    public void tickCleanupAndDecay() {
        if (!cfg.damageMemoryEnabled()) {
            map.clear();
            return;
        }
        long now = System.currentTimeMillis();
        int seconds = cfg.damageMemoryConfig().seconds();
        String mode = cfg.damageMemoryConfig().mode().toUpperCase(Locale.ROOT);

        for (Iterator<Map.Entry<BlockKey, DamageState>> it = map.entrySet().iterator(); it.hasNext();) {
            var e = it.next();
            DamageState s = e.getValue();

            long ageMs = now - s.lastTouchedMs;
            if (ageMs < 0) ageMs = 0;

            if ("NONE".equals(mode)) {
                // keep forever unless block changes type
            } else if ("REGEN".equals(mode)) {
                if (ageMs > 0) {
                    double t = Math.min(1.0, ageMs / (seconds * 1000.0));
                    int newHp = (int)Math.round(s.hp + (s.maxHp - s.hp) * t);
                    s.hp = Math.min(s.maxHp, Math.max(0, newHp));
                    if (s.hp >= s.maxHp) {
                        it.remove();
                        continue;
                    }
                }
            } else if ("DECAY".equals(mode)) {
                if (ageMs > 0) {
                    double t = Math.min(1.0, ageMs / (seconds * 1000.0));
                    int newHp = (int)Math.round(s.hp * (1.0 - t));
                    s.hp = Math.max(0, newHp);
                    if (s.hp <= 0) {
                        it.remove();
                        continue;
                    }
                }
            }

            // Validate block still same type (prevent leaking data)
            World w = Bukkit.getWorld(e.getKey().worldId());
            if (w == null) { it.remove(); continue; }
            Material current = w.getBlockAt(e.getKey().x(), e.getKey().y(), e.getKey().z()).getType();
            if (current != s.type) it.remove();
        }
    }

    public void loadAllFromDisk() {
        if (!cfg.damageMemoryEnabled() || !cfg.damageMemoryConfig().saveToDisk()) return;

        for (World w : Bukkit.getWorlds()) {
            File f = damageFile(w.getName());
            if (!f.exists()) continue;

            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            var sec = y.getConfigurationSection("damage");
            if (sec == null) continue;

            for (String key : sec.getKeys(false)) {
                String[] parts = key.split(",");
                if (parts.length != 3) continue;
                int x, yy, z;
                try {
                    x = Integer.parseInt(parts[0]);
                    yy = Integer.parseInt(parts[1]);
                    z = Integer.parseInt(parts[2]);
                } catch (NumberFormatException ex) { continue; }

                String typeName = sec.getString(key + ".type", "STONE");
                Material type = Material.matchMaterial(typeName);
                if (type == null || !type.isBlock()) continue;

                int max = sec.getInt(key + ".max", 0);
                int hp = sec.getInt(key + ".hp", 0);
                long last = sec.getLong(key + ".last", System.currentTimeMillis());
                if (max <= 0 || hp <= 0) continue;

                BlockKey bk = BlockKey.of(w, x, yy, z);
                map.put(bk, new DamageState(type, max, hp, last));
            }

            plugin.getLogger().info("Loaded damage memory for world " + w.getName() + " (" + map.size() + " entries total).");
        }
    }

    public void saveWorldToDisk(String worldName) {
        if (!cfg.damageMemoryEnabled() || !cfg.damageMemoryConfig().saveToDisk()) return;

        World w = Bukkit.getWorld(worldName);
        if (w == null) return;

        File f = damageFile(worldName);
        YamlConfiguration y = new YamlConfiguration();
        y.set("info.saved_at", Instant.now().toString());

        for (var e : map.entrySet()) {
            if (!e.getKey().worldId().equals(w.getUID())) continue;
            DamageState s = e.getValue();
            String k = e.getKey().x() + "," + e.getKey().y() + "," + e.getKey().z();
            y.set("damage." + k + ".type", s.type.name());
            y.set("damage." + k + ".max", s.maxHp);
            y.set("damage." + k + ".hp", s.hp);
            y.set("damage." + k + ".last", s.lastTouchedMs);
        }

        try {
            f.getParentFile().mkdirs();
            y.save(f);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed saving damage memory for " + worldName + ": " + ex.getMessage());
        }
    }

    private File damageFile(String worldName) {
        return new File(new File(new File(plugin.getDataFolder(), "worlds"), worldName), "damage.yml");
    }
}
