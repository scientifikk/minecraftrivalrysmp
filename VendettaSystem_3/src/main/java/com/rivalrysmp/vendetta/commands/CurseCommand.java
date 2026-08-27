package com.rivalrysmp.vendetta.commands;

import com.rivalrysmp.vendetta.managers.BloodDebtManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CurseCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final BloodDebtManager bloodDebtManager;

    public CurseCommand(JavaPlugin plugin, BloodDebtManager bloodDebtManager) {
        this.plugin = plugin;
        this.bloodDebtManager = bloodDebtManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7Usage: /curse check [player] | /curse clear <player>");
            return true;
        }

        if (args[0].equalsIgnoreCase("check")) {
            Player target;
            if (args.length > 1) {
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found or offline.");
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§7Usage: /curse check <player>");
                return true;
            }
            BloodDebtManager.CurseType type = bloodDebtManager.getCurseType(target);
            sender.sendMessage("§e" + target.getName() + " is currently "
                    + (type == null ? "not cursed." : "cursed with: §c" + type.name()));
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            if (!sender.isOp() && !sender.hasPermission("vendetta.admin")) {
                sender.sendMessage("§cYou don't have permission to do that.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§7Usage: /curse clear <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found or offline.");
                return true;
            }
            bloodDebtManager.adminClearCurse(target);
            sender.sendMessage("§a" + target.getName() + "'s curse has been cleared.");
            return true;
        }

        sender.sendMessage("§7Usage: /curse check [player] | /curse clear <player>");
        return true;
    }
}
