package hs.orePlugin;

import hs.orePlugin.Commands.OreCommand;
import hs.orePlugin.Commands.TrustCommand;
import hs.orePlugin.Commands.TrustListCommand;
import hs.orePlugin.Commands.UntrustCommand;
import hs.orePlugin.Listeners.*;
import hs.orePlugin.Managers.OreManager;
import hs.orePlugin.Managers.PlayerDataManager;
import hs.orePlugin.Managers.TrustManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

public class OrePlugin extends JavaPlugin implements Listener {

    private static OrePlugin instance;
    private PlayerDataManager playerDataManager;
    private TrustManager trustManager;
    private OreManager oreManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        playerDataManager = new PlayerDataManager(this);
        trustManager = new TrustManager(this);
        oreManager = new OreManager(this);

        // Register commands
        getCommand("trust").setExecutor(new TrustCommand(trustManager));
        getCommand("untrust").setExecutor(new UntrustCommand(trustManager));
        getCommand("trustlist").setExecutor(new TrustListCommand(trustManager));
        getCommand("ore").setExecutor(new OreCommand(oreManager, playerDataManager));

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(playerDataManager, oreManager), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(oreManager, playerDataManager, trustManager), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(trustManager, playerDataManager), this);
        getServer().getPluginManager().registerEvents(new PlayerItemCraftListener(oreManager, playerDataManager), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(playerDataManager), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(playerDataManager), this);
        getServer().getPluginManager().registerEvents(new EntityDamageByEntityListener(playerDataManager, trustManager), this);
        getServer().getPluginManager().registerEvents(new PlayerItemConsumeListener(playerDataManager), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(playerDataManager, this), this);

        // Start passive effect tasks
        startPassiveEffectTasks();

        getLogger().info("OrePlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }
        getLogger().info("OrePlugin has been disabled!");
    }

    private void startPassiveEffectTasks() {
        // Task for checking passive effects every second
        new BukkitRunnable() {
            @Override
            public void run() {
                if (playerDataManager != null) {
                    playerDataManager.updatePassiveEffects();
                }
            }
        }.runTaskTimer(this, 0L, 20L);

        // Task for item dropping (Iron ore downside)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (playerDataManager != null) {
                    playerDataManager.handleIronItemDrop();
                }
            }
        }.runTaskTimer(this, 0L, 12000L); // Every 10 minutes (12000 ticks)
    }

    public static OrePlugin getInstance() {
        return instance;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public TrustManager getTrustManager() {
        return trustManager;
    }

    public OreManager getOreManager() {
        return oreManager;
    }
}