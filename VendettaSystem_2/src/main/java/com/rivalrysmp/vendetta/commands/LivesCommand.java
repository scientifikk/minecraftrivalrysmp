package com.rivalrysmp.vendetta.commands;

import com.rivalrysmp.vendetta.managers.HardcoreManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class LivesCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final HardcoreManager hardcoreManager;

    public LivesCommand(JavaPlugin plugin, HardcoreManager hardcoreManager) {
        this.plugin = plugin;
        this.hardcoreManager = hardcoreManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.isOp() && !sender.hasPermission("vendetta.admin")) {
                sender.sendMessage("§cYou don't have permission to do that.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§7Usage: /lives reset <player>");
                return true;
            }
            Player resetTarget = Bukkit.getPlayer(args[1]);
            if (resetTarget == null) {
                sender.sendMessage("§cPlayer not found or offline.");
                return true;
            }
            hardcoreManager.adminRelease(resetTarget);
            sender.sendMessage("§a" + resetTarget.getName() + "'s lives and frozen state have been reset.");
            return true;
        }

        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found or offline.");
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§7Usage: /lives <player>");
            return true;
        }

        int lives = hardcoreManager.getLives(target);
        boolean frozen = hardcoreManager.isFrozen(target);
        sender.sendMessage("§e" + target.getName() + " has " + lives + " life/lives remaining."
                + (frozen ? " §c(FROZEN - awaiting revenge kill)" : ""));
        return true;
    }
}
