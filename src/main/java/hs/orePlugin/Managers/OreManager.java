package hs.orePlugin.Managers;

import hs.orePlugin.OrePlugin;
import hs.orePlugin.OreType;
import hs.orePlugin.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.concurrent.ThreadLocalRandom;

public class OreManager {
    private final OrePlugin plugin;

    public OreManager(OrePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean useOreAbility(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        OreType ore = data.getCurrentOre();

        if (ore == null) {
            player.sendMessage(ChatColor.RED + "You don't have an ore assigned!");
            return false;
        }

        if (data.isOnCooldown(ore)) {
            long remaining = data.getRemainingCooldown(ore);
            player.sendMessage(ChatColor.RED + "Ability on cooldown for " + (remaining / 1000) + " seconds!");
            return false;
        }

        // Execute ore ability
        executeOreAbility(player, ore, data);
        data.setCooldown(ore);

        player.sendMessage(ChatColor.GREEN + "Used ability: " + ore.getColor() + ore.getAbilityName());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        return true;
    }

    private void executeOreAbility(Player player, OreType ore, PlayerData data) {
        switch (ore) {
            case DIRT:
                // If standing on grass or dirt, get +2 hearts for 15 seconds
                Material blockBelow = player.getLocation().subtract(0, 1, 0).getBlock().getType();
                if (blockBelow == Material.GRASS_BLOCK || blockBelow == Material.DIRT) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 300, 1)); // 15 seconds, +2 hearts
                    data.setTemporaryData("heartBoostEnd", System.currentTimeMillis() + 15000);
                    player.sendMessage(ChatColor.GREEN + "You feel the earth's strength!");
                } else {
                    player.sendMessage(ChatColor.RED + "You must be standing on grass or dirt!");
                }
                break;

            case WOOD:
                // Axes deal 1.5x damage for 5 seconds
                data.setTemporaryData("axeDamageEnd", System.currentTimeMillis() + 5000);
                player.sendMessage(ChatColor.GREEN + "Your axes feel sharper!");
                break;

