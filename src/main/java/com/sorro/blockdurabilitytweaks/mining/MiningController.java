package com.sorro.blockdurabilitytweaks.mining;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.sorro.blockdurabilitytweaks.config.BDTConfig;
import com.sorro.blockdurabilitytweaks.hook.WorldGuardHook;
import com.sorro.blockdurabilitytweaks.util.BreakUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MiningController implements Listener {

    private final JavaPlugin plugin;
    private final ProtocolManager protocol;
    private final WorldGuardHook worldGuard;

    private volatile BDTConfig config;

    private PacketListener digListener;
    private int taskId = -1;

    // Simple tick counter (Spigot API doesn't expose Bukkit#getCurrentTick)
    private long tickCounter = 0;

    private final Map<UUID, MiningSession> sessions = new ConcurrentHashMap<>();

    public MiningController(JavaPlugin plugin, BDTConfig config, ProtocolManager protocol, WorldGuardHook worldGuard) {
        this.plugin = plugin;
        this.config = config;
        this.protocol = protocol;
        this.worldGuard = worldGuard;
    }

    public void reload(BDTConfig newConfig) {
        this.config = newConfig;
        this.worldGuard.reload(newConfig);
    }

    public void enable() {
        digListener = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!config.miningEnabled() || !config.interceptBreak()) return;

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
        for (UUID uuid : sessions.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            MiningSession s = sessions.get(uuid);
            if (p != null && s != null) sendCrackStage(p, s.block, -1);
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
        if (s != null && config.clearCracksOnAbort()) {
            sendCrackStage(player, s.block, -1);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!config.miningEnabled() || !config.interceptBreak()) return;

        Player player = event.getPlayer();
        MiningSession s = sessions.get(player.getUniqueId());
        if (s == null) return;

        if (!sameBlock(event.getBlock(), s.block)) return;

        if (event.isCancelled()) {
            stop(player);
            return;
        }

        long now = System.currentTimeMillis();
        if (s.vanillaCompleteMs < 0) {
            s.vanillaCompleteMs = now;

            String worldName = s.block.getWorld().getName();
            Material mat = s.block.getType();
            double mult = config.effectiveHardnessMultiplier(worldName, mat);

            if (mult <= 1.0) {
                sessions.remove(player.getUniqueId());
                sendCrackStage(player, s.block, -1);
                return;
            }

            long vanillaDuration = Math.max(1, s.vanillaCompleteMs - s.startMs);
            long targetDuration = (long) Math.ceil(vanillaDuration * mult);
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
            sendCrackStage(player, s.block, -1);

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
        if (!config.miningEnabled() || !config.interceptBreak()) return;

        tickCounter++;

        int tickRate = config.animationTickRate();
        if (tickCounter % tickRate != 0) return;

        long now = System.currentTimeMillis();

        for (var entry : sessions.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;

            MiningSession s = entry.getValue();
            if (s == null || !s.active) continue;

            if (s.block.getType().isAir()) {
                sendCrackStage(player, s.block, -1);
                sessions.remove(player.getUniqueId());
                continue;
            }

            if (s.targetBreakMs <= 0) continue;

            long total = Math.max(1, s.targetBreakMs - s.startMs);
            long elapsed = Math.max(0, now - s.startMs);
            double pct = Math.max(0.0, Math.min(1.0, (double) elapsed / (double) total));

            int stage = (int) Math.floor(pct * 10.0);
            if (stage >= 10) stage = 9;

            sendCrackStage(player, s.block, stage);

            if (now >= s.targetBreakMs) {
                sessions.remove(player.getUniqueId());
                sendCrackStage(player, s.block, -1);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (s.block.getType().isAir()) return;
                    if (!worldGuard.allowed(player, s.block.getLocation())) return;

                    BreakUtil.breakAsPlayer(player, s.block);
                });
            }
        }
    }

    private void sendCrackStage(Player sourcePlayer, Block block, int stage) {
        if (block == null) return;

        int radius = config.animationRadius();
        int breakerId = sourcePlayer.getEntityId();

        for (Player viewer : sourcePlayer.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(block.getLocation().add(0.5, 0.5, 0.5)) > (radius * radius)) continue;

            PacketContainer packet = protocol.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
            packet.getIntegers().write(0, breakerId);
            packet.getBlockPositionModifier().write(0, new BlockPosition(block.getX(), block.getY(), block.getZ()));
            packet.getIntegers().write(1, stage);

            try {
                protocol.sendServerPacket(viewer, packet);
            } catch (Exception ignored) {}
        }
    }

    private boolean sameBlock(Block a, Block b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.getWorld() != b.getWorld()) return false;
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ();
    }
}
