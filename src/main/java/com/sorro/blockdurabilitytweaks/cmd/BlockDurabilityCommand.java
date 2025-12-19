package com.sorro.blockdurabilitytweaks.cmd;

import com.sorro.blockdurabilitytweaks.BlockDurabilityTweaks;
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

                VanillaStats v = VanillaStatsUtil.getVanillaStats(mat);

                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "BlockDurabilityTweaks Info");
                sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.WHITE + world + ChatColor.DARK_GRAY + " (profile: " + prof.profileName + ")");
                sender.sendMessage(ChatColor.GRAY + "Block: " + ChatColor.WHITE + mat.name());
                sender.sendMessage(ChatColor.GRAY + "Vanilla hardness: " + ChatColor.WHITE + v.hardness());
                sender.sendMessage(ChatColor.GRAY + "Vanilla blast: " + ChatColor.WHITE + v.blastResistance());
                sender.sendMessage(ChatColor.GRAY + "Effective hardness (profile): " + ChatColor.WHITE + prof.hardnessFor(mat));
                sender.sendMessage(ChatColor.GRAY + "Effective blast (profile): " + ChatColor.WHITE + prof.blastFor(mat));
                sender.sendMessage(ChatColor.DARK_GRAY + "WorldGuard respect: " + plugin.mainConfig().respectWorldGuard());
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
                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability world load <world> <profile>");
                            return true;
                        }
                        String world = args[2];
                        String profile = args[3];
                        if (!pm.loadProfile(world, profile)) {
                            sender.sendMessage(ChatColor.RED + "Profile not found: " + profile + " (world " + world + ")");
                            return true;
                        }

                        // Persist the active profile name into config.yml so it survives restarts.
                        plugin.getConfig().set("profiles.active_per_world." + world, profile);
                        plugin.saveConfig();

                        sender.sendMessage(ChatColor.GREEN + "Loaded profile '" + profile + "' for world '" + world + "'.");
                        return true;
                    }
                    case "save" -> {
                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability world save <world> <profile>");
                            return true;
                        }
                        String world = args[2];
                        String profile = args[3];
                        if (!pm.saveProfileFromActive(world, profile)) {
                            sender.sendMessage(ChatColor.RED + "Failed to save profile.");
                            return true;
                        }
                        sender.sendMessage(ChatColor.GREEN + "Saved profile '" + profile + "' for world '" + world + "'.");
                        return true;
                    }
                    case "reset" -> {
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability world reset <world>");
                            return true;
                        }
                        String world = args[2];
                        pm.resetToVanilla(world);
                        plugin.getConfig().set("profiles.active_per_world." + world, "vanilla");
                        plugin.saveConfig();
                        sender.sendMessage(ChatColor.GREEN + "World '" + world + "' reset to vanilla profile.");
                        return true;
                    }
                    case "vanilla" -> {
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability world vanilla <world>");
                            return true;
                        }
                        String world = args[2];
                        boolean ok = pm.regenerateVanilla(world);
                        sender.sendMessage(ok ? ChatColor.GREEN + "Regenerated vanilla.yml for " + world : ChatColor.RED + "Failed to regenerate vanilla.yml for " + world);
                        return true;
                    }
                    default -> {
                        sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability world <list|load|save|reset|vanilla> ...");
                        return true;
                    }
                }
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
        sender.sendMessage(ChatColor.GRAY + "/blockdurability world list [world]");
        sender.sendMessage(ChatColor.GRAY + "/blockdurability world load <world> <profile>");
        sender.sendMessage(ChatColor.GRAY + "/blockdurability world save <world> <profile>");
        sender.sendMessage(ChatColor.GRAY + "/blockdurability world reset <world>");
        sender.sendMessage(ChatColor.GRAY + "/blockdurability world vanilla <world>");
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
            if ("info".startsWith(p)) out.add("info");
            if ("reload".startsWith(p) && sender.hasPermission("blockdurabilitytweaks.admin")) out.add("reload");
            if ("world".startsWith(p) && sender.hasPermission("blockdurabilitytweaks.admin")) out.add("world");
            return out;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("world") && sender.hasPermission("blockdurabilitytweaks.admin")) {
            if (args.length == 2) {
                String p = args[1].toLowerCase(Locale.ROOT);
                for (String s : List.of("list","load","save","reset","vanilla")) {
                    if (s.startsWith(p)) out.add(s);
                }
                return out;
            }

            if (args.length == 3) {
                String p = args[2].toLowerCase(Locale.ROOT);
                for (var w : Bukkit.getWorlds()) {
                    if (w.getName().toLowerCase(Locale.ROOT).startsWith(p)) out.add(w.getName());
                }
                return out;
            }

            if (args.length == 4) {
                String sub = args[1].toLowerCase(Locale.ROOT);
                String world = args[2];
                if (sub.equals("load") || sub.equals("save")) {
                    String p = args[3].toLowerCase(Locale.ROOT);
                    for (String prof : plugin.profiles().listProfiles(world)) {
                        if (prof.toLowerCase(Locale.ROOT).startsWith(p)) out.add(prof);
                    }
                }
            }
        }
        return out;
    }
}
