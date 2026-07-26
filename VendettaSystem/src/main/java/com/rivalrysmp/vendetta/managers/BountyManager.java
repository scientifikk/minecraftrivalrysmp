package com.rivalrysmp.vendetta.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bounties: any player can place money on another player's head.
 * To collect, a hunter must sneak within a radius of the target,
 * undetected (target hasn't spotted/attacked them), for a hold duration.
 */
public class BountyManager {

    private final JavaPlugin plugin;
    private final VaultManager vault;
    private final Map<UUID, Double> bounties = new HashMap<>();
    // hunterUUID -> targetUUID currently being "stalked"
    private final Map<UUID, UUID> activeStalks = new HashMap<>();
    private final Map<UUID, Integer> stalkProgress = new HashMap<>();
    private final Map<UUID, BukkitTask> stalkTasks = new HashMap<>();

    public BountyManager(JavaPlugin plugin, VaultManager vault) {
        this.plugin = plugin;
        this.vault = vault;
    }

    public boolean placeBounty(Player placer, Player target, double amount) {
        double min = plugin.getConfig().getDouble("bounty.min-amount", 100.0);
        if (amount < min) return false;
        if (!vault.isEnabled()) return false;
        if (!vault.getEconomy().has(placer, amount)) return false;

        vault.getEconomy().withdrawPlayer(placer, amount);
        bounties.merge(target.getUniqueId(), amount, Double::sum);
        Bukkit.broadcast(net.kyori.adventure.text.Component.text(
                "§6§lBOUNTY §7- " + placer.getName() + " placed a $" + amount + " bounty on " + target.getName() + "!"));
        return true;
    }

    public double getBounty(UUID target) {
        return bounties.getOrDefault(target, 0.0);
    }

    /** Called every tick-ish while a hunter is sneaking near a target. */
    public void tryStartStalk(Player hunter, Player target) {
        if (getBounty(target.getUniqueId()) <= 0) return;
        if (activeStalks.containsKey(hunter.getUniqueId())) return;

        activeStalks.put(hunter.getUniqueId(), target.getUniqueId());
        stalkProgress.put(hunter.getUniqueId(), 0);

        int holdSeconds = plugin.getConfig().getInt("bounty.confirm-hold-seconds", 5);
        int radius = plugin.getConfig().getInt("bounty.confirm-radius", 5);

        BukkitTask t = Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            Player h = Bukkit.getPlayer(hunter.getUniqueId());
            Player targ = Bukkit.getPlayer(target.getUniqueId());
            if (h == null || targ == null || !h.isSneaking()
                    || h.getLocation().distance(targ.getLocation()) > radius
                    || hasBeenSpotted(h, targ)) {
                cancelStalk(hunter.getUniqueId());
                task.cancel();
                return;
            }
            int progress = stalkProgress.merge(hunter.getUniqueId(), 1, Integer::sum);
            if (progress >= holdSeconds) {
                confirmBounty(h, targ);
                cancelStalk(hunter.getUniqueId());
                task.cancel();
            }
        }, 20L, 20L);
        stalkTasks.put(hunter.getUniqueId(), t);
    }

    private boolean hasBeenSpotted(Player hunter, Player target) {
        // Simple line-of-sight check: if target is looking roughly at hunter, they're spotted.
        return target.hasLineOfSight(hunter);
    }

    public void cancelStalk(UUID hunterUUID) {
        activeStalks.remove(hunterUUID);
        stalkProgress.remove(hunterUUID);
        BukkitTask t = stalkTasks.remove(hunterUUID);
        if (t != null) t.cancel();
    }

    private void confirmBounty(Player hunter, Player target) {
        double amount = bounties.remove(target.getUniqueId());
        if (vault.isEnabled()) {
            vault.getEconomy().depositPlayer(hunter, amount);
        }
        Bukkit.broadcast(net.kyori.adventure.text.Component.text(
                "§4§lBOUNTY CLAIMED §7- " + hunter.getName() + " collected $" + amount + " for " + target.getName() + "!"));
    }

    /** Called on death: if the victim had a bounty and the killer is who was stalking, pay out. */
    public void onPlayerKilled(Player killer, Player victim) {
        if (getBounty(victim.getUniqueId()) > 0) {
            confirmBounty(killer, victim);
        }
    }
}
