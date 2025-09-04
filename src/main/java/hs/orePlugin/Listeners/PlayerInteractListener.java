package hs.orePlugin.Listeners;

import hs.orePlugin.Managers.OreManager;
import hs.orePlugin.Managers.PlayerDataManager;
import hs.orePlugin.Managers.TrustManager;
import hs.orePlugin.OreType;
import hs.orePlugin.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {
    private final OreManager oreManager;
    private final PlayerDataManager playerDataManager;
    private final TrustManager trustManager;

    public PlayerInteractListener(OreManager oreManager, PlayerDataManager playerDataManager, TrustManager trustManager) {
        this.oreManager = oreManager;
        this.playerDataManager = playerDataManager;
        this.trustManager = trustManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        PlayerData data = playerDataManager.getPlayerData(player);
        OreType currentOre = data.getCurrentOre();

        // Check if player is right-clicking with their ore material to use ability
        if (currentOre != null && item.getType() == currentOre.getMaterial()) {
            event.setCancelled(true);
            oreManager.useOreAbility(player);
        }
    }
}