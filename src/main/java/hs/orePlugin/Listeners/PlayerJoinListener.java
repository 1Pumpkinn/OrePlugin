package hs.orePlugin.Listeners;

import hs.orePlugin.Managers.OreManager;
import hs.orePlugin.Managers.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final PlayerDataManager playerDataManager;
    private final OreManager oreManager;

    public PlayerJoinListener(PlayerDataManager playerDataManager, OreManager oreManager) {
        this.playerDataManager = playerDataManager;
        this.oreManager = oreManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerDataManager.assignStarterOre(event.getPlayer());
    }
}
