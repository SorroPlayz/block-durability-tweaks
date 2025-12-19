package com.sorro.blockdurabilitytweaks.cmd;

import com.sorro.blockdurabilitytweaks.BlockDurabilityTweaks;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BlockDurabilityCommand implements CommandExecutor, TabCompleter {

    private final BlockDurabilityTweaks plugin;

    public BlockDurabilityCommand(BlockDurabilityTweaks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability <info|reload>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("blockdurabilitytweaks.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                plugin.reloadBDTConfig();
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

                var cfg = plugin.getBDTConfig();
                Material mat = target.getType();
                String world = target.getWorld().getName();

                double hard = cfg.effectiveHardnessMultiplier(world, mat);
                double blast = cfg.effectiveBlastMultiplier(world, mat);

                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "BlockDurabilityTweaks Info");
                sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.WHITE + world);
                sender.sendMessage(ChatColor.GRAY + "Block: " + ChatColor.WHITE + mat.name());
                sender.sendMessage(ChatColor.GRAY + "Hardness multiplier: " + ChatColor.WHITE + hard + ChatColor.DARK_GRAY + " (only slows if > 1.0)");
                sender.sendMessage(ChatColor.GRAY + "Blast multiplier: " + ChatColor.WHITE + blast);

                boolean wgRespect = cfg.respectWorldGuard();
                boolean wgBypass = p.hasPermission("blockdurabilitytweaks.worldguard.bypass");
                sender.sendMessage(ChatColor.DARK_GRAY + "WorldGuard respect: " + wgRespect + " | WG bypass perm: " + wgBypass);

                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /blockdurability <info|reload>");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            if ("info".startsWith(prefix)) out.add("info");
            if ("reload".startsWith(prefix) && sender.hasPermission("blockdurabilitytweaks.admin")) out.add("reload");
        }
        return out;
    }
}
