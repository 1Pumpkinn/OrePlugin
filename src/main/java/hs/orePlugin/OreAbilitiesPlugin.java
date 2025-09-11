package hs.orePlugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.io.IOException;

public class OreAbilitiesPlugin extends JavaPlugin {

    private static OreAbilitiesPlugin instance;
    private PlayerDataManager playerDataManager;
    private AbilityManager abilityManager;
    private AbilityListener abilityListener;
    private PlayerListener playerListener;
    private TrustManager trustManager;
    private ActionBarManager actionBarManager;
    private AbilityActivationManager activationManager;
    private RecipeManager recipeManager;
    private OreConfigs oreConfigs;
    private File playerDataFile;
    private FileConfiguration playerDataConfig;

    @Override
    public void onEnable() {
        instance = this;

        // Create plugin folder and files
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveDefaultConfig();
        setupPlayerDataFile();
        this.oreConfigs = new OreConfigs(this);

        // Initialize managers in correct order
        playerDataManager = new PlayerDataManager(this);
        abilityManager = new AbilityManager(this);
        abilityListener = new AbilityListener(this);
        playerListener = new PlayerListener(this);
        trustManager = new TrustManager(this);
        actionBarManager = new ActionBarManager(this);
        activationManager = new AbilityActivationManager(this);

        // Initialize recipe manager last (it needs other managers to be ready)
        recipeManager = new RecipeManager(this);

        // Register recipes after a short delay to ensure server is fully loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                recipeManager.registerAllRecipes();
                getLogger().info("Successfully registered " + recipeManager.getRecipeCount() + " ore mastery recipes!");
            }
        }.runTaskLater(this, 20L); // 1 second delay

        // Register events
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(abilityListener, this);
        getServer().getPluginManager().registerEvents(activationManager, this);
        // RecipeManager registers itself as a listener in its constructor

        // Register commands with new simplified structure
        OreAbilitiesCommand mainCommand = new OreAbilitiesCommand(this);
        TrustCommand trustCommand = new TrustCommand(this);

        getCommand("trust").setExecutor(trustCommand);
        getCommand("untrust").setExecutor(trustCommand);
        getCommand("trustlist").setExecutor(trustCommand);
        getCommand("ore").setExecutor(mainCommand);
        getCommand("ability").setExecutor(mainCommand);
        getCommand("bedrock").setExecutor(mainCommand);
        getCommand("orecd").setExecutor(mainCommand); // NEW: Register orecd command

        getLogger().info("Ore Abilities Plugin has been enabled!");
        getLogger().info("Features: Simplified commands (/ore), effect cleanup, enhanced ore info!");
        getLogger().info("NEW: /orecd reset command for admins, improved abilities!");
        getLogger().info("Crafting system initialized - recipes will be registered shortly...");
    }

    @Override
    public void onDisable() {
        // Stop all action bars when plugin disables
        if (actionBarManager != null) {
            actionBarManager.stopAllActionBars();
        }

        // Save bedrock data
        if (activationManager != null) {
            activationManager.saveBedrockData();
        }

        // Remove custom recipes
        if (recipeManager != null) {
            getLogger().info("Removed all custom recipes");
        }

        savePlayerData();
        getLogger().info("Ore Abilities Plugin has been disabled!");
    }

    private void setupPlayerDataFile() {
        playerDataFile = new File(getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
                getLogger().info("Created new playerdata.yml file");
            } catch (IOException e) {
                getLogger().severe("Could not create playerdata.yml file!");
                e.printStackTrace();
            }
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public void savePlayerData() {
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            getLogger().severe("Could not save playerdata.yml!");
            e.printStackTrace();
        }
    }

    public OreConfigs getOreConfigs() {
        return oreConfigs;
    }

    // Reload method for admin commands
    public void reloadPlugin() {
        // Reload config
        reloadConfig();
        this.oreConfigs = new OreConfigs(this);

        // Reload player data
        playerDataManager.loadPlayerData();

        // Re-register recipes after a small delay
        new BukkitRunnable() {
            @Override
            public void run() {
                recipeManager.registerAllRecipes();
                getLogger().info("Reloaded " + recipeManager.getRecipeCount() + " ore mastery recipes!");
            }
        }.runTaskLater(this, 5L);

        getLogger().info("Ore Abilities Plugin reloaded successfully!");
    }

    // Getters
    public static OreAbilitiesPlugin getInstance() {
        return instance;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public AbilityListener getAbilityListener() {
        return abilityListener;
    }

    public TrustManager getTrustManager() {
        return trustManager;
    }

    public ActionBarManager getActionBarManager() {
        return actionBarManager;
    }

    public AbilityActivationManager getActivationManager() {
        return activationManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public FileConfiguration getPlayerDataConfig() {
        return playerDataConfig;
    }

    public PlayerListener getPlayerListener() {
        return playerListener;
    }
}