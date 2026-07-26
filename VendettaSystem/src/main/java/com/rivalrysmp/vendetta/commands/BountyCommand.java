package com.rivalrysmp.vendetta.commands;

import com.rivalrysmp.vendetta.managers.BountyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class BountyCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final BountyManager bountyManager;

    public BountyCommand(JavaPlugin plugin, BountyManager bountyManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) ) {
            sender.sendMessage("Players only.");
            return true;
        }
        Player placer = (Player) sender;

        if (args.length == 0) {
            sender.sendMessage("§7Usage: /bounty place <player> <amount> | /bounty list");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("§6§lActive Bounties:");
            for (Player p : Bukkit.getOnlinePlayers()) {
                double amt = bountyManager.getBounty(p.getUniqueId());
                if (amt > 0) sender.sendMessage("§7- " + p.getName() + ": $" + amt);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("place")) {
            if (args.length < 3) {
                sender.sendMessage("§7Usage: /bounty place <player> <amount>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
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
            boolean success = bountyManager.placeBounty(placer, target, amount);
            if (!success) {
                sender.sendMessage("§cCould not place bounty. Check your balance and the minimum amount.");
            }
            return true;
        }

        sender.sendMessage("§7Usage: /bounty place <player> <amount> | /bounty list");
        return true;
    }
}
