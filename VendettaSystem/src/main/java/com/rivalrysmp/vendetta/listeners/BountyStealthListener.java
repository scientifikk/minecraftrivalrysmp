package com.rivalrysmp.vendetta.listeners;

import com.rivalrysmp.vendetta.managers.BountyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BountyStealthListener implements Listener {

    private final JavaPlugin plugin;
    private final BountyManager bountyManager;

    public BountyStealthListener(JavaPlugin plugin, BountyManager bountyManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
    }

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player hunter = event.getPlayer();
        if (!event.isSneaking()) {
            bountyManager.cancelStalk(hunter.getUniqueId());
            return;
        }

        // Check nearby players for anyone with an active bounty.
        int radius = plugin.getConfig().getInt("bounty.confirm-radius", 5);
        for (Player nearby : hunter.getWorld().getPlayers()) {
            if (nearby.equals(hunter)) continue;
            if (bountyManager.getBounty(nearby.getUniqueId()) <= 0) continue;
            if (hunter.getLocation().distance(nearby.getLocation()) <= radius) {
                bountyManager.tryStartStalk(hunter, nearby);
                break;
            }
        }
    }
}
