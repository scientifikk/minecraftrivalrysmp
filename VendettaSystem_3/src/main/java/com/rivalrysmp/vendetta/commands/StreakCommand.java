package com.rivalrysmp.vendetta.commands;

import com.rivalrysmp.vendetta.managers.DeathCurseManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class StreakCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final DeathCurseManager deathCurseManager;

    public StreakCommand(JavaPlugin plugin, DeathCurseManager deathCurseManager) {
        this.plugin = plugin;
        this.deathCurseManager = deathCurseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
            sender.sendMessage("§7Usage: /streak <player>");
            return true;
        }

        int streak = deathCurseManager.getStreak(target);
        boolean exiled = deathCurseManager.isExiled(target);
        sender.sendMessage("§e" + target.getName() + " has a " + streak + "-death losing streak."
                + (exiled ? " §4(EXILED - " + deathCurseManager.getExileRemainingSeconds(target) + "s remaining)" : ""));
        return true;
    }
}
