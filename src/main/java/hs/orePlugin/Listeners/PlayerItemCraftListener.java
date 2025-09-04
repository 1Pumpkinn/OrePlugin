package hs.orePlugin.Listeners;

import hs.orePlugin.Managers.OreManager;
import hs.orePlugin.Managers.PlayerDataManager;
import hs.orePlugin.OreType;
import hs.orePlugin.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.concurrent.ThreadLocalRandom;

public class PlayerItemCraftListener implements Listener {
    private final OreManager oreManager;
    private final PlayerDataManager playerDataManager;

    public PlayerItemCraftListener(OreManager oreManager, PlayerDataManager playerDataManager) {
        this.oreManager = oreManager;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        PlayerData data = playerDataManager.getPlayerData(player);
        ItemStack result = event.getRecipe().getResult();

        // Check if crafting an ore ability
        OreType craftedOre = getOreTypeFromMaterial(result.getType());
        if (craftedOre != null && !craftedOre.isStarter()) {
            event.setCancelled(true);
            if (oreManager.attemptCraft(player, craftedOre)) {
                // Successfully crafted ore ability
                player.getInventory().removeItem(event.getRecipe().getResult());
            }
            return;
        }

        // Gold ore downside - random durability
        if (data.getCurrentOre() != null && data.getCurrentOre().name().equals("GOLD")) {
            if (isToolWeaponOrArmor(result.getType())) {
                ItemMeta meta = result.getItemMeta();
                if (meta instanceof Damageable) {
                    Damageable damageable = (Damageable) meta;
                    int maxDurability = result.getType().getMaxDurability();
                    int randomDurability = ThreadLocalRandom.current().nextInt(1, 101);
                    int damage = maxDurability - randomDurability;
                    if (damage > 0) {
                        damageable.setDamage(damage);
                        result.setItemMeta(meta);
                    }
                }
            }
        }
    }

    private OreType getOreTypeFromMaterial(Material material) {
        for (OreType ore : OreType.values()) {
            if (ore.getMaterial() == material) {
                return ore;
            }
        }
        return null;
    }

    private boolean isToolWeaponOrArmor(Material material) {
        String name = material.name().toLowerCase();
        return name.contains("sword") || name.contains("pickaxe") || name.contains("axe") ||
                name.contains("shovel") || name.contains("hoe") || name.contains("helmet") ||
                name.contains("chestplate") || name.contains("leggings") || name.contains("boots");
    }
}