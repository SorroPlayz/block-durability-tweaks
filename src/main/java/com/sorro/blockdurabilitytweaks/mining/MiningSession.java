package com.sorro.blockdurabilitytweaks.mining;

import org.bukkit.block.Block;

public class MiningSession {
    public final Block block;
    public final long startMs;

    public volatile boolean active = true;
    public volatile long vanillaCompleteMs = -1;
    public volatile long targetBreakMs = -1;

    public MiningSession(Block block, long startMs) {
        this.block = block;
        this.startMs = startMs;
    }
}
