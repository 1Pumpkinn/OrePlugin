package hs.orePlugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

public class OreItemListener implements Listener {

    private final OreAbilitiesPlugin plugin;

    public OreItemListener(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerUseOreItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isValidOreInteraction(event, item)) {
            return;
        }

        // Cancel the event to prevent other interactions
        event.setCancelled(true);

        OreType newOreType = getOreTypeFromCustomItem(item);
        if (newOreType == null) {
            player.sendMessage("§cThis is not a valid ability ore!");
            return;
        }

        if (!processOreTypeChange(player, newOreType)) {
            return;
        }

        // Consume the item BEFORE applying effects
        consumeOreItem(player, item, event);
        showSuccessEffects(player, newOreType);
        updatePlayerDisplay(player);
        applyOreTypeEffectsFixed(player, newOreType);
    }

    private boolean isValidOreInteraction(PlayerInteractEvent event, ItemStack item) {
        return (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && item != null
                && item.hasItemMeta()
                && isCustomOreItem(item);
    }

    private boolean isCustomOreItem(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        return displayName.equals("Dirt Ability Ore") ||
                displayName.equals("Wood Ability Ore") ||
                displayName.equals("Stone Ability Ore") ||
                displayName.equals("Coal Ability Ore") ||
                displayName.equals("Copper Ability Ore") ||
                displayName.equals("Iron Ability Ore") ||
                displayName.equals("Gold Ability Ore") ||
                displayName.equals("Redstone Ability Ore") ||
                displayName.equals("Lapis Ability Ore") ||
                displayName.equals("Emerald Ability Ore") ||
                displayName.equals("Amethyst Ability Ore") ||
                displayName.equals("Diamond Ability Ore") ||
                displayName.equals("Netherite Ability Ore");
    }

    private OreType getOreTypeFromCustomItem(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return null;
        }

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        switch (displayName) {
            case "Dirt Ability Ore": return OreType.DIRT;
            case "Wood Ability Ore": return OreType.WOOD;
            case "Stone Ability Ore": return OreType.STONE;
            case "Coal Ability Ore": return OreType.COAL;
            case "Copper Ability Ore": return OreType.COPPER;
            case "Iron Ability Ore": return OreType.IRON;
            case "Gold Ability Ore": return OreType.GOLD;
            case "Redstone Ability Ore": return OreType.REDSTONE;
            case "Lapis Ability Ore": return OreType.LAPIS;
            case "Emerald Ability Ore": return OreType.EMERALD;
            case "Amethyst Ability Ore": return OreType.AMETHYST;
            case "Diamond Ability Ore": return OreType.DIAMOND;
            case "Netherite Ability Ore": return OreType.NETHERITE;
            default: return null;
        }
    }

    private boolean processOreTypeChange(Player player, OreType newOreType) {
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType currentOre = dataManager.getPlayerOre(player);

        if (currentOre == newOreType) {
            player.sendMessage("§cYou already have the " + newOreType.getDisplayName() + " ore ability!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        if (currentOre != null) {
            player.sendMessage("§e⚠ Replacing your current " + currentOre.getDisplayName() + " ore ability with " + newOreType.getDisplayName() + "!");
            removeOreTypeEffectsFixed(player, currentOre);
        }

        dataManager.setPlayerOre(player, newOreType);
        return true;
    }

    private void consumeOreItem(Player player, ItemStack item, PlayerInteractEvent event) {
        // Check which hand the item is in and consume it properly
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (mainHand != null && mainHand.equals(item)) {
            // Item is in main hand
            if (mainHand.getAmount() > 1) {
                mainHand.setAmount(mainHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        } else if (offHand != null && offHand.equals(item)) {
            // Item is in off hand
            if (offHand.getAmount() > 1) {
                offHand.setAmount(offHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
        }
    }

    private void showSuccessEffects(Player player, OreType oreType) {
        String oreColor = getOreColor(oreType);
        player.sendMessage("§a✓ You have unlocked the " + oreColor + oreType.getDisplayName() + " §aore ability!");
        player.sendMessage("§7Your new ability: §6" + getAbilityName(oreType));
        player.sendMessage("§7Cooldown: §b" + oreType.getCooldown() + " seconds");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
    }

    private void updatePlayerDisplay(Player player) {
        // Restart action bar to show new ore type immediately
        plugin.getActionBarManager().stopActionBar(player);
        plugin.getActionBarManager().startActionBar(player);
    }

    private void applyOreTypeEffectsFixed(Player player, OreType oreType) {
        switch (oreType) {
            case DIRT:
                checkAndApplyDirtEffects(player);
                break;
            case IRON:
                applyArmorBonus(player, 2);
                plugin.getAbilityManager().startIronDropTimer(player);
                break;
            case AMETHYST:
                // Start the glowing effect properly
                plugin.getAbilityManager().startAmethystGlowing(player);
                break;
            case EMERALD:
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 9));
                // Check emerald requirement immediately
                handleEmeraldWeakness(player);
                break;
            case STONE:
                player.sendMessage("§7Stone passive: Regeneration and slowness when standing on stone!");
                break;
            case COPPER:
                player.sendMessage("§3Copper passive: Armor takes more durability damage when hit!");
                break;
            case DIAMOND:
                player.sendMessage("§bDiamond passive: Armor takes less durability damage when hit!");
                break;
            case COAL:
                player.sendMessage("§8Coal passive: Takes damage from water and rain!");
                break;
            case NETHERITE:
                player.sendMessage("§4Netherite passive: Complete fire immunity!");
                break;
            case REDSTONE:
                player.sendMessage("§4Redstone passive: 5x damage from bees and slimes, immune to dripstone!");
                break;
            case LAPIS:
                player.sendMessage("§9Lapis passive: Free anvil enchanting and XP regeneration!");
                break;
            case GOLD:
                player.sendMessage("§eGold passive: Random durability on crafted items!");
                break;
            case WOOD:
                player.sendMessage("§eWood passive: Apples act like golden apples, axe efficiency capped at 3!");
                break;
        }
    }

    private void removeOreTypeEffectsFixed(Player player, OreType oreType) {
        switch (oreType) {
            case DIRT:
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                resetArmorAttribute(player);
                player.sendMessage("§7Dirt ore effects removed (mining fatigue cleared).");
                break;
            case IRON:
                resetArmorAttribute(player);
                plugin.getAbilityManager().cancelIronDropTimer(player);
                player.sendMessage("§7Iron ore effects removed.");
                break;
            case AMETHYST:
                plugin.getAbilityManager().cancelAmethystGlowing(player);
                player.sendMessage("§7Amethyst ore effects removed.");
                break;
            case EMERALD:
                player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
                player.removePotionEffect(PotionEffectType.WEAKNESS);
                player.sendMessage("§7Emerald ore effects removed.");
                break;
            case STONE:
                player.removePotionEffect(PotionEffectType.REGENERATION);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.sendMessage("§7Stone ore effects removed (regen/slowness cleared).");
                break;
            case COPPER:
                player.sendMessage("§7Copper ore effects removed.");
                break;
            case DIAMOND:
                player.sendMessage("§7Diamond ore effects removed.");
                break;
            case COAL:
                player.sendMessage("§7Coal ore effects removed.");
                break;
            case NETHERITE:
                player.sendMessage("§7Netherite ore effects removed.");
                break;
            case REDSTONE:
                player.sendMessage("§7Redstone ore effects removed.");
                break;
            case LAPIS:
                player.sendMessage("§7Lapis ore effects removed.");
                break;
            case GOLD:
                player.sendMessage("§7Gold ore effects removed.");
                break;
            case WOOD:
                player.sendMessage("§7Wood ore effects removed.");
                break;
        }
    }

    private void checkAndApplyDirtEffects(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean hasFullLeatherArmor = true;

        for (ItemStack piece : armor) {
            if (piece == null || !piece.getType().name().contains("LEATHER")) {
                hasFullLeatherArmor = false;
                break;
            }
        }

        AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);

        if (hasFullLeatherArmor) {
            if (armorAttribute != null) {
                armorAttribute.setBaseValue(20);
                player.sendMessage("§6Dirt blessing! Full leather armor provides diamond-level protection!");
            }

            if (player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) {
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                player.sendMessage("§aDirt blessing! Mining fatigue removed!");
            }
        } else {
            if (armorAttribute != null) {
                armorAttribute.setBaseValue(0);
            }

            if (!player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, Integer.MAX_VALUE, 0, false, false));
                player.sendMessage("§cDirt curse! Mining fatigue applied - you need full leather armor!");
            }
        }
    }

    private void handleEmeraldWeakness(Player player) {
        int emeraldCount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.EMERALD) {
                emeraldCount += item.getAmount();
            }
        }

        boolean hasEnoughEmeralds = emeraldCount >= 256; // 4 stacks of 64
        boolean hasWeakness = player.hasPotionEffect(PotionEffectType.WEAKNESS);

        if (!hasEnoughEmeralds && !hasWeakness) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 0, false, false));
            player.sendMessage("§cEmerald curse! You need at least 4 stacks of emeralds or you'll have weakness!");
        } else if (hasEnoughEmeralds && hasWeakness) {
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.sendMessage("§aEmerald blessing! Weakness removed - you have enough emeralds!");
        }
    }

    private void applyArmorBonus(Player player, double bonus) {
        if (player.getAttribute(org.bukkit.attribute.Attribute.ARMOR) != null) {
            double currentBase = player.getAttribute(org.bukkit.attribute.Attribute.ARMOR).getBaseValue();
            player.getAttribute(org.bukkit.attribute.Attribute.ARMOR).setBaseValue(currentBase + bonus);
        }
    }

    private void resetArmorAttribute(Player player) {
        if (player.getAttribute(org.bukkit.attribute.Attribute.ARMOR) != null) {
            // Reset to default armor value (0)
            player.getAttribute(org.bukkit.attribute.Attribute.ARMOR).setBaseValue(0);
        }
    }

    private String getOreColor(OreType oreType) {
        switch (oreType) {
            case DIRT: return "§6";
            case WOOD: return "§e";
            case STONE: return "§7";
            case COAL: return "§8";
            case COPPER: return "§c";
            case IRON: return "§f";
            case GOLD: return "§e";
            case REDSTONE: return "§4";
            case LAPIS: return "§9";
            case EMERALD: return "§a";
            case AMETHYST: return "§d";
            case DIAMOND: return "§b";
            case NETHERITE: return "§8";
            default: return "§7";
        }
    }

    private String getAbilityName(OreType oreType) {
        switch (oreType) {
            case DIRT: return "Earth's Blessing";
            case WOOD: return "Lumberjack's Fury";
            case STONE: return "Stone Skin";
            case COAL: return "Sizzle";
            case COPPER: return "Channel The Clouds";
            case IRON: return "Bucket Roulette";
            case GOLD: return "Goldrush";
            case REDSTONE: return "Sticky Slime";
            case LAPIS: return "Level Replenish";
            case EMERALD: return "Bring Home The Effects";
            case AMETHYST: return "Crystal Cluster";
            case DIAMOND: return "Gleaming Power";
            case NETHERITE: return "Debris, Debris, Debris";
            default: return "Unknown Ability";
        }
    }

    public static ItemStack createCustomOreItem(OreType oreType) {
        Material material = getBaseMaterial(oreType);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String oreColor = getStaticOreColor(oreType);
        String displayName = oreColor + oreType.getDisplayName() + " Ability Ore";

        meta.setDisplayName(displayName);
        meta.setLore(java.util.Arrays.asList(
                "§7Right-click to unlock this ore ability!",
                "§7Ability: §6" + getStaticAbilityName(oreType),
                "§7Cooldown: §b" + oreType.getCooldown() + " seconds",
                "§8Custom crafted ore item"
        ));

        item.setItemMeta(meta);
        return item;
    }

    private static Material getBaseMaterial(OreType oreType) {
        switch (oreType) {
            case DIRT: return Material.DIRT;
            case WOOD: return Material.OAK_LOG;
            case STONE: return Material.STONE;
            case COAL: return Material.COAL;
            case COPPER: return Material.COPPER_INGOT;
            case IRON: return Material.IRON_INGOT;
            case GOLD: return Material.GOLD_INGOT;
            case REDSTONE: return Material.REDSTONE;
            case LAPIS: return Material.LAPIS_LAZULI;
            case EMERALD: return Material.EMERALD;
            case AMETHYST: return Material.AMETHYST_SHARD;
            case DIAMOND: return Material.DIAMOND;
            case NETHERITE: return Material.NETHERITE_INGOT;
            default: return Material.STONE;
        }
    }

    private static String getStaticOreColor(OreType oreType) {
        switch (oreType) {
            case DIRT: return "§6";
            case WOOD: return "§e";
            case STONE: return "§7";
            case COAL: return "§8";
            case COPPER: return "§c";
            case IRON: return "§f";
            case GOLD: return "§e";
            case REDSTONE: return "§4";
            case LAPIS: return "§9";
            case EMERALD: return "§a";
            case AMETHYST: return "§d";
            case DIAMOND: return "§b";
            case NETHERITE: return "§8";
            default: return "§7";
        }
    }

    private static String getStaticAbilityName(OreType oreType) {
        switch (oreType) {
            case DIRT: return "Earth's Blessing";
            case WOOD: return "Lumberjack's Fury";
            case STONE: return "Stone Skin";
            case COAL: return "Sizzle";
            case COPPER: return "Channel The Clouds";
            case IRON: return "Bucket Roulette";
            case GOLD: return "Goldrush";
            case REDSTONE: return "Sticky Slime";
            case LAPIS: return "Level Replenish";
            case EMERALD: return "Bring Home The Effects";
            case AMETHYST: return "Crystal Cluster";
            case DIAMOND: return "Gleaming Power";
            case NETHERITE: return "Debris, Debris, Debris";
            default: return "Unknown Ability";
        }
    }
}