package com.sorro.blockdurabilitytweaks.cmd;

import com.sorro.blockdurabilitytweaks.BlockDurabilityTweaks;
import com.sorro.blockdurabilitytweaks.events.EventScheduler;
import com.sorro.blockdurabilitytweaks.gui.ProfileGui;
import com.sorro.blockdurabilitytweaks.config.ProfileManager;
import com.sorro.blockdurabilitytweaks.config.WorldProfile;
import com.sorro.blockdurabilitytweaks.util.VanillaStats;
import com.sorro.blockdurabilitytweaks.util.VanillaStatsUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class BlockDurabilityCommand implements CommandExecutor, TabCompleter {

    private final BlockDurabilityTweaks plugin;

    public BlockDurabilityCommand(BlockDurabilityTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission("blockdurabilitytweaks.admin")) return noPerm(sender);
                plugin.reloadAll();
                sender.sendMessage(ChatColor.GREEN + "BlockDurabilityTweaks reloaded.");
                return true;
            }
            case "gui" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "In-game only."); return true; }
                ProfileGui gui = plugin.profileGui();
                if (gui == null) { sender.sendMessage(ChatColor.RED + "GUI disabled."); return true; }
                gui.open(p);
                return true;
            }
            case "events" -> {
                if (!sender.hasPermission("blockdurabilitytweaks.events")) return noPerm(sender);
                EventScheduler es = plugin.eventScheduler();
                if (es == null) { sender.sendMessage(ChatColor.RED + "Events disabled."); return true; }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability events <list|run|stop> ...");
                    return true;
                }
                switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "list" -> {
                        sender.sendMessage(ChatColor.AQUA + "Schedules: " + String.join(", ", es.scheduleNames()));
                        return true;
                    }
                    case "run" -> {
                        if (args.length < 3) { sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability events run <name>"); return true; }
                        es.forceRun(args[2]);
                        sender.sendMessage(ChatColor.GREEN + "Forced run: " + args[2]);
                        return true;
                    }
                    case "stop" -> {
                        if (args.length < 3) { sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability events stop <name>"); return true; }
                        es.forceStop(args[2]);
                        sender.sendMessage(ChatColor.GREEN + "Forced stop: " + args[2]);
                        return true;
                    }
                }
                sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability events <list|run|stop> ...");
                return true;
            }
            case "info" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
                    return true;
                }
                Block target = p.getTargetBlockExact(6);
                if (target == null || target.getType().isAir()) {
                    sender.sendMessage(ChatColor.RED + "Look at a block (within 6 blocks) and try again.");
                    return true;
                }
                String world = target.getWorld().getName();
                Material mat = target.getType();

                WorldProfile prof = plugin.profiles().getActive(world);
                double hard = prof.hardnessFor(mat);
                double blast = prof.blastFor(mat);

                VanillaStats v = VanillaStatsUtil.getVanillaStats(mat);

                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "BlockDurabilityTweaks Info");
                sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.WHITE + world + ChatColor.DARK_GRAY + " (profile: " + prof.profileName + ")");
                sender.sendMessage(ChatColor.GRAY + "Mode: " + ChatColor.WHITE + plugin.mainConfig().mode());
                sender.sendMessage(ChatColor.GRAY + "Block: " + ChatColor.WHITE + mat.name());
                sender.sendMessage(ChatColor.GRAY + "Vanilla hardness: " + ChatColor.WHITE + v.hardness());
                sender.sendMessage(ChatColor.GRAY + "Vanilla blast: " + ChatColor.WHITE + v.blastResistance());
                sender.sendMessage(ChatColor.GRAY + "Profile hardness: " + ChatColor.WHITE + hard);
                sender.sendMessage(ChatColor.GRAY + "Profile blast: " + ChatColor.WHITE + blast);

                sender.sendMessage(ChatColor.GRAY + "Per-tool: " + plugin.mainConfig().perToolMultipliersEnabled()
                        + ChatColor.DARK_GRAY + " | Per-biome: " + plugin.mainConfig().perBiomeEnabled()
                        + ChatColor.DARK_GRAY + " | GUI: " + plugin.mainConfig().guiEnabled()
                        + ChatColor.DARK_GRAY + " | Events: " + plugin.mainConfig().eventsEnabled()
                        + ChatColor.DARK_GRAY + " | HP: " + plugin.mainConfig().hpEnabled()
                        + ChatColor.DARK_GRAY + " | Memory: " + plugin.mainConfig().damageMemoryEnabled());
                return true;
            }
            case "world" -> {
                if (!sender.hasPermission("blockdurabilitytweaks.admin")) return noPerm(sender);
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability world <list|load|save|reset|vanilla> ...");
                    return true;
                }

                ProfileManager pm = plugin.profiles();

                switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "list" -> {
                        String world = (args.length >= 3) ? args[2] : (sender instanceof Player p ? p.getWorld().getName() : "world");
                        Set<String> profiles = pm.listProfiles(world);
                        sender.sendMessage(ChatColor.AQUA + "Profiles for " + world + ": " + ChatColor.WHITE + String.join(", ", profiles));
                        return true;
                    }
                    case "load" -> {
                        if (args.length < 4) { sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability world load <world> <profile>"); return true; }
                        String world = args[2];
                        String profile = args[3];
                        if (!pm.loadProfile(world, profile)) { sender.sendMessage(ChatColor.RED + "Profile not found: " + profile); return true; }
                        sender.sendMessage(ChatColor.GREEN + "Loaded profile '" + profile + "' for world '" + world + "'.");
                        return true;
                    }
                    case "save" -> {
                        if (args.length < 4) { sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability world save <world> <profile>"); return true; }
                        String world = args[2];
                        String profile = args[3];
                        if (!pm.saveProfileFromActive(world, profile)) { sender.sendMessage(ChatColor.RED + "Failed to save profile."); return true; }
                        sender.sendMessage(ChatColor.GREEN + "Saved profile '" + profile + "' for world '" + world + "'.");
                        return true;
                    }
                    case "reset" -> {
                        if (args.length < 3) { sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability world reset <world>"); return true; }
                        String world = args[2];
                        pm.resetToVanilla(world);
                        sender.sendMessage(ChatColor.GREEN + "World '" + world + "' reset to vanilla profile.");
                        return true;
                    }
                    case "vanilla" -> {
                        if (args.length < 3) { sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability world vanilla <world>"); return true; }
                        String world = args[2];
                        boolean ok = pm.regenerateVanilla(world);
                        sender.sendMessage(ok ? ChatColor.GREEN + "Regenerated vanilla.yml for " + world : ChatColor.RED + "Failed to regenerate vanilla.yml for " + world);
                        return true;
                    }
                }

                sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability world <list|load|save|reset|vanilla> ...");
                return true;
            }
            default -> {
                help(sender);
                return true;
            }
        }
    }

    private void help(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "BlockDurabilityTweaks");
        sender.sendMessage(ChatColor.GRAY + "/blockdurability info");
        sender.sendMessage(ChatColor.GRAY + "/blockdurability reload");
        sender.sendMessage(ChatColor.GRAY + "/blockdurability gui");
        sender.sendMessage(ChatColor.GRAY + "/blockdurability world ...");
        sender.sendMessage(ChatColor.GRAY + "/blockdurability events ...");
    }

    private boolean noPerm(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "No permission.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            for (String s : List.of("info","reload","gui","world","events")) {
                if (s.startsWith(p)) out.add(s);
            }
            return out;
        }

        if (args[0].equalsIgnoreCase("events") && sender.hasPermission("blockdurabilitytweaks.events")) {
            if (args.length == 2) {
                String p = args[1].toLowerCase(Locale.ROOT);
                for (String s : List.of("list","run","stop")) if (s.startsWith(p)) out.add(s);
                return out;
            }
            if (args.length == 3 && (args[1].equalsIgnoreCase("run") || args[1].equalsIgnoreCase("stop"))) {
                var es = plugin.eventScheduler();
                if (es != null) {
                    String p = args[2].toLowerCase(Locale.ROOT);
                    for (String n : es.scheduleNames()) if (n.toLowerCase(Locale.ROOT).startsWith(p)) out.add(n);
                }
                return out;
            }
        }

        // world subcommand completion kept simple
        if (args[0].equalsIgnoreCase("world") && sender.hasPermission("blockdurabilitytweaks.admin")) {
            if (args.length == 2) {
                String p = args[1].toLowerCase(Locale.ROOT);
                for (String s : List.of("list","load","save","reset","vanilla")) if (s.startsWith(p)) out.add(s);
                return out;
            }
            if (args.length == 3) {
                String p = args[2].toLowerCase(Locale.ROOT);
                for (var w : Bukkit.getWorlds()) if (w.getName().toLowerCase(Locale.ROOT).startsWith(p)) out.add(w.getName());
                return out;
            }
            if (args.length == 4 && (args[1].equalsIgnoreCase("load") || args[1].equalsIgnoreCase("save"))) {
                String world = args[2];
                String p = args[3].toLowerCase(Locale.ROOT);
                for (String prof : plugin.profiles().listProfiles(world)) if (prof.toLowerCase(Locale.ROOT).startsWith(p)) out.add(prof);
                return out;
            }
        }
        return out;
    }
}
