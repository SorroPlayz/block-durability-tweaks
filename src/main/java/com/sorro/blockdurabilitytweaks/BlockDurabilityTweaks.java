package com.sorro.blockdurabilitytweaks;

import com.comphenix.protocol.ProtocolLibrary;
import com.sorro.blockdurabilitytweaks.cmd.BlockDurabilityCommand;
import com.sorro.blockdurabilitytweaks.config.MainConfig;
import com.sorro.blockdurabilitytweaks.config.ProfileManager;
import com.sorro.blockdurabilitytweaks.events.EventScheduler;
import com.sorro.blockdurabilitytweaks.explosion.ExplosionHandler;
import com.sorro.blockdurabilitytweaks.gui.ProfileGui;
import com.sorro.blockdurabilitytweaks.hook.WorldGuardHook;
import com.sorro.blockdurabilitytweaks.hp.DamageStore;
import com.sorro.blockdurabilitytweaks.mining.MiningController;
import com.sorro.blockdurabilitytweaks.util.GeneratedReadme;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockDurabilityTweaks extends JavaPlugin {

    private MainConfig mainCfg;
    private ProfileManager profiles;
    private MiningController miningController;
    private ProfileGui profileGui;
    private EventScheduler eventScheduler;
    private DamageStore damageStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadAll();

        GeneratedReadme.write(this);

        BlockDurabilityCommand cmd = new BlockDurabilityCommand(this);
        getCommand("blockdurability").setExecutor(cmd);
        getCommand("blockdurability").setTabCompleter(cmd);

        if (mainCfg.guiEnabled()) {
            profileGui = new ProfileGui(this);
            Bukkit.getPluginManager().registerEvents(profileGui, this);
        }

        if (mainCfg.eventsEnabled()) {
            eventScheduler = new EventScheduler(this, mainCfg, profiles);
            eventScheduler.start();
        }

        if (mainCfg.explosionsEnabled()) {
            Bukkit.getPluginManager().registerEvents(new ExplosionHandler(this), this);
        }

        if (mainCfg.miningEnabled()) {
            if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
                getLogger().warning("Mining enabled, but ProtocolLib not found. Mining controller disabled.");
            } else {
                WorldGuardHook wg = new WorldGuardHook(this, mainCfg);
                miningController = new MiningController(this, mainCfg, profiles, damageStore, ProtocolLibrary.getProtocolManager(), wg);
                miningController.enable();
                Bukkit.getPluginManager().registerEvents(miningController, this);
            }
        }

        getLogger().info("BlockDurabilityTweaks enabled.");
    }

    @Override
    public void onDisable() {
        if (miningController != null) miningController.disable();
        if (eventScheduler != null) eventScheduler.stop();
        if (damageStore != null && mainCfg.damageMemoryEnabled() && mainCfg.damageMemoryConfig().saveToDisk()) {
            for (var w : Bukkit.getWorlds()) damageStore.saveWorldToDisk(w.getName());
        }
    }

    public void reloadAll() {
        reloadConfig();
        this.mainCfg = MainConfig.from(getConfig());
        this.profiles = new ProfileManager(this, mainCfg);
        this.profiles.ensureAllLoaded();

        this.damageStore = new DamageStore(this, mainCfg);
        this.damageStore.loadAllFromDisk();

        if (eventScheduler != null) eventScheduler.reload(mainCfg, profiles);
        if (miningController != null) miningController.reload(mainCfg, profiles, damageStore);
    }

    public MainConfig mainConfig() { return mainCfg; }
    public ProfileManager profiles() { return profiles; }
    public ProfileGui profileGui() { return profileGui; }
    public EventScheduler eventScheduler() { return eventScheduler; }
    public DamageStore damageStore() { return damageStore; }
}
