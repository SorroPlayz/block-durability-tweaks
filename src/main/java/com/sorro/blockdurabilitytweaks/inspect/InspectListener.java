package com.sorro.blockdurabilitytweaks.inspect;

import com.sorro.blockdurabilitytweaks.BlockDurabilityTweaks;
import com.sorro.blockdurabilitytweaks.config.WorldProfile;
import com.sorro.blockdurabilitytweaks.hp.DamageStore;
import com.sorro.blockdurabilitytweaks.util.BlockKey;
import com.sorro.blockdurabilitytweaks.util.VanillaStats;
import com.sorro.blockdurabilitytweaks.util.VanillaStatsUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class InspectListener implements Listener {

    private final BlockDurabilityTweaks plugin;

    public InspectListener(BlockDurabilityTweaks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        if (!p.hasPermission("blockdurabilitytweaks.inspect")) return;
        if (!InspectWand.isWand(plugin, p.getInventory().getItemInMainHand())) return;

        Block b = e.getClickedBlock();
        if (b == null) return;

        e.setCancelled(true);

        String world = b.getWorld().getName();
        Material mat = b.getType();
        WorldProfile prof = plugin.profiles().getActive(world);

        VanillaStats v = VanillaStatsUtil.getVanillaStats(mat);

        p.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "BD Inspect");
        p.sendMessage(ChatColor.GRAY + "World: " + ChatColor.WHITE + world);
        p.sendMessage(ChatColor.GRAY + "Block: " + ChatColor.WHITE + mat.name()
                + ChatColor.DARK_GRAY + " @ " + b.getX() + "," + b.getY() + "," + b.getZ());
        p.sendMessage(ChatColor.GRAY + "Vanilla: " + ChatColor.WHITE + "hard=" + v.hardness() + " blast=" + v.blastResistance());
        p.sendMessage(ChatColor.GRAY + "Profile: " + ChatColor.WHITE + prof.profileName
                + ChatColor.DARK_GRAY + " hardMult=" + prof.hardnessFor(mat) + " blastMult=" + prof.blastFor(mat));

        if (plugin.mainConfig().hpEnabled()) {
            DamageStore ds = plugin.damageStore();
            var key = BlockKey.of(b.getWorld(), b.getX(), b.getY(), b.getZ());
            DamageStore.DamageState st = ds.get(key);
            if (st != null) {
                p.sendMessage(ChatColor.GRAY + "HP: " + ChatColor.WHITE + st.hp + "/" + st.maxHp);
            } else {
                p.sendMessage(ChatColor.GRAY + "HP: " + ChatColor.DARK_GRAY + "no stored damage");
            }
        }
    }
}
