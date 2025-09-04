package hs.orePlugin.Listeners;

import hs.orePlugin.Managers.PlayerDataManager;
import hs.orePlugin.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.concurrent.ThreadLocalRandom;

public class BlockBreakListener implements Listener {
    private final PlayerDataManager playerDataManager;

    public BlockBreakListener(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerData data = playerDataManager.getPlayerData(player);

        // Diamond ore downside - 50% chance ores don't drop
        if (data.getCurrentOre() != null && data.getCurrentOre().name().equals("DIAMOND")) {
            String blockName = event.getBlock().getType().name().toLowerCase();
            if (blockName.contains("ore") && ThreadLocalRandom.current().nextBoolean()) {
                event.setDropItems(false);
            }
        }
    }
}