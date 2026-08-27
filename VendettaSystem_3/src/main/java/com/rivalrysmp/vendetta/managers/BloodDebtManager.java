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
import java.util.Random;
import java.util.UUID;

/**
 * Blood Debt: the 1st kill against a rival rolls a random curse from Pool A.
 * Every kill after that (2nd, 3rd, 4th...) re-rolls a fresh random curse
 * from the nastier Pool B — there's no fixed "final" curse tied to a
 * specific kill count, since a player's actual life total can vary (extra
 * lives can be crafted), so nothing here assumes they're still able to
 * move freely by any particular kill number. Every repeat kill (2nd+) also
 * always grants the rival a small XP prize, independent of curse state.
 * The curse only clears when the cursed player kills the specific rival
 * it's owed to.
 */
public class BloodDebtManager {

    public enum CurseType {
        // Pool A — 1st kill
        FRAGILE,            // taking damage triggers Blindness
        MARKED_PREY,        // mobs deal 1.5x damage
        WITHERING_HUNGER,   // continuous Hunger
        HEAVY_LIMBS,        // continuous Mining Fatigue
        WEAK_ARM,           // continuous Weakness I
        CLUMSY,             // continuous Slowness I
        DIZZY_STRIKES,      // taking damage triggers a brief Nausea burst
        // Pool B — every kill from the 2nd onward
        CURSED_SIGHT,       // continuous Nausea
        BRITTLE_BONES,      // fall damage doubled
        WEAK_BLOOD,         // continuous Weakness II
        SLOW_BLEED,         // any damage taken also drains hunger
        HUNTED_GLOW,        // periodic glowing, every 60s for 5s
        SLUGGISH,           // continuous Slowness II
        STAGGERED,          // taking damage triggers a brief Slowness burst
        FESTERING_WOUNDS    // taking damage triggers a brief Poison burst
    }

    private static final CurseType[] POOL_A = {
            CurseType.FRAGILE, CurseType.MARKED_PREY, CurseType.WITHERING_HUNGER, CurseType.HEAVY_LIMBS,
            CurseType.WEAK_ARM, CurseType.CLUMSY, CurseType.DIZZY_STRIKES
    };
    private static final CurseType[] POOL_B = {
            CurseType.CURSED_SIGHT, CurseType.BRITTLE_BONES, CurseType.WEAK_BLOOD, CurseType.SLOW_BLEED,
            CurseType.HUNTED_GLOW, CurseType.SLUGGISH, CurseType.STAGGERED, CurseType.FESTERING_WOUNDS
    };

    private final JavaPlugin plugin;
    private final Random random = new Random();

    private final NamespacedKey curseTypeKey;
    private final NamespacedKey curseSourceKey;

    // key: "killerUUID:victimUUID" -> kill count
    private final Map<String, Integer> killCounts = new HashMap<>();
    // per-player last time their Hunted-glow curse pulsed
    private final Map<UUID, Long> lastGlow = new HashMap<>();

