package com.sorro.blockdurabilitytweaks;

import com.comphenix.protocol.ProtocolLibrary;
import com.sorro.blockdurabilitytweaks.cmd.BlockDurabilityCommand;
import com.sorro.blockdurabilitytweaks.config.MainConfig;
import com.sorro.blockdurabilitytweaks.config.ProfileManager;
import com.sorro.blockdurabilitytweaks.explosion.ExplosionHandler;
import com.sorro.blockdurabilitytweaks.hook.WorldGuardHook;
import com.sorro.blockdurabilitytweaks.mining.MiningController;
import com.sorro.blockdurabilitytweaks.util.GeneratedReadme;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockDurabilityTweaks extends JavaPlugin {

    private MainConfig mainCfg;
    private ProfileManager profiles;
    private MiningController miningController;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadAll();

        // Generate a human-readable help file into the plugin folder.
        GeneratedReadme.ensureGenerated(this);

        BlockDurabilityCommand cmd = new BlockDurabilityCommand(this);
        getCommand("blockdurability").setExecutor(cmd);
        getCommand("blockdurability").setTabCompleter(cmd);

        if (mainCfg.explosionsEnabled()) {
            Bukkit.getPluginManager().registerEvents(new ExplosionHandler(this), this);
        }

        if (mainCfg.miningEnabled()) {
            if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
                getLogger().warning("Mining enabled, but ProtocolLib not found. Mining controller disabled.");
            } else {
                WorldGuardHook wg = new WorldGuardHook(this, mainCfg);
                miningController = new MiningController(this, mainCfg, profiles, ProtocolLibrary.getProtocolManager(), wg);
                miningController.enable();
                Bukkit.getPluginManager().registerEvents(miningController, this);
            }
        }

        getLogger().info("BlockDurabilityTweaks enabled.");
    }

    @Override
    public void onDisable() {
        if (miningController != null) miningController.disable();
    }

    public void reloadAll() {
        reloadConfig();
        this.mainCfg = MainConfig.from(getConfig());
        this.profiles = new ProfileManager(this, mainCfg);
        this.profiles.ensureAllLoaded();
        if (miningController != null) miningController.reload(mainCfg, profiles);
    }

    public MainConfig mainConfig() { return mainCfg; }
    public ProfileManager profiles() { return profiles; }
}
