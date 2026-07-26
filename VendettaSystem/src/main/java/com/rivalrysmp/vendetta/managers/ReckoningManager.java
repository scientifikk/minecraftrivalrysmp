package com.rivalrysmp.vendetta.managers;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Weekly event: pulls any players flagged as unresolved rivals (tracked via
 * BloodDebtManager curses) into a shared arena world, locks them in, and
 * lets spectators bet money on the outcome.
 */
public class ReckoningManager {

    private final JavaPlugin plugin;
    private final VaultManager vault;
    private boolean eventActive = false;
    private final List<UUID> combatants = new ArrayList<>();
    // bettorUUID -> [pickedPlayerUUID, amount]
    private final Map<UUID, Object[]> bets = new HashMap<>();

    public ReckoningManager(JavaPlugin plugin, VaultManager vault) {
        this.plugin = plugin;
        this.vault = vault;
    }

    public void scheduleWeeklyCheck() {
        // Check every minute whether it's time to trigger; simple + reliable across restarts.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            LocalDateTime now = LocalDateTime.now();
            DayOfWeek targetDay = DayOfWeek.valueOf(
                    plugin.getConfig().getString("reckoning.day", "SUNDAY").toUpperCase());
            int targetHour = plugin.getConfig().getInt("reckoning.hour", 20);

            if (!eventActive && now.getDayOfWeek() == targetDay
                    && now.getHour() == targetHour && now.getMinute() == 0) {
                triggerReckoning();
            }
        }, 20L * 30, 20L * 60);
    }

    public void triggerReckoning() {
        String worldName = plugin.getConfig().getString("reckoning.arena-world", "reckoning_arena");
        World arena = Bukkit.getWorld(worldName);
        if (arena == null) {
            plugin.getLogger().warning("Reckoning arena world '" + worldName + "' not found. Skipping event.");
            return;
        }

        // In a full build, unresolved rivals are pulled from BloodDebtManager's
        // curse records. Placeholder: server admins can also force-add via command.
        if (combatants.size() < 2) {
            plugin.getLogger().info("Not enough flagged rivals for The Reckoning this week.");
            return;
        }

        eventActive = true;
        int warning = plugin.getConfig().getInt("reckoning.warning-seconds", 60);
        Bukkit.broadcast(net.kyori.adventure.text.Component.text(
                "§4§l⚔ THE RECKONING ⚔ §7- Rivals will be pulled into the arena in " + warning + " seconds. Place your bets!"));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID uuid : combatants) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.teleport(arena.getSpawnLocation());
            }
            Bukkit.broadcast(net.kyori.adventure.text.Component.text(
                    "§4§lTHE RECKONING HAS BEGUN §7- No escape until one rival falls."));
        }, warning * 20L);
    }

    public void addCombatant(UUID uuid) {
        if (!combatants.contains(uuid)) combatants.add(uuid);
    }

    public boolean isEventActive() {
        return eventActive;
    }

    public boolean placeBet(Player bettor, Player onPlayer, double amount) {
        if (!eventActive || !vault.isEnabled()) return false;
        if (!vault.getEconomy().has(bettor, amount)) return false;
        vault.getEconomy().withdrawPlayer(bettor, amount);
        bets.put(bettor.getUniqueId(), new Object[]{onPlayer.getUniqueId(), amount});
        bettor.sendMessage("§6Bet placed: $" + amount + " on " + onPlayer.getName());
        return true;
    }

    /** Call when the arena fight resolves (one combatant dies), to pay out bettors and reset. */
    public void resolveReckoning(UUID winnerUUID) {
        double pot = bets.values().stream().mapToDouble(o -> (double) o[1]).sum();
        List<UUID> winningBettors = new ArrayList<>();
        for (Map.Entry<UUID, Object[]> entry : bets.entrySet()) {
            if (entry.getValue()[0].equals(winnerUUID)) winningBettors.add(entry.getKey());
        }
        if (!winningBettors.isEmpty() && vault.isEnabled()) {
            double share = pot / winningBettors.size();
            for (UUID bettorId : winningBettors) {
                Player bettor = Bukkit.getPlayer(bettorId);
                if (bettor != null) vault.getEconomy().depositPlayer(bettor, share);
            }
        }
        Player winner = Bukkit.getPlayer(winnerUUID);
        Bukkit.broadcast(net.kyori.adventure.text.Component.text(
                "§4§lTHE RECKONING IS OVER §7- " + (winner != null ? winner.getName() : "A rival") + " stands victorious."));

        bets.clear();
        combatants.clear();
        eventActive = false;
    }
}
