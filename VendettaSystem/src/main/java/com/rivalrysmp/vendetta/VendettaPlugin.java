package com.rivalrysmp.vendetta;

import com.rivalrysmp.vendetta.managers.BloodDebtManager;
import com.rivalrysmp.vendetta.managers.BountyManager;
import com.rivalrysmp.vendetta.managers.HardcoreManager;
import com.rivalrysmp.vendetta.managers.LegacyBookManager;
import com.rivalrysmp.vendetta.managers.ReckoningManager;
import com.rivalrysmp.vendetta.managers.VaultManager;
import com.rivalrysmp.vendetta.commands.BountyCommand;
import com.rivalrysmp.vendetta.commands.LegacyCommand;
import com.rivalrysmp.vendetta.commands.LivesCommand;
import com.rivalrysmp.vendetta.commands.ReckoningCommand;
import com.rivalrysmp.vendetta.listeners.PlayerDeathListener;
import com.rivalrysmp.vendetta.listeners.BountyStealthListener;

import org.bukkit.plugin.java.JavaPlugin;

public class VendettaPlugin extends JavaPlugin {

    private static VendettaPlugin instance;

    private VaultManager vaultManager;
    private BloodDebtManager bloodDebtManager;
    private BountyManager bountyManager;
    private HardcoreManager hardcoreManager;
    private ReckoningManager reckoningManager;
    private LegacyBookManager legacyBookManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.vaultManager = new VaultManager(this);
        if (!vaultManager.setupEconomy()) {
            getLogger().warning("Vault economy not found! Money-based features (bounties, Reckoning bets) will be disabled.");
        }

        this.legacyBookManager = new LegacyBookManager(this);
        this.bloodDebtManager = new BloodDebtManager(this);
        this.hardcoreManager = new HardcoreManager(this);
        this.bountyManager = new BountyManager(this, vaultManager);
        this.reckoningManager = new ReckoningManager(this, vaultManager);

        // Listeners
        getServer().getPluginManager().registerEvents(
                new PlayerDeathListener(this, bloodDebtManager, hardcoreManager, bountyManager, legacyBookManager), this);
        getServer().getPluginManager().registerEvents(
                new BountyStealthListener(this, bountyManager), this);

        // Commands
        getCommand("bounty").setExecutor(new BountyCommand(this, bountyManager));
        getCommand("reckoning").setExecutor(new ReckoningCommand(this, reckoningManager));
        getCommand("legacy").setExecutor(new LegacyCommand(this, legacyBookManager));
        getCommand("lives").setExecutor(new LivesCommand(this, hardcoreManager));

        reckoningManager.scheduleWeeklyCheck();

        getLogger().info("Vendetta System enabled.");
    }

    @Override
    public void onDisable() {
        if (legacyBookManager != null) legacyBookManager.close();
        getLogger().info("Vendetta System disabled.");
    }

    public static VendettaPlugin get() {
        return instance;
    }

    public BloodDebtManager getBloodDebtManager() { return bloodDebtManager; }
    public BountyManager getBountyManager() { return bountyManager; }
    public HardcoreManager getHardcoreManager() { return hardcoreManager; }
    public ReckoningManager getReckoningManager() { return reckoningManager; }
    public LegacyBookManager getLegacyBookManager() { return legacyBookManager; }
    public VaultManager getVaultManager() { return vaultManager; }
}
