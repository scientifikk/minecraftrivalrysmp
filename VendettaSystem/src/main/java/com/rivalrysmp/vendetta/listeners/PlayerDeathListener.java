package com.rivalrysmp.vendetta.listeners;

import com.rivalrysmp.vendetta.managers.BloodDebtManager;
import com.rivalrysmp.vendetta.managers.BountyManager;
import com.rivalrysmp.vendetta.managers.HardcoreManager;
import com.rivalrysmp.vendetta.managers.LegacyBookManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerDeathListener implements Listener {

    private final JavaPlugin plugin;
    private final BloodDebtManager bloodDebt;
    private final HardcoreManager hardcore;
    private final BountyManager bounty;
    private final LegacyBookManager legacyBook;

    public PlayerDeathListener(JavaPlugin plugin, BloodDebtManager bloodDebt, HardcoreManager hardcore,
                                BountyManager bounty, LegacyBookManager legacyBook) {
        this.plugin = plugin;
        this.bloodDebt = bloodDebt;
        this.hardcore = hardcore;
        this.bounty = bounty;
        this.legacyBook = legacyBook;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            bloodDebt.registerKill(killer, victim);
            bounty.onPlayerKilled(killer, victim);
            hardcore.handleDeath(victim, killer);
            // If this death was itself a revenge kill on someone else's killer, release them.
            hardcore.checkForRevengeRelease(victim);

            legacyBook.logEntry("kill", killer.getName(), victim.getName(),
                    killer.getName() + " struck down " + victim.getName());
        } else {
            hardcore.handleDeath(victim, null);
            legacyBook.logEntry("death", victim.getName(), "", victim.getName() + " died (no PvP killer).");
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Delay one tick so the respawn location is set before we force spectator mode.
        plugin.getServer().getScheduler().runTask(plugin, () -> hardcore.applyFreeze(player));
    }
}
