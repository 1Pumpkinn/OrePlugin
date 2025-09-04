package hs.orePlugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class PlayerListener implements Listener {

    private final OreAbilitiesPlugin plugin;
    private final Map<UUID, Long> lastMoveCheck = new HashMap<>();
    private final Map<UUID, Boolean> wasOnStone = new HashMap<>();
    private final Random random = new Random();

    public PlayerListener(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();

        if (!dataManager.hasPlayerOre(player)) {
            dataManager.assignRandomStarterOre(player);
            OreType oreType = dataManager.getPlayerOre(player);
            player.sendMessage("§6Welcome! You have been assigned the " + oreType.getDisplayName() + " ore type!");

            // Show activation methods based on mode
            AbilityActivationManager activationManager = plugin.getActivationManager();
            if (activationManager.isBedrockMode(player)) {
                player.sendMessage("§eUse §6/ability §eto activate your abilities!");
            } else {
                player.sendMessage("§eUse §6Shift + Right-click §eto activate abilities!");
                player.sendMessage("§7Use §6/bedrock §efor Bedrock Edition support");
            }
        }

        // Apply passive effects based on ore type
        applyPassiveEffects(player);

        // Start iron drop timer if player has iron ore
        if (dataManager.getPlayerOre(player) == OreType.IRON) {
            dataManager.setIronDropTimer(player);
        }

        // Start action bar for the player
        plugin.getActionBarManager().startActionBar(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Stop action bar display
        plugin.getActionBarManager().stopActionBar(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Throttle movement checks to every 0.5 seconds
        long currentTime = System.currentTimeMillis();
        if (lastMoveCheck.containsKey(uuid) && currentTime - lastMoveCheck.get(uuid) < 500) {
            return;
        }
        lastMoveCheck.put(uuid, currentTime);

        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);
        if (oreType == null) return;

        Location loc = player.getLocation();
        Block blockBelow = loc.subtract(0, 1, 0).getBlock();
        loc.add(0, 1, 0); // Reset location

        handleMovementEffects(player, oreType, blockBelow.getType());
    }

    // REMOVED onPlayerInteract method - now handled by AbilityActivationManager

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == null) return;

        ItemStack item = event.getItem();

        // Wood ore - apples act like golden apples
        if (oreType == OreType.WOOD && item.getType() == Material.APPLE) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0));
            player.sendMessage("§6Apple enhanced by Wood ore!");
        }

        // Gold ore - golden apples give 2x absorption
        if (oreType == OreType.GOLD && item.getType() == Material.GOLDEN_APPLE) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1)); // Extra absorption
                }
            }.runTaskLater(plugin, 1); // Slight delay to apply after normal golden apple effect
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == null) return;

        // Diamond ore downside - 50% chance to not drop ore
        if (oreType == OreType.DIAMOND && isOre(event.getBlock().getType())) {
            if (random.nextDouble() < 0.5) {
                event.setDropItems(false);
                player.sendMessage("§cDiamond curse prevented ore drop!");
            }
        }

        // Check for iron ore item drop timer
        if (oreType == OreType.IRON && dataManager.shouldDropIronItem(player)) {
            dropRandomInventoryItem(player);
        }
    }

    private void handleMovementEffects(Player player, OreType oreType, Material blockBelow) {
        UUID uuid = player.getUniqueId();

        switch (oreType) {
            case STONE:
                boolean isOnStone = isStoneType(blockBelow);
                Boolean wasOnStonePreviously = wasOnStone.get(uuid);

                if (isOnStone && (wasOnStonePreviously == null || !wasOnStonePreviously)) {
                    // Just stepped on stone - apply effects
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0));
                } else if (!isOnStone && wasOnStonePreviously != null && wasOnStonePreviously) {
                    // Just stepped off stone - remove effects
                    player.removePotionEffect(PotionEffectType.REGENERATION);
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                }

                wasOnStone.put(uuid, isOnStone);
                break;
        }
    }

    private void applyPassiveEffects(Player player) {
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);
        if (oreType == null) return;

        switch (oreType) {
            case IRON:
                // Permanent +2 armor attribute
                AttributeInstance armor = player.getAttribute(Attribute.ARMOR);
                if (armor != null) {
                    armor.setBaseValue(armor.getBaseValue() + 2);
                }
                break;

            case NETHERITE:
                // No fire damage
                player.setFireTicks(0);
                break;

            case AMETHYST:
                // Permanent glowing (purple)
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
                break;

            case EMERALD:
                // Infinite hero of the village
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 9));
                break;
        }
    }

    private boolean isStoneType(Material material) {
        return material == Material.STONE || material == Material.COBBLESTONE ||
                material == Material.STONE_BRICKS || material == Material.DEEPSLATE ||
                material == Material.COBBLED_DEEPSLATE || material.name().contains("STONE");
    }

    private boolean isOre(Material material) {
        return material.name().contains("_ORE") || material.name().contains("RAW_");
    }

    private void dropRandomInventoryItem(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = contents.length - 1; i >= 0; i--) { // Start from end to avoid hotbar
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                ItemStack toDrop = contents[i].clone();
                toDrop.setAmount(1);

                player.getWorld().dropItemNaturally(player.getLocation(), toDrop);

                if (contents[i].getAmount() > 1) {
                    contents[i].setAmount(contents[i].getAmount() - 1);
                } else {
                    contents[i] = null;
                }

                player.getInventory().setContents(contents);
                player.sendMessage("§cIron curse! An item was dropped from your inventory!");
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.5f);
                return;
            }
        }
    }
}