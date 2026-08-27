package com.rivalrysmp.vendetta;

import com.rivalrysmp.vendetta.managers.BloodDebtManager;
import com.rivalrysmp.vendetta.managers.DeathCurseManager;
import com.rivalrysmp.vendetta.managers.HardcoreManager;
import com.rivalrysmp.vendetta.commands.CurseCommand;
import com.rivalrysmp.vendetta.commands.LivesCommand;
import com.rivalrysmp.vendetta.commands.StreakCommand;
import com.rivalrysmp.vendetta.listeners.BloodCurseEffectListener;
import com.rivalrysmp.vendetta.listeners.LifeTotemListener;
import com.rivalrysmp.vendetta.listeners.PlayerDeathListener;

import org.bukkit.plugin.java.JavaPlugin;

public class VendettaPlugin extends JavaPlugin {

    private static VendettaPlugin instance;

    private BloodDebtManager bloodDebtManager;
    private HardcoreManager hardcoreManager;
    private DeathCurseManager deathCurseManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.bloodDebtManager = new BloodDebtManager(this);
        this.hardcoreManager = new HardcoreManager(this);
        this.deathCurseManager = new DeathCurseManager(this);

        bloodDebtManager.startHeartbeat();
        hardcoreManager.registerLifeTotemRecipe();

        // Listeners
        getServer().getPluginManager().registerEvents(
                new PlayerDeathListener(this, bloodDebtManager, hardcoreManager, deathCurseManager), this);
        getServer().getPluginManager().registerEvents(
                new BloodCurseEffectListener(bloodDebtManager), this);
        getServer().getPluginManager().registerEvents(
                new LifeTotemListener(hardcoreManager), this);

        // Commands
        getCommand("lives").setExecutor(new LivesCommand(this, hardcoreManager));
        getCommand("streak").setExecutor(new StreakCommand(this, deathCurseManager));
        getCommand("curse").setExecutor(new CurseCommand(this, bloodDebtManager));

        getLogger().info("Vendetta System enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Vendetta System disabled.");
    }

    public static VendettaPlugin get() {
        return instance;
    }

    public BloodDebtManager getBloodDebtManager() { return bloodDebtManager; }
    public HardcoreManager getHardcoreManager() { return hardcoreManager; }
    public DeathCurseManager getDeathCurseManager() { return deathCurseManager; }
}
