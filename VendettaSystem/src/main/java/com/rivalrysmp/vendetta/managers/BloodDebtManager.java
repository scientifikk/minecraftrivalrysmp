package com.rivalrysmp.vendetta.managers;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks kill counts between each pair of players. When the count against a
 * specific victim reaches a threshold, the loser is cursed until they land
 * a kill back on their killer (revenge kill clears the curse).
 */
public class BloodDebtManager {

    private final JavaPlugin plugin;
    // key: "killerUUID:victimUUID" -> kill count
    private final Map<String, Integer> killCounts = new HashMap<>();
    private final NamespacedKey curseTierKey;
    private final NamespacedKey curseSourceKey; // who the curse is "owed" to

    public BloodDebtManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.curseTierKey = new NamespacedKey(plugin, "curse_tier");
        this.curseSourceKey = new NamespacedKey(plugin, "curse_source");
    }

    private String key(UUID killer, UUID victim) {
        return killer.toString() + ":" + victim.toString();
    }

    /** Call when killer kills victim. Returns true if a curse tier was applied/updated. */
    public boolean registerKill(Player killer, Player victim) {
        String k = key(killer.getUniqueId(), victim.getUniqueId());
        int count = killCounts.merge(k, 1, Integer::sum);

        // If the victim was cursed because of this killer, clear it (revenge kill).
        clearCurseIfOwedTo(victim, killer.getUniqueId());

        int t1 = plugin.getConfig().getInt("blood-debt.tier-1-kills", 2);
        int t2 = plugin.getConfig().getInt("blood-debt.tier-2-kills", 4);
        int t3 = plugin.getConfig().getInt("blood-debt.tier-3-kills", 6);

        int tier = 0;
        if (count >= t3) tier = 3;
        else if (count >= t2) tier = 2;
        else if (count >= t1) tier = 1;

        if (tier > 0) {
            applyCurse(victim, tier, killer.getUniqueId());
            return true;
        }
        return false;
    }

    private void applyCurse(Player victim, int tier, UUID owedTo) {
        PersistentDataContainer pdc = victim.getPersistentDataContainer();
        pdc.set(curseTierKey, PersistentDataType.INTEGER, tier);
        pdc.set(curseSourceKey, PersistentDataType.STRING, owedTo.toString());

        int durationTicks;
        switch (tier) {
            case 1:
                durationTicks = plugin.getConfig().getInt("blood-debt.tier-1-duration-seconds", 300) * 20;
                victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, durationTicks, 0));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, durationTicks, 0));
                victim.sendMessage("§c§lBLOOD DEBT §7- Tier 1: You are weakened until you kill your rival back.");
                break;
            case 2:
                durationTicks = plugin.getConfig().getInt("blood-debt.tier-2-duration-seconds", 300) * 20;
                victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, durationTicks, 1));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, durationTicks, 1));
                victim.sendMessage("§c§lBLOOD DEBT §7- Tier 2: Your rival can now sense your general direction.");
                startCompassTracking(victim, owedTo);
                break;
            case 3:
            default:
                victim.sendMessage("§4§lBLOOD DEBT §7- Tier 3: You are cursed prey. Any player who kills you now gets bonus drops from you.");
                pdc.set(new NamespacedKey(plugin, "cursed_prey"), PersistentDataType.BYTE, (byte) 1);
                break;
        }
    }

    private void startCompassTracking(Player victim, UUID rivalUUID) {
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            Player current = Bukkit.getPlayer(victim.getUniqueId());
            Player rival = Bukkit.getPlayer(rivalUUID);
            if (current == null || rival == null || !current.isOnline() || getCurseTier(current) < 2) {
                task.cancel();
                return;
            }
            rival.sendActionBar(net.kyori.adventure.text.Component.text(
                    "§c" + current.getName() + " is near: " + current.getLocation().getBlockX()
                            + ", " + current.getLocation().getBlockZ()));
        }, 0L, 200L); // every ~10s
    }

    public int getCurseTier(Player player) {
        Integer tier = player.getPersistentDataContainer().get(curseTierKey, PersistentDataType.INTEGER);
        return tier == null ? 0 : tier;
    }

    public boolean isCursedPrey(Player player) {
        Byte flag = player.getPersistentDataContainer().get(new NamespacedKey(plugin, "cursed_prey"), PersistentDataType.BYTE);
        return flag != null && flag == (byte) 1;
    }

    private void clearCurseIfOwedTo(Player victimTurnedKiller, UUID rivalUUID) {
        PersistentDataContainer pdc = victimTurnedKiller.getPersistentDataContainer();
        String owed = pdc.get(curseSourceKey, PersistentDataType.STRING);
        if (owed != null && owed.equals(rivalUUID.toString())) {
            pdc.remove(curseTierKey);
            pdc.remove(curseSourceKey);
            pdc.remove(new NamespacedKey(plugin, "cursed_prey"));
            victimTurnedKiller.sendMessage("§a§lBLOOD DEBT CLEARED §7- Revenge settles the score.");
        }
    }
}
