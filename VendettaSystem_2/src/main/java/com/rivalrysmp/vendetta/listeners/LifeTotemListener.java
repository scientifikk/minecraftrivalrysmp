package com.rivalrysmp.vendetta.listeners;

import com.rivalrysmp.vendetta.managers.HardcoreManager;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class LifeTotemListener implements Listener {

    private final HardcoreManager hardcoreManager;

    public LifeTotemListener(HardcoreManager hardcoreManager) {
        this.hardcoreManager = hardcoreManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!hardcoreManager.isLifeTotem(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (hardcoreManager.isFrozen(player)) {
            player.sendMessage("\u00a7cYou must be revived first before gaining extra lives.");
            return;
        }

        hardcoreManager.addLife(player);
        item.setAmount(item.getAmount() - 1);

        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
    }
}
