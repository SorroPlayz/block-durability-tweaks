package com.sorro.blockdurabilitytweaks.mining;

import org.bukkit.block.Block;

public class MiningSession {
    public final Block block;
    public final long startMs;

    public volatile boolean active = true;

    // When vanilla break would have completed (measured when BlockBreakEvent first fires)
    public volatile long vanillaCompleteMs = -1;

    // When we will actually allow the block to break (after multiplier)
    public volatile long targetBreakMs = -1;

    public MiningSession(Block block, long startMs) {
        this.block = block;
        this.startMs = startMs;
    }
}
