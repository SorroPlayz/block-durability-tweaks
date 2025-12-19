package com.sorro.blockdurabilitytweaks.mining;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.sorro.blockdurabilitytweaks.config.MainConfig;
import com.sorro.blockdurabilitytweaks.config.ProfileManager;
import com.sorro.blockdurabilitytweaks.config.WorldProfile;
import com.sorro.blockdurabilitytweaks.hook.WorldGuardHook;
import com.sorro.blockdurabilitytweaks.hp.DamageStore;
import com.sorro.blockdurabilitytweaks.util.BlockKey;
import com.sorro.blockdurabilitytweaks.util.BreakUtil;
import com.sorro.blockdurabilitytweaks.util.VanillaStats;
import com.sorro.blockdurabilitytweaks.util.VanillaStatsUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MiningController implements Listener {

    private final JavaPlugin plugin;
    private final ProtocolManager protocol;
    private final WorldGuardHook worldGuard;

    private volatile MainConfig cfg;
    private volatile ProfileManager profiles;
    private volatile DamageStore damageStore;

    private PacketListener digListener;
    private int taskId = -1;

    private final Map<UUID, MiningSession> sessions = new ConcurrentHashMap<>();
    private int saveTick = 0;

    public MiningController(JavaPlugin plugin, MainConfig cfg, ProfileManager profiles, DamageStore damageStore, ProtocolManager protocol, WorldGuardHook worldGuard) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.profiles = profiles;
        this.damageStore = damageStore;
        this.protocol = protocol;
        this.worldGuard = worldGuard;
    }

    public void reload(MainConfig cfg, ProfileManager pm, DamageStore ds) {
        this.cfg = cfg;
        this.profiles = pm;
        this.damageStore = ds;
        this.worldGuard.reload(cfg);
        this.damageStore.reload(cfg);
    }

    public void enable() {
        digListener = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!cfg.miningEnabled() || !cfg.interceptBreak()) return;

                Player player = event.getPlayer();
                var packet = event.getPacket();
                var digType = packet.getPlayerDigTypes().read(0);
                BlockPosition pos = packet.getBlockPositionModifier().read(0);
                if (pos == null) return;

                World world = player.getWorld();
                Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());

                switch (digType.name()) {
                    case "START_DESTROY_BLOCK" -> start(player, block);
                    case "ABORT_DESTROY_BLOCK", "STOP_DESTROY_BLOCK" -> stop(player);
                    default -> { }
                }
            }
        };

        protocol.addPacketListener(digListener);
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 1L, 1L);
    }

    public void disable() {
        if (digListener != null) {
            protocol.removePacketListener(digListener);
            digListener = null;
        }
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        sessions.clear();
    }

    private void start(Player player, Block block) {
        if (!player.isOnline()) return;
        if (block.getType().isAir()) return;
        if (!worldGuard.allowed(player, block.getLocation())) return;
        sessions.put(player.getUniqueId(), new MiningSession(block, System.currentTimeMillis()));
    }

    private void stop(Player player) {
        MiningSession s = sessions.remove(player.getUniqueId());
        if (s != null && cfg.clearCracksOnAbort() && cfg.damageMemoryEnabled() && cfg.damageMemoryConfig().cracksEnabled()) {
            // Clear crack animation
            sendCrack(player.getWorld(), s.block.getX(), s.block.getY(), s.block.getZ(), -1);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!cfg.miningEnabled() || !cfg.interceptBreak()) return;
        Player player = event.getPlayer();
        MiningSession s = sessions.get(player.getUniqueId());
        if (s == null) return;
        if (!sameBlock(event.getBlock(), s.block)) return;

        if (event.isCancelled()) { stop(player); return; }

        if ("MULTIPLIER".equalsIgnoreCase(cfg.mode())) {
            handleMultiplier(event, player, s);
        } else if ("HP".equalsIgnoreCase(cfg.mode()) && cfg.hpEnabled()) {
            event.setCancelled(true);
            event.setDropItems(false);
            event.setExpToDrop(0);
            // breaking is controlled by HP tick; vanilla break event just signals "they reached break point" which we override.
        }
    }

    private void handleMultiplier(BlockBreakEvent event, Player player, MiningSession s) {
        long now = System.currentTimeMillis();
        if (s.vanillaCompleteMs < 0) {
            s.vanillaCompleteMs = now;

            String worldName = s.block.getWorld().getName();
            WorldProfile prof = profiles.getActive(worldName);

            Material mat = s.block.getType();

            double profileMult = prof.hardnessFor(mat);
            double toolMult = toolMultiplier(player);
            double biomeMult = biomeMultiplier(s.block);

            double finalMult = profileMult * toolMult * biomeMult;

            if (finalMult <= 1.0) {
                sessions.remove(player.getUniqueId());
                return;
            }

            long vanillaDuration = Math.max(1, s.vanillaCompleteMs - s.startMs);
            long targetDuration = (long) Math.ceil(vanillaDuration * finalMult);
            s.targetBreakMs = s.startMs + targetDuration;
        }

        if (s.targetBreakMs > 0 && now < s.targetBreakMs) {
            event.setCancelled(true);
            event.setDropItems(false);
            event.setExpToDrop(0);
        } else if (s.targetBreakMs > 0) {
            event.setCancelled(true);
            event.setDropItems(false);
            event.setExpToDrop(0);

            sessions.remove(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (s.block.getType().isAir()) return;
                if (!worldGuard.allowed(player, s.block.getLocation())) return;
                BreakUtil.breakAsPlayer(player, s.block);
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        stop(e.getPlayer());
    }

    private void tick() {
        if (!cfg.miningEnabled() || !cfg.interceptBreak()) return;

        // Stage 6: damage store decay/cleanup + optional disk save
        if (cfg.damageMemoryEnabled()) {
            damageStore.tickCleanupAndDecay();
            if (cfg.damageMemoryConfig().saveToDisk()) {
                saveTick++;
                if (saveTick >= cfg.damageMemoryConfig().saveIntervalSeconds()) {
                    saveTick = 0;
                    for (World w : Bukkit.getWorlds()) damageStore.saveWorldToDisk(w.getName());
                }
            }
        }

        if ("HP".equalsIgnoreCase(cfg.mode()) && cfg.hpEnabled()) {
            // Apply HP damage while player is holding left click (session exists)
            for (var entry : sessions.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) continue;
                MiningSession s = entry.getValue();
                Block block = s.block;
                if (block.getType().isAir()) continue;

                if (!worldGuard.allowed(player, block.getLocation())) continue;

                // compute / load state
                BlockKey key = BlockKey.of(block.getWorld(), block.getX(), block.getY(), block.getZ());
                DamageStore.DamageState st = damageStore.get(key);

                Material type = block.getType();
                int maxHp = computeMaxHp(type);
                if (st == null) {
                    st = new DamageStore.DamageState(type, maxHp, maxHp, System.currentTimeMillis());
                    damageStore.put(key, st);
                } else {
                    // refresh if block changed
                    if (st.type != type || st.maxHp != maxHp) {
                        st.type = type;
                        st.maxHp = maxHp;
                        st.hp = Math.min(st.hp, maxHp);
                    }
                }

                int dmg = computeDamagePerTick(player, block);
                if (dmg <= 0) dmg = 1;

                st.hp = Math.max(0, st.hp - dmg);
                st.lastTouchedMs = System.currentTimeMillis();

                if (cfg.damageMemoryConfig().cracksEnabled()) {
                    int stage = crackStage(st.hp, st.maxHp);
                    sendCrack(block.getWorld(), block.getX(), block.getY(), block.getZ(), stage);
                }

                if (st.hp <= 0) {
                    damageStore.remove(key);
                    sessions.remove(player.getUniqueId());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (block.getType().isAir()) return;
                        if (!worldGuard.allowed(player, block.getLocation())) return;
                        BreakUtil.breakAsPlayer(player, block);
                    });
                }
            }
        }
    }

    private int computeMaxHp(Material blockType) {
        Integer explicit = cfg.hpConfig().byBlockHp().get(blockType);
        if (explicit != null) return clamp(explicit, cfg.hpConfig().minHp(), cfg.hpConfig().maxHp());

        VanillaStats v = VanillaStatsUtil.getVanillaStats(blockType);
        float hardness = v.hardness();
        if (Float.isNaN(hardness) || hardness <= 0) hardness = 1.0f;

        int hp = (int)Math.round(hardness * cfg.hpConfig().hpPerHardness());
        return clamp(hp, cfg.hpConfig().minHp(), cfg.hpConfig().maxHp());
    }

    private int computeDamagePerTick(Player player, Block block) {
        // base damage per tick
        int dmg = 5;

        // tool multiplier makes you slower/faster in HP mode:
        double toolMult = toolMultiplier(player);
        if (cfg.perToolMultipliersEnabled()) {
            // higher tool multiplier means "tougher" in MULTIPLIER mode; in HP mode we invert to represent better tools
            dmg = (int)Math.max(1, Math.round(dmg * (1.0 / toolMult)));
        }

        // biome effect
        double b = biomeMultiplier(block);
        if (cfg.perBiomeEnabled()) {
            if (cfg.biomeMultipliers().hpBehavior() == MainConfig.HpBiomeBehavior.DIVIDE) {
                dmg = (int)Math.max(1, Math.round(dmg * (1.0 / b)));
            } else {
                dmg = (int)Math.max(1, Math.round(dmg * b));
            }
        }

        // profile hardness influences HP toughness (hardness >1 => tougher)
        WorldProfile prof = profiles.getActive(block.getWorld().getName());
        double profile = prof.hardnessFor(block.getType());
        dmg = (int)Math.max(1, Math.round(dmg * (1.0 / Math.max(0.1, profile))));

        return dmg;
    }

    private double toolMultiplier(Player player) {
        if (!cfg.perToolMultipliersEnabled()) return 1.0;
        ItemStack tool = player.getInventory().getItemInMainHand();
        Material toolType = (tool == null) ? Material.AIR : tool.getType();
        int eff = tool != null ? tool.getEnchantmentLevel(Enchantment.EFFICIENCY) : 0;
        return cfg.toolMultipliers().forTool(toolType, eff);
    }

    private double biomeMultiplier(Block block) {
        if (!cfg.perBiomeEnabled()) return 1.0;
        try {
            Biome biome = block.getBiome();
            return cfg.biomeMultipliers().forBiome(biome);
        } catch (Throwable t) {
            return 1.0;
        }
    }

    private int crackStage(int hp, int maxHp) {
        if (maxHp <= 0) return -1;
        double pct = 1.0 - (hp / (double)maxHp);
        int stage = (int)Math.floor(pct * 10.0);
        if (stage < 0) stage = 0;
        if (stage > 9) stage = 9;
        return stage;
    }

    private void sendCrack(World world, int x, int y, int z, int stage) {
        if (!cfg.damageMemoryEnabled() || !cfg.damageMemoryConfig().cracksEnabled()) return;

        // Packet: BLOCK_BREAK_ANIMATION
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        // entityId: arbitrary stable-ish (hash)
        int id = Objects.hash(world.getUID(), x, y, z);
        packet.getIntegers().write(0, id);
        packet.getBlockPositionModifier().write(0, new BlockPosition(x, y, z));
        packet.getIntegers().write(1, stage);

        int radius = cfg.damageMemoryConfig().cracksRadius();
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(new Location(world, x+0.5, y+0.5, z+0.5)) <= radius * radius) {
                try { protocol.sendServerPacket(p, packet); } catch (Exception ignored) {}
            }
        }
    }

    private boolean sameBlock(Block a, Block b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.getWorld() != b.getWorld()) return false;
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ();
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