    public BloodDebtManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.curseTypeKey = new NamespacedKey(plugin, "curse_type");
        this.curseSourceKey = new NamespacedKey(plugin, "curse_source");
    }

    /** Starts the heartbeat that keeps continuous curse effects topped up. */
    public void startHeartbeat() {
        int glowIntervalSeconds = plugin.getConfig().getInt("blood-debt.hunted-glow-interval-seconds", 60);
        int glowDurationTicks = plugin.getConfig().getInt("blood-debt.hunted-glow-duration-seconds", 5) * 20;

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player p : Bukkit.getOnlinePlayers()) {
                CurseType type = getCurseType(p);
                if (type == null) continue;

                switch (type) {
                    case WITHERING_HUNGER:
                        p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 140, 0, false, false));
                        break;
                    case HEAVY_LIMBS:
                        p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 140, 0, false, false));
                        break;
                    case WEAK_ARM:
                        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 140, 0, false, false));
                        break;
                    case CLUMSY:
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 140, 0, false, false));
                        break;
                    case CURSED_SIGHT:
                        p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 140, 0, false, false));
                        break;
                    case WEAK_BLOOD:
                        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 140, 1, false, false));
                        break;
                    case SLUGGISH:
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 140, 1, false, false));
                        break;
                    case HUNTED_GLOW:
                        long last = lastGlow.getOrDefault(p.getUniqueId(), 0L);
                        if (now - last >= glowIntervalSeconds * 1000L) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowDurationTicks, 0, false, false));
                            lastGlow.put(p.getUniqueId(), now);
                        }
                        break;
                    default:
                        break;
                }
            }
        }, 100L, 100L); // every 5 seconds
    }

    private String key(UUID killer, UUID victim) {
        return killer.toString() + ":" + victim.toString();
    }

    /** Call when killer kills victim. */
    public void registerKill(Player killer, Player victim) {
        // If the killer just avenged themselves on the person who cursed them, clear the killer's own curse.
        clearCurseIfOwedTo(killer, victim.getUniqueId());

        String k = key(killer.getUniqueId(), victim.getUniqueId());
        int count = killCounts.merge(k, 1, Integer::sum);

        if (count == 1) {
            applyCurse(victim, killer.getUniqueId(), POOL_A[random.nextInt(POOL_A.length)]);
            victim.sendMessage("§c§lBLOOD DEBT §7- Cursed. Kill " + killer.getName() + " back to clear it.");
        } else {
            applyCurse(victim, killer.getUniqueId(), POOL_B[random.nextInt(POOL_B.length)]);
            victim.sendMessage("§4§lBLOOD DEBT §7- Cursed again, and it's worse this time.");

            int prizeLevels = plugin.getConfig().getInt("blood-debt.repeat-kill-prize-xp-levels", 3);
            killer.giveExpLevels(prizeLevels);
            killer.sendMessage("§6§lPRIZE §7- Repeat kill on a rival. +" + prizeLevels + " levels.");
        }
    }

    private void applyCurse(Player victim, UUID owedTo, CurseType type) {
        clearAppliedEffects(victim); // wipe any previous curse's lingering effects first
        PersistentDataContainer pdc = victim.getPersistentDataContainer();
        pdc.set(curseTypeKey, PersistentDataType.STRING, type.name());
        pdc.set(curseSourceKey, PersistentDataType.STRING, owedTo.toString());
        lastGlow.remove(victim.getUniqueId());
    }

    public CurseType getCurseType(Player player) {
        String name = player.getPersistentDataContainer().get(curseTypeKey, PersistentDataType.STRING);
        if (name == null) return null;
        try {
            return CurseType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Reactive hook for damage-triggered curses. Called from BloodCurseEffectListener. */
    public void onDamageTaken(Player victim, boolean isFallDamage, boolean isFromMob) {
        CurseType type = getCurseType(victim);
        if (type == null) return;

        switch (type) {
            case FRAGILE:
                victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false));
                break;
            case DIZZY_STRIKES:
                victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 40, 0, false, false));
                break;
            case SLOW_BLEED:
                victim.setFoodLevel(Math.max(0, victim.getFoodLevel() - 1));
                break;
            case STAGGERED:
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));
                break;
            case FESTERING_WOUNDS:
                victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, false, false));
                break;
            default:
                break;
        }
    }

    /** Returns a damage multiplier to apply for the victim's current curse, given the damage context. */
    public double getDamageMultiplier(Player victim, boolean isFallDamage, boolean isFromMob) {
        CurseType type = getCurseType(victim);
        if (type == null) return 1.0;
        if (type == CurseType.MARKED_PREY && isFromMob) return 1.5;
        if (type == CurseType.BRITTLE_BONES && isFallDamage) return 2.0;
        return 1.0;
    }

    private void clearAppliedEffects(Player player) {
        player.removePotionEffect(PotionEffectType.HUNGER);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.GLOWING);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.POISON);
    }

    /** Admin override: force-clears a player's curse regardless of whether they've avenged it. */
    public void adminClearCurse(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.remove(curseTypeKey);
        pdc.remove(curseSourceKey);
        clearAppliedEffects(player);
        lastGlow.remove(player.getUniqueId());
        player.sendMessage("§a§lBLOOD DEBT CLEARED §7- An admin wiped your curse.");
    }

    private void clearCurseIfOwedTo(Player possiblyAvenged, UUID rivalUUID) {
        PersistentDataContainer pdc = possiblyAvenged.getPersistentDataContainer();
        String owed = pdc.get(curseSourceKey, PersistentDataType.STRING);
        if (owed != null && owed.equals(rivalUUID.toString())) {
            pdc.remove(curseTypeKey);
            pdc.remove(curseSourceKey);
            clearAppliedEffects(possiblyAvenged);
            lastGlow.remove(possiblyAvenged.getUniqueId());
            possiblyAvenged.sendMessage("§a§lBLOOD DEBT CLEARED §7- Revenge settles the score.");
        }
    }
}