            case STONE:
                // Give resistance 1 for 10 seconds
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0));
                data.setTemporaryData("resistanceEnd", System.currentTimeMillis() + 10000);
                break;

            case COAL:
                // Smelt ore in hand
                smeltOreInHand(player);
                break;

            case COPPER:
                // For 10 seconds, all players hit get struck by lightning
                data.setTemporaryData("channelingEnd", System.currentTimeMillis() + 10000);
                player.sendMessage(ChatColor.YELLOW + "Lightning courses through your veins!");
                break;

            case IRON:
                // Give random bucket
                giveRandomBucket(player);
                break;

            case GOLD:
                // Haste 5 and Speed 3 for 10 seconds
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 200, 4));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2));
                data.setTemporaryData("goldRushEnd", System.currentTimeMillis() + 10000);
                break;

            case REDSTONE:
                // Next player hit cannot jump for 10 seconds
                data.setTemporaryData("stickySlimeActive", true);
                data.setTemporaryData("stickySlimeEnd", System.currentTimeMillis() + 10000);
                player.sendMessage(ChatColor.RED + "Your next hit will ground your target!");
                break;

            case LAPIS:
                // Splashing exp gives regeneration for next 30 seconds
                data.setTemporaryData("expRegenEnd", System.currentTimeMillis() + 30000);
                player.sendMessage(ChatColor.BLUE + "Experience will now heal you!");
                break;

            case EMERALD:
                // Get every beacon effect at level 1 for 20 seconds
                applyBeaconEffects(player);
                data.setTemporaryData("beaconEffectsEnd", System.currentTimeMillis() + 20000);
                break;

            case AMETHYST:
                // Crystal mode - no knockback and no damage for 10 seconds
                data.setCrystalMode(true, 10000);
                player.sendMessage(ChatColor.LIGHT_PURPLE + "You are now in crystal mode!");
                break;

            case DIAMOND:
                // If using diamond sword, do 2x damage for 5 seconds
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                if (mainHand.getType() == Material.DIAMOND_SWORD) {
                    data.setTemporaryData("diamondDamageEnd", System.currentTimeMillis() + 5000);
                    player.sendMessage(ChatColor.AQUA + "Your diamond sword gleams with power!");
                } else {
                    player.sendMessage(ChatColor.RED + "You must be holding a diamond sword!");
                }
                break;

            case NETHERITE:
                // Upgrade item in hand to netherite
                upgradeToNetherite(player);
                break;
        }
    }

    private void smeltOreInHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must be holding an ore!");
            return;
        }

        Material smelted = getSmeltedVersion(item.getType());
        if (smelted == null) {
            player.sendMessage(ChatColor.RED + "That item cannot be smelted!");
            return;
        }

        int amount = item.getAmount();
        player.getInventory().setItemInMainHand(new ItemStack(smelted, amount));
        player.sendMessage(ChatColor.GREEN + "Smelted " + amount + " " + item.getType().name().toLowerCase() + "!");
    }

    private Material getSmeltedVersion(Material material) {
        switch (material) {
            case IRON_ORE: return Material.IRON_INGOT;
            case GOLD_ORE: return Material.GOLD_INGOT;
            case COPPER_ORE: return Material.COPPER_INGOT;
            case ANCIENT_DEBRIS: return Material.NETHERITE_SCRAP;
            case COBBLESTONE: return Material.STONE;
            case SAND: return Material.GLASS;
            case RAW_IRON: return Material.IRON_INGOT;
            case RAW_GOLD: return Material.GOLD_INGOT;
            case RAW_COPPER: return Material.COPPER_INGOT;
            default: return null;
        }
    }

    private void giveRandomBucket(Player player) {
        Material[] buckets = {Material.BUCKET, Material.WATER_BUCKET, Material.LAVA_BUCKET};
        Material randomBucket = buckets[ThreadLocalRandom.current().nextInt(buckets.length)];

        ItemStack bucket = new ItemStack(randomBucket);
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(bucket);
            player.sendMessage(ChatColor.WHITE + "You received a " + randomBucket.name().toLowerCase().replace("_", " ") + "!");
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), bucket);
            player.sendMessage(ChatColor.WHITE + "A " + randomBucket.name().toLowerCase().replace("_", " ") + " was dropped near you!");
        }
    }

    private void applyBeaconEffects(Player player) {
        // All beacon effects at level 1 for 20 seconds
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 400, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 400, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 400, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 400, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 400, 0));
        player.sendMessage(ChatColor.GREEN + "You feel the power of all beacons!");
    }

    private void upgradeToNetherite(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must be holding an item!");
            return;
        }

        Material netheriteVersion = getNetheriteVersion(item.getType());
        if (netheriteVersion == null) {
            player.sendMessage(ChatColor.RED + "That item cannot be upgraded to netherite!");
            return;
        }

        ItemStack netheriteItem = new ItemStack(netheriteVersion);

        // Copy enchantments and other metadata
        if (item.hasItemMeta()) {
            ItemMeta oldMeta = item.getItemMeta();
            ItemMeta newMeta = netheriteItem.getItemMeta();

            if (oldMeta.hasEnchants()) {
                for (Enchantment ench : oldMeta.getEnchants().keySet()) {
                    newMeta.addEnchant(ench, oldMeta.getEnchantLevel(ench), true);
                }
            }

            if (oldMeta.hasDisplayName()) {
                newMeta.setDisplayName(oldMeta.getDisplayName());
            }

            if (oldMeta.hasLore()) {
                newMeta.setLore(oldMeta.getLore());
            }

            netheriteItem.setItemMeta(newMeta);
        }

        player.getInventory().setItemInMainHand(netheriteItem);
        player.sendMessage(ChatColor.DARK_PURPLE + "Upgraded to netherite!");
    }

    private Material getNetheriteVersion(Material material) {
        switch (material) {
            case DIAMOND_SWORD: return Material.NETHERITE_SWORD;
            case DIAMOND_PICKAXE: return Material.NETHERITE_PICKAXE;
            case DIAMOND_AXE: return Material.NETHERITE_AXE;
            case DIAMOND_SHOVEL: return Material.NETHERITE_SHOVEL;
            case DIAMOND_HOE: return Material.NETHERITE_HOE;
            case DIAMOND_HELMET: return Material.NETHERITE_HELMET;
            case DIAMOND_CHESTPLATE: return Material.NETHERITE_CHESTPLATE;
            case DIAMOND_LEGGINGS: return Material.NETHERITE_LEGGINGS;
            case DIAMOND_BOOTS: return Material.NETHERITE_BOOTS;
            default: return null;
        }
    }

    public boolean attemptCraft(Player player, OreType oreType) {
        // 25% chance to shatter and not give ability
        if (ThreadLocalRandom.current().nextDouble() < 0.25) {
            player.sendMessage(ChatColor.RED + "The " + oreType.getDisplayName().toLowerCase() + " shattered! Crafting failed!");
            return false;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        data.setCurrentOre(oreType);

        player.sendMessage(ChatColor.GREEN + "Successfully crafted " + oreType.getColor() +
                oreType.getDisplayName() + ChatColor.GREEN + " ore ability!");

        return true;
    }
}