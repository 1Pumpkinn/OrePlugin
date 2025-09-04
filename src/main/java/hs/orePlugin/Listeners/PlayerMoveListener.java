package hs.orePlugin.Listeners;

import hs.orePlugin.Managers.PlayerDataManager;
import hs.orePlugin.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerMoveListener implements Listener {
    private final PlayerDataManager playerDataManager;

    public PlayerMoveListener(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerData data = playerDataManager.getPlayerData(player);

        // Check for water contact with Coal ore
        if (data.getCurrentOre() != null && data.getCurrentOre().name().equals("COAL")) {
            if (player.getLocation().getBlock().getType() == Material.WATER ||
                    player.getEyeLocation().getBlock().getType() == Material.WATER) {
                player.damage(1.0); // Take 1 damage when in water
            }
        }
    }
}