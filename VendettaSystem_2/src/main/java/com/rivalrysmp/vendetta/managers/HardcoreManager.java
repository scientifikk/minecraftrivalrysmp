package com.rivalrysmp.vendetta.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.UUID;

/**
 * Each player has N lives (default 3). On their final death, they are
 * frozen (spectator mode) until someone kills the specific player who
 * killed them last (their "killer"). Extra lives can also be earned by
 * crafting an Extra Life Totem (4 diamonds, 2 netherite ingots, 2 quartz,
 * 1 enchanted golden apple) and consuming it.
 */
public class HardcoreManager {

    private final JavaPlugin plugin;
    private final NamespacedKey livesKey;
    private final NamespacedKey frozenKey;
    private final NamespacedKey killerKey;
    private final NamespacedKey lifeTotemKey;

    public HardcoreManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.livesKey = new NamespacedKey(plugin, "lives_remaining");
        this.frozenKey = new NamespacedKey(plugin, "frozen");
        this.killerKey = new NamespacedKey(plugin, "last_killer");
        this.lifeTotemKey = new NamespacedKey(plugin, "life_totem");
    }

    public int getLives(Player player) {
        Integer lives = player.getPersistentDataContainer().get(livesKey, PersistentDataType.INTEGER);
        if (lives == null) {
            int starting = plugin.getConfig().getInt("hardcore.starting-lives", 3);
            player.getPersistentDataContainer().set(livesKey, PersistentDataType.INTEGER, starting);
            return starting;
        }
        return lives;
    }

    public boolean isFrozen(Player player) {
        Byte frozen = player.getPersistentDataContainer().get(frozenKey, PersistentDataType.BYTE);
        return frozen != null && frozen == (byte) 1;
    }

    public UUID getLastKiller(Player player) {
        String s = player.getPersistentDataContainer().get(killerKey, PersistentDataType.STRING);
        return s == null ? null : UUID.fromString(s);
    }

    /** Called whenever a player dies to a PvP killer. Returns true if this triggered a freeze. */
    public boolean handleDeath(Player victim, Player killer) {
        PersistentDataContainer pdc = victim.getPersistentDataContainer();
        int lives = getLives(victim) - 1;
        pdc.set(livesKey, PersistentDataType.INTEGER, Math.max(lives, 0));

        if (lives <= 0) {
            pdc.set(frozenKey, PersistentDataType.BYTE, (byte) 1);
            if (killer != null) {
                pdc.set(killerKey, PersistentDataType.STRING, killer.getUniqueId().toString());
            }
            victim.sendMessage("§4§lOUT OF LIVES §7- You are frozen until someone kills " +
                    (killer != null ? killer.getName() : "your killer") + ".");
            return true;
        } else {
            victim.sendMessage("§e§lLIVES §7- You have " + lives + " life/lives remaining.");
            return false;
        }
    }

    /** Applies spectator lock; call on respawn if the player is flagged frozen. */
    public void applyFreeze(Player player) {
        if (isFrozen(player) && plugin.getConfig().getBoolean("hardcore.use-spectator-freeze", true)) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    /** Call whenever anyone dies in PvP; checks if that death frees a frozen victim. */
    public void checkForRevengeRelease(Player killedPlayer) {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            UUID storedKiller = getLastKiller(online);
            if (isFrozen(online) && storedKiller != null && storedKiller.equals(killedPlayer.getUniqueId())) {
                release(online);
            }
        }
    }

    /** Admin safety valve: force-clears a frozen/0-life state regardless of cause. */
    public void adminRelease(Player player) {
        release(player);
    }

    private void release(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(frozenKey, PersistentDataType.BYTE, (byte) 0);
        pdc.set(livesKey, PersistentDataType.INTEGER, plugin.getConfig().getInt("hardcore.starting-lives", 3));
        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(player.getWorld().getSpawnLocation());
        player.sendMessage("§a§lREVIVED §7- Your killer has fallen. You're back in the fight with a fresh set of lives.");
    }

    // ---- Craftable Extra Life Totem ----

    public ItemStack createLifeTotemItem() {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7d\u00a7lExtra Life Totem");
        meta.setLore(Arrays.asList(
                "\u00a77Right-click to consume.",
                "\u00a77Grants one extra hardcore life.",
                "\u00a78Forged from diamonds, netherite,",
                "\u00a78quartz, and an enchanted apple."
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(lifeTotemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isLifeTotem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(lifeTotemKey, PersistentDataType.BYTE);
    }

    public void addLife(Player player) {
        int current = getLives(player);
        int max = plugin.getConfig().getInt("hardcore.max-lives", 0);
        if (max > 0 && current >= max) {
            player.sendMessage("\u00a77You're already at the maximum number of lives (" + max + ").");
            return;
        }
        player.getPersistentDataContainer().set(livesKey, PersistentDataType.INTEGER, current + 1);
        player.sendMessage("\u00a7a\u00a7lEXTRA LIFE \u00a77- You now have " + (current + 1) + " lives.");
    }

    public void registerLifeTotemRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(plugin, "craft_life_totem");
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createLifeTotemItem());
        recipe.shape("DDD", "NGN", "QDQ");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('G', Material.ENCHANTED_GOLDEN_APPLE);
        recipe.setIngredient('Q', Material.QUARTZ);
        recipe.setCategory(CraftingBookCategory.MISC);
        Bukkit.addRecipe(recipe);
    }
}
