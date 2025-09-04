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
    private ActionBarManager actionBarManager; // ADD THIS LINE - Missing field declaration
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

        // Initialize managers
        playerDataManager = new PlayerDataManager(this);
        abilityManager = new AbilityManager(this);
        trustManager = new TrustManager(this);
        actionBarManager = new ActionBarManager(this); // ADD THIS LINE - Initialize ActionBarManager

        // Register events
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new AbilityListener(this), this);

        // Register commands
        getCommand("trust").setExecutor(new TrustCommand(this));
        getCommand("untrust").setExecutor(new TrustCommand(this));
        getCommand("trustlist").setExecutor(new TrustCommand(this));
        getCommand("oreabilities").setExecutor(new OreAbilitiesCommand(this));

        getLogger().info("Ore Abilities Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Stop all action bars when plugin disables
        if (actionBarManager != null) {
            actionBarManager.stopAllActionBars();
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
        return actionBarManager; // This will now work correctly
    }

    public FileConfiguration getPlayerDataConfig() {
        return playerDataConfig;
    }
}