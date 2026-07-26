package com.rivalrysmp.vendetta.managers;

import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Each player has N lives (default 3). On their final death, they are
 * frozen (spectator mode) until someone kills the specific player who
 * killed them last (their "killer").
 */
public class HardcoreManager {

    private final JavaPlugin plugin;
    private final NamespacedKey livesKey;
    private final NamespacedKey frozenKey;
    private final NamespacedKey killerKey;

    public HardcoreManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.livesKey = new NamespacedKey(plugin, "lives_remaining");
        this.frozenKey = new NamespacedKey(plugin, "frozen");
        this.killerKey = new NamespacedKey(plugin, "last_killer");
    }

    public int getLives(Player player) {
        Integer lives = player.getPersistentDataContainer().get(livesKey, PersistentDataType.INTEGER);
        if (lives == null) {
            int starting = plugin.getConfig().getInt("hardcore.starting-lives", 3);
            player.getPersistentDataContainer().set(livesKey, PersistentDataType.INTEGER, starting);
            return starting;
        }
        return lives;
    }

    public boolean isFrozen(Player player) {
        Byte frozen = player.getPersistentDataContainer().get(frozenKey, PersistentDataType.BYTE);
        return frozen != null && frozen == (byte) 1;
    }

    public UUID getLastKiller(Player player) {
        String s = player.getPersistentDataContainer().get(killerKey, PersistentDataType.STRING);
        return s == null ? null : UUID.fromString(s);
    }

    /** Called whenever a player dies to a PvP killer. Returns true if this triggered a freeze. */
    public boolean handleDeath(Player victim, Player killer) {
        PersistentDataContainer pdc = victim.getPersistentDataContainer();
        int lives = getLives(victim) - 1;
        pdc.set(livesKey, PersistentDataType.INTEGER, Math.max(lives, 0));

        if (lives <= 0) {
            pdc.set(frozenKey, PersistentDataType.BYTE, (byte) 1);
            if (killer != null) {
                pdc.set(killerKey, PersistentDataType.STRING, killer.getUniqueId().toString());
            }
            victim.sendMessage("§4§lOUT OF LIVES §7- You are frozen until someone kills " +
                    (killer != null ? killer.getName() : "your killer") + ".");
            return true;
        } else {
            victim.sendMessage("§e§lLIVES §7- You have " + lives + " life/lives remaining.");
            return false;
        }
    }

    /** Applies spectator lock; call on respawn if the player is flagged frozen. */
    public void applyFreeze(Player player) {
        if (isFrozen(player) && plugin.getConfig().getBoolean("hardcore.use-spectator-freeze", true)) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    /** Call whenever anyone dies in PvP; checks if that death frees a frozen victim. */
    public void checkForRevengeRelease(Player killedPlayer) {
        // Any frozen player whose recorded killer matches killedPlayer gets released.
        for (org.bukkit.entity.Player online : plugin.getServer().getOnlinePlayers()) {
            UUID storedKiller = getLastKiller(online);
            if (isFrozen(online) && storedKiller != null && storedKiller.equals(killedPlayer.getUniqueId())) {
                release(online);
            }
        }
    }

    private void release(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(frozenKey, PersistentDataType.BYTE, (byte) 0);
        pdc.set(livesKey, PersistentDataType.INTEGER, plugin.getConfig().getInt("hardcore.starting-lives", 3));
        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(player.getWorld().getSpawnLocation());
        player.sendMessage("§a§lREVIVED §7- Your killer has fallen. You're back in the fight with a fresh set of lives.");
    }
}
