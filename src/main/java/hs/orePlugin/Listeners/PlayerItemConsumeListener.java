package hs.orePlugin.Listeners;

import hs.orePlugin.Managers.PlayerDataManager;
import hs.orePlugin.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerItemConsumeListener implements Listener {
    private final PlayerDataManager playerDataManager;

    public PlayerItemConsumeListener(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        PlayerData data = playerDataManager.getPlayerData(player);
        Material consumed = event.getItem().getType();

        // Gold ore - golden apples give 2x absorption hearts
        if (data.getCurrentOre() != null && data.getCurrentOre().name().equals("GOLD") &&
                consumed == Material.GOLDEN_APPLE) {
            // Add extra absorption hearts
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1)); // Extra absorption
        }

        // Wood ore - apples act like golden apples
        if (data.getCurrentOre() != null && data.getCurrentOre().name().equals("WOOD") &&
                consumed == Material.APPLE) {
            // Give golden apple effects
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        }
    }
}