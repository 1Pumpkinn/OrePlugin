package hs.orePlugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

public class OreAbilitiesPlugin extends JavaPlugin {

    private static OreAbilitiesPlugin instance;
    private PlayerDataManager playerDataManager;
    private AbilityManager abilityManager;
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

        // Initialize managers
        playerDataManager = new PlayerDataManager(this);
        abilityManager = new AbilityManager(this);
        trustManager = new TrustManager(this);
        actionBarManager = new ActionBarManager(this);
        activationManager = new AbilityActivationManager(this);
        recipeManager = new RecipeManager(this);

        // Register recipes
        recipeManager.registerAllRecipes();

        // Register events
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new AbilityListener(this), this);
        // Note: OreItemListener is no longer needed since ore switching happens through crafting
        getServer().getPluginManager().registerEvents(activationManager, this);

        // Register commands
        OreAbilitiesCommand mainCommand = new OreAbilitiesCommand(this);
        TrustCommand trustCommand = new TrustCommand(this);

        getCommand("trust").setExecutor(trustCommand);
        getCommand("untrust").setExecutor(trustCommand);
        getCommand("trustlist").setExecutor(trustCommand);
        getCommand("oreabilities").setExecutor(mainCommand);
        getCommand("ability").setExecutor(mainCommand);
        getCommand("bedrock").setExecutor(mainCommand);

        getLogger().info("Ore Abilities Plugin has been enabled!");
        getLogger().info("Features: Direct crafting system, enhanced ore info, admin commands, bedrock support!");
        getLogger().info("Registered " + getRecipeCount() + " custom ore recipes with 25% shatter chance!");
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
            recipeManager.removeAllRecipes();
        }

        savePlayerData();
        getLogger().info("Ore Abilities Plugin has been disabled!");
    }

    private void setupPlayerDataFile() {
        playerDataFile = new File(getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public void savePlayerData() {
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public OreConfigs getOreConfigs() {
        return oreConfigs;
    }

    private int getRecipeCount() {
        // Count of craftable ore types
        return 10; // Coal, Copper, Iron, Gold, Redstone, Lapis, Emerald, Amethyst, Diamond, Netherite
    }


    // Reload method for admin commands
    public void reloadPlugin() {
        // Reload config
        reloadConfig();

        // Reload player data
        playerDataManager.loadPlayerData();

        // Reload recipes
        recipeManager.removeAllRecipes();
        recipeManager.registerAllRecipes();

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
}