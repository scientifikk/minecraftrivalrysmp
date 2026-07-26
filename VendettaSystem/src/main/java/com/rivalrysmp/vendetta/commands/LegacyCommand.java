package com.rivalrysmp.vendetta.commands;

import com.rivalrysmp.vendetta.managers.LegacyBookManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class LegacyCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final LegacyBookManager legacyBookManager;

    public LegacyCommand(JavaPlugin plugin, LegacyBookManager legacyBookManager) {
        this.plugin = plugin;
        this.legacyBookManager = legacyBookManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<String> entries;
        if (args.length == 0) {
            entries = legacyBookManager.getRecentEntries(15);
            sender.sendMessage("§6§lLegacy Book §7- Recent events:");
        } else {
            entries = legacyBookManager.getEntriesFor(args[0], 15);
            sender.sendMessage("§6§lLegacy Book §7- Events involving " + args[0] + ":");
        }

        if (entries.isEmpty()) {
            sender.sendMessage("§7No entries found.");
        } else {
            for (String entry : entries) {
                sender.sendMessage("§7" + entry);
            }
        }
        return true;
    }
}
