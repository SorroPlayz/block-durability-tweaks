package com.sorro.blockdurabilitytweaks;

import com.comphenix.protocol.ProtocolLibrary;
import com.sorro.blockdurabilitytweaks.cmd.BlockDurabilityCommand;
import com.sorro.blockdurabilitytweaks.config.BDTConfig;
import com.sorro.blockdurabilitytweaks.explosion.ExplosionHandler;
import com.sorro.blockdurabilitytweaks.hook.WorldGuardHook;
import com.sorro.blockdurabilitytweaks.mining.MiningController;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockDurabilityTweaks extends JavaPlugin {

    private BDTConfig cfg;
    private MiningController miningController;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadBDTConfig();

        var cmd = new BlockDurabilityCommand(this);
        getCommand("blockdurability").setExecutor(cmd);
        getCommand("blockdurability").setTabCompleter(cmd);

        if (cfg.explosionsEnabled()) {
            Bukkit.getPluginManager().registerEvents(new ExplosionHandler(cfg), this);
        }

        if (cfg.miningEnabled()) {
            if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
                getLogger().warning("Mining controller enabled, but ProtocolLib not found. Disabling mining controller.");
            } else {
                WorldGuardHook wg = new WorldGuardHook(this, cfg);
                miningController = new MiningController(this, cfg, ProtocolLibrary.getProtocolManager(), wg);
                miningController.enable();
                Bukkit.getPluginManager().registerEvents(miningController, this);
            }
        }

        getLogger().info("BlockDurabilityTweaks enabled.");
    }

    @Override
    public void onDisable() {
        if (miningController != null) {
            miningController.disable();
        }
    }

    public void reloadBDTConfig() {
        reloadConfig();
        this.cfg = BDTConfig.from(getConfig(), getLogger());
        if (miningController != null) {
            miningController.reload(this.cfg);
        }
    }

    public BDTConfig getBDTConfig() {
        return cfg;
    }
}
