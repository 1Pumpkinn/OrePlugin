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
    private RecipeGUI recipeGUI;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveDefaultConfig();
        setupPlayerDataFile();
        this.oreConfigs = new OreConfigs(this);

        playerDataManager = new PlayerDataManager(this);
        abilityManager = new AbilityManager(this);
        abilityListener = new AbilityListener(this);
        playerListener = new PlayerListener(this);
        trustManager = new TrustManager(this);
        actionBarManager = new ActionBarManager(this);
        activationManager = new AbilityActivationManager(this);

        recipeManager = new RecipeManager(this);
        recipeGUI = new RecipeGUI(this);

        new BukkitRunnable() {
            @Override
            public void run() {
                recipeManager.registerAllRecipes();
                getLogger().info("Successfully registered " + recipeManager.getRecipeCount() + " ore mastery recipes!");
            }
        }.runTaskLater(this, 20L);

        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(abilityListener, this);
        getServer().getPluginManager().registerEvents(activationManager, this);

        OreAbilitiesCommand mainCommand = new OreAbilitiesCommand(this);
        TrustCommand trustCommand = new TrustCommand(this);

        // Register trust commands
        getCommand("trust").setExecutor(trustCommand);
        getCommand("untrust").setExecutor(trustCommand);
        getCommand("trustlist").setExecutor(trustCommand);

        // Register main ore commands
        getCommand("ore").setExecutor(mainCommand);
        getCommand("ability").setExecutor(mainCommand);
        getCommand("bedrock").setExecutor(mainCommand);
        getCommand("orecd").setExecutor(mainCommand);

        // Register recipes commands
        getCommand("recipes").setExecutor(mainCommand);
        getCommand("recipe").setExecutor(mainCommand);
        getCommand("orerecipes").setExecutor(mainCommand);

        // NEW: Register reroll command
        getCommand("reroll").setExecutor(mainCommand);

        getLogger().info("Ore Abilities Plugin has been enabled!");
        getLogger().info("Features: Dirt leather armor is truly unbreakable, LAPIS can enchant with 0 levels!");
        getLogger().info("Recipe commands registered: /recipes, /recipe, /orerecipes");
        getLogger().info("Admin commands: /reroll <player> - Reroll starter ore");
    }

    @Override
    public void onDisable() {
        if (actionBarManager != null) {
            actionBarManager.stopAllActionBars();
        }

        if (activationManager != null) {
            activationManager.saveBedrockData();
        }

        if (recipeManager != null) {
            getLogger().info("Removed all custom recipes");
        }
        if (recipeGUI != null) {
            recipeGUI.cleanup();
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

    public void reloadPlugin() {
        reloadConfig();
        this.oreConfigs = new OreConfigs(this);

        playerDataManager.loadPlayerData();

        new BukkitRunnable() {
            @Override
            public void run() {
                recipeManager.registerAllRecipes();
                getLogger().info("Reloaded " + recipeManager.getRecipeCount() + " ore mastery recipes!");
            }
        }.runTaskLater(this, 5L);

        getLogger().info("Ore Abilities Plugin reloaded successfully!");
    }

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

    public RecipeGUI getRecipeGUI() {
        return recipeGUI;
    }
}