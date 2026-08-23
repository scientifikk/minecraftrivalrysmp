package com.rivalrysmp.vendetta.listeners;

import com.rivalrysmp.vendetta.managers.BloodDebtManager;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class BloodCurseEffectListener implements Listener {

    private final BloodDebtManager bloodDebt;

    public BloodCurseEffectListener(BloodDebtManager bloodDebt) {
        this.bloodDebt = bloodDebt;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();

        boolean isFall = event.getCause() == EntityDamageEvent.DamageCause.FALL;
        boolean isFromMob = event instanceof EntityDamageByEntityEvent
                && ((EntityDamageByEntityEvent) event).getDamager() instanceof Monster;

        double multiplier = bloodDebt.getDamageMultiplier(victim, isFall, isFromMob);
        if (multiplier != 1.0) {
            event.setDamage(event.getDamage() * multiplier);
        }

        // Reactive effects (Blindness-on-hit, hunger drain) applied after damage is finalized.
        bloodDebt.onDamageTaken(victim, isFall, isFromMob);
    }
}
