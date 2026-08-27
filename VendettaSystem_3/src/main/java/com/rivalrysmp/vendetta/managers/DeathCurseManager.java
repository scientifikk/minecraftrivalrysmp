package com.rivalrysmp.vendetta.managers;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Tracks each player's consecutive PvP-death streak (resets on a PvP kill).
 * Escalating tiers apply real, non-money punishment: a slowness debuff,
 * then a heavier debuff, then a full temporary exile with a strong debuff
 * for the exile's duration.
 */
public class DeathCurseManager {

    private final JavaPlugin plugin;
    private final NamespacedKey streakKey;
    private final NamespacedKey exiledUntilKey;

    public DeathCurseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.streakKey = new NamespacedKey(plugin, "death_streak");
        this.exiledUntilKey = new NamespacedKey(plugin, "exiled_until");
    }

    public int getStreak(Player player) {
        Integer streak = player.getPersistentDataContainer().get(streakKey, PersistentDataType.INTEGER);
        return streak == null ? 0 : streak;
    }

    public boolean isExiled(Player player) {
        Long until = player.getPersistentDataContainer().get(exiledUntilKey, PersistentDataType.LONG);
        return until != null && until > System.currentTimeMillis();
    }

    public long getExileRemainingSeconds(Player player) {
        Long until = player.getPersistentDataContainer().get(exiledUntilKey, PersistentDataType.LONG);
        if (until == null) return 0;
        long remaining = (until - System.currentTimeMillis()) / 1000L;
        return Math.max(remaining, 0);
    }

    /** Call when killer beats victim in PvP. Handles both the winner's streak reset and the loser's escalation. */
    public void handlePvpResult(Player killer, Player victim) {
        // Winning clears your own bad streak.
        killer.getPersistentDataContainer().set(streakKey, PersistentDataType.INTEGER, 0);

        int streak = getStreak(victim) + 1;
        victim.getPersistentDataContainer().set(streakKey, PersistentDataType.INTEGER, streak);

        int t1 = plugin.getConfig().getInt("death-curses.tier-1-streak", 2);
        int t2 = plugin.getConfig().getInt("death-curses.tier-2-streak", 3);
        int t3 = plugin.getConfig().getInt("death-curses.tier-3-streak", 4);

        if (streak >= t3) {
            applyTier3Exile(victim);
        } else if (streak >= t2) {
            applyTier2Debuff(victim);
        } else if (streak >= t1) {
            applyTier1Slowness(victim);
        }
    }

    private void applyTier1Slowness(Player victim) {
        int seconds = plugin.getConfig().getInt("death-curses.tier-1-slowness-seconds", 60);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, seconds * 20, 0));
        victim.sendMessage("§e§lDEATH CURSE §7- Tier 1: You're shaken. Slowness applied. Win a fight to clear your streak.");
    }

    private void applyTier2Debuff(Player victim) {
        int seconds = plugin.getConfig().getInt("death-curses.tier-2-debuff-seconds", 90);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, seconds * 20, 1));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, seconds * 20, 0));
        victim.sendMessage("§6§lDEATH CURSE §7- Tier 2: Your losing streak is getting dangerous.");
    }

    private void applyTier3Exile(Player victim) {
        int durationSeconds = plugin.getConfig().getInt("death-curses.tier-3-exile-seconds", 600);
        long until = System.currentTimeMillis() + (durationSeconds * 1000L);

        victim.getPersistentDataContainer().set(exiledUntilKey, PersistentDataType.LONG, until);
        victim.getPersistentDataContainer().set(streakKey, PersistentDataType.INTEGER, 0);

        int durationTicks = durationSeconds * 20;
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 1));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, durationTicks, 1));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, durationTicks, 1));

        victim.teleport(victim.getWorld().getSpawnLocation());
        Bukkit.broadcast(net.kyori.adventure.text.Component.text(
                "§4§lEXILED §7- " + victim.getName() + "'s losing streak has caught up with them. Exiled and weakened for " + durationSeconds + "s."));
    }
}
