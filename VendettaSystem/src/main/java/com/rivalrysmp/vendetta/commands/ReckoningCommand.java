package com.rivalrysmp.vendetta.commands;

import com.rivalrysmp.vendetta.managers.ReckoningManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ReckoningCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final ReckoningManager reckoningManager;

    public ReckoningCommand(JavaPlugin plugin, ReckoningManager reckoningManager) {
        this.plugin = plugin;
        this.reckoningManager = reckoningManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7Usage: /reckoning bet <player> <amount> | /reckoning info");
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(reckoningManager.isEventActive()
                    ? "§4The Reckoning is currently active!"
                    : "§7No Reckoning is active right now. Check back this week.");
            return true;
        }

        if (args[0].equalsIgnoreCase("bet")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§7Usage: /reckoning bet <player> <amount>");
                return true;
            }
            Player bettor = (Player) sender;
            Player onPlayer = Bukkit.getPlayer(args[1]);
            if (onPlayer == null) {
                sender.sendMessage("§cPlayer not found or offline.");
                return true;
            }
            double amount;
            try {
                amount = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid amount.");
                return true;
            }
            boolean success = reckoningManager.placeBet(bettor, onPlayer, amount);
            if (!success) {
                sender.sendMessage("§cCould not place bet. Is a Reckoning active, and do you have enough money?");
            }
            return true;
        }

        sender.sendMessage("§7Usage: /reckoning bet <player> <amount> | /reckoning info");
        return true;
    }
}
