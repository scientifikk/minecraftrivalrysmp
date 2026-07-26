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
