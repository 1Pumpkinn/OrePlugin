package hs.orePlugin.Listeners;

import hs.orePlugin.Managers.PlayerDataManager;
import hs.orePlugin.Managers.TrustManager;
import hs.orePlugin.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntityDamageListener implements Listener {
    private final TrustManager trustManager;
    private final PlayerDataManager playerDataManager;

    public EntityDamageListener(TrustManager trustManager, PlayerDataManager playerDataManager) {
        this.trustManager = trustManager;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        PlayerData data = playerDataManager.getPlayerData(player);

        // Crystal mode protection (Amethyst ore)
        if (data.isCrystalModeActive()) {
            event.setCancelled(true);
            return;
        }

        // Coal ore - water damage
        if (data.getCurrentOre() != null && data.getCurrentOre().name().equals("COAL") &&
                event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
            // Amplify water damage
            event.setDamage(event.getDamage() * 2);
        }

        // Netherite ore - no fire damage
        if (data.getCurrentOre() != null && data.getCurrentOre().name().equals("NETHERITE") &&
                (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                        event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                        event.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
            event.setCancelled(true);
        }

        // Redstone ore - dripstone immunity
        if (data.getCurrentOre() != null && data.getCurrentOre().name().equals("REDSTONE") &&
                event.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK) {
            event.setCancelled(true);
        }
    }
}