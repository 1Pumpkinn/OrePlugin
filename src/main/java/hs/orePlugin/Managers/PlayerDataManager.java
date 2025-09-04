package hs.orePlugin.Managers;

import hs.orePlugin.OrePlugin;
import hs.orePlugin.OreType;
import hs.orePlugin.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerDataManager {
    private final OrePlugin plugin;
    private final Map<UUID, PlayerData> playerDataMap;

    public PlayerDataManager(OrePlugin plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
    }

    public PlayerData getPlayerData(UUID playerId) {
        return playerDataMap.computeIfAbsent(playerId, PlayerData::new);
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public void assignStarterOre(Player player) {
        PlayerData data = getPlayerData(player);
        if (data.getCurrentOre() != null) return; // Already has an ore

        OreType[] starterOres = OreType.getStarterOres();
        OreType assigned = starterOres[ThreadLocalRandom.current().nextInt(starterOres.length)];

        data.setCurrentOre(assigned);
        player.sendMessage(ChatColor.GREEN + "You have been assigned the " +
                assigned.getColor() + assigned.getDisplayName() + ChatColor.GREEN + " ore!");

        // Apply permanent effects based on ore
        applyPermanentOreEffects(player, assigned);
    }

    public void updatePassiveEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = getPlayerData(player);
            OreType ore = data.getCurrentOre();

            if (ore == null) continue;

            applyPassiveEffects(player, ore, data);
        }
    }

    private void applyPassiveEffects(Player player, OreType ore, PlayerData data) {
        switch (ore) {
            case EMERALD:
                // Infinite hero of the village 10
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 60, 9, true, false));

                // Check emerald requirement
                int emeraldCount = 0;
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == Material.EMERALD) {
                        emeraldCount += item.getAmount();
                    }
                }
                if (emeraldCount < 256) { // 4 stacks
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, true, false));
                }
                break;

            case AMETHYST:
                // Purple glow effect
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, true, false));
                break;

            case STONE:
                // Regeneration when standing on stone
                if (player.getLocation().getBlock().getType() == Material.STONE ||
                        player.getLocation().subtract(0, 1, 0).getBlock().getType() == Material.STONE) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false));
                }
                // Slowness when on stone
                if (player.getLocation().getBlock().getType() == Material.STONE ||
                        player.getLocation().subtract(0, 1, 0).getBlock().getType() == Material.STONE) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, false));
                }
                break;

            case DIRT:
                // Mining fatigue when wearing leather armor
                if (isWearingLeatherArmor(player)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 0, true, false));
                }
                break;
        }
    }

    private void applyPermanentOreEffects(Player player, OreType ore) {
        AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);

        switch (ore) {
            case IRON:
                // Permanent +2 Armor
                if (armorAttribute != null) {
                    armorAttribute.setBaseValue(armorAttribute.getBaseValue() + 2);
                }
                break;
        }
    }

    public void handleIronItemDrop() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = getPlayerData(player);
            if (data.getCurrentOre() == OreType.IRON) {
                // Every 10 minutes, drop a random item
                if (System.currentTimeMillis() - data.getLastIronDrop() >= 600000) { // 10 minutes
                    dropRandomItem(player);
                    data.updateIronDropTime();
                }
            }
        }
    }

    private void dropRandomItem(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        List<Integer> nonEmptySlots = new ArrayList<>();

        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                nonEmptySlots.add(i);
            }
        }

        if (!nonEmptySlots.isEmpty()) {
            int randomSlot = nonEmptySlots.get(ThreadLocalRandom.current().nextInt(nonEmptySlots.size()));
            ItemStack droppedItem = contents[randomSlot];
            player.getInventory().setItem(randomSlot, null);
            player.getWorld().dropItemNaturally(player.getLocation(), droppedItem);
            player.sendMessage(ChatColor.RED + "Your iron ore caused you to drop an item!");
        }
    }

    private boolean isWearingLeatherArmor(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        return (helmet != null && helmet.getType() == Material.LEATHER_HELMET) ||
                (chestplate != null && chestplate.getType() == Material.LEATHER_CHESTPLATE) ||
                (leggings != null && leggings.getType() == Material.LEATHER_LEGGINGS) ||
                (boots != null && boots.getType() == Material.LEATHER_BOOTS);
    }

    public boolean canUseAbility(Player player, OreType ore) {
        PlayerData data = getPlayerData(player);
        return !data.isOnCooldown(ore);
    }

    public void saveAllData() {
        // Implementation for saving player data to file/database
        // This is a placeholder - you would implement actual persistence here
        plugin.getLogger().info("Saving player data...");
    }

    public void loadPlayerData(UUID playerId) {
        // Implementation for loading player data from file/database
        // This is a placeholder - you would implement actual persistence here
    }
}