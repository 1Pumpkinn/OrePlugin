package hs.orePlugin.Listeners;

import hs.orePlugin.Managers.PlayerDataManager;
import hs.orePlugin.OrePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

public class InventoryClickListener implements Listener {
    private final PlayerDataManager playerDataManager;
    private final OrePlugin orePlugin;

    public InventoryClickListener(PlayerDataManager playerDataManager, OrePlugin orePlugin) {
        this.playerDataManager = playerDataManager;
        this.orePlugin = orePlugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        hs.orePlugin.PlayerData data = playerDataManager.getPlayerData(player);

        // Lapis ore - using anvils doesn't use levels
        if (data.getCurrentOre() != null && data.getCurrentOre().name().equals("LAPIS") &&
                event.getInventory().getType() == InventoryType.ANVIL) {
            // Store player's current level to restore after anvil use
            int currentLevel = player.getLevel();
            player.getServer().getScheduler().runTaskLater(
                    orePlugin,
                    () -> player.setLevel(currentLevel), 1L);
        }
    }
}