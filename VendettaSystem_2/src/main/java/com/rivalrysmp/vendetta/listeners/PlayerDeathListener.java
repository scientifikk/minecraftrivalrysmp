package com.rivalrysmp.vendetta.listeners;

import com.rivalrysmp.vendetta.managers.BloodDebtManager;
import com.rivalrysmp.vendetta.managers.DeathCurseManager;
import com.rivalrysmp.vendetta.managers.HardcoreManager;
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
    private final DeathCurseManager deathCurse;

    public PlayerDeathListener(JavaPlugin plugin, BloodDebtManager bloodDebt, HardcoreManager hardcore,
                                DeathCurseManager deathCurse) {
        this.plugin = plugin;
        this.bloodDebt = bloodDebt;
        this.hardcore = hardcore;
        this.deathCurse = deathCurse;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            bloodDebt.registerKill(killer, victim);
            hardcore.handleDeath(victim, killer);
            // If this death was itself a revenge kill on someone else's killer, release them.
            hardcore.checkForRevengeRelease(victim);
            deathCurse.handlePvpResult(killer, victim);
        }
        // Mob/environmental deaths (killer == null) don't touch Hardcore Lives,
        // Blood Debt, or Death Curses — those are all PvP-only systems.
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Delay one tick so the respawn location is set before we force spectator mode.
        plugin.getServer().getScheduler().runTask(plugin, () -> hardcore.applyFreeze(player));
    }
}
