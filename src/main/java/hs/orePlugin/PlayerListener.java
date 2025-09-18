package hs.orePlugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.Action;
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
    private final Map<UUID, Long> emeraldWeaknessCheck = new HashMap<>();
    private final Map<UUID, Long> dirtArmorCheck = new HashMap<>();
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

            AbilityActivationManager activationManager = plugin.getActivationManager();
            if (activationManager.isBedrockMode(player)) {
                player.sendMessage("§eUse §6/ability §eto activate your abilities!");
            } else {
                player.sendMessage("§eUse §6Shift + Right-click §eto activate abilities!");
                player.sendMessage("§7Use §6/bedrock §efor Bedrock Edition support");
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                applyAllPassiveEffectsFixed(player);
                plugin.getAbilityManager().restartPlayerTimers(player);
            }
        }.runTaskLater(plugin, 5); // Small delay to ensure player is fully loaded

        plugin.getActionBarManager().startActionBar(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getActionBarManager().stopActionBar(player);
        plugin.getAbilityManager().cleanup(player);
        plugin.getAbilityListener().cleanup(player);


        emeraldWeaknessCheck.remove(player.getUniqueId());
        dirtArmorCheck.remove(player.getUniqueId());
        lastMoveCheck.remove(player.getUniqueId());
        wasOnStone.remove(player.getUniqueId());
    }

    public void cleanupPlayerEffects(Player player, OreType oreType) {
        UUID uuid = player.getUniqueId();

        // Clean up tracking for all ore types
        emeraldWeaknessCheck.remove(uuid);
        dirtArmorCheck.remove(uuid);
        wasOnStone.remove(uuid);
        lastMoveCheck.remove(uuid);

        // Remove specific effects based on ore type
        switch (oreType) {
            case DIRT:
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                // Reset armor attribute
                AttributeInstance armor = player.getAttribute(Attribute.ARMOR);
                if (armor != null) {
                    armor.setBaseValue(0);
                }
                plugin.getLogger().info("Cleaned up dirt effects for " + player.getName());
                break;
            case STONE:
                player.removePotionEffect(PotionEffectType.REGENERATION);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                plugin.getLogger().info("Cleaned up stone effects for " + player.getName());
                break;
            case EMERALD:
                player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
                player.removePotionEffect(PotionEffectType.WEAKNESS);
                plugin.getLogger().info("Cleaned up emerald effects for " + player.getName());
                break;
            case AMETHYST:
                player.removePotionEffect(PotionEffectType.GLOWING);
                plugin.getLogger().info("Cleaned up amethyst effects for " + player.getName());
                break;
            case IRON:
                // Reset armor attribute
                AttributeInstance ironArmor = player.getAttribute(Attribute.ARMOR);
                if (ironArmor != null) {
                    double current = ironArmor.getBaseValue();
                    double newValue = Math.max(0.0, current - 2.0);
                    ironArmor.setBaseValue(newValue);
                    player.sendMessage("§7Iron armor bonus removed: -1 armor bar");
                    plugin.getLogger().info("Removed iron armor bonus from " + player.getName() +
                            " - Armor: " + current + " -> " + newValue);
                }
                plugin.getLogger().info("Cleaned up iron effects for " + player.getName());
                break;
            case NETHERITE:
                // Fire resistance is handled directly in AbilityListener damage events
                plugin.getLogger().info("Cleaned up netherite effects for " + player.getName());
                break;
            case COPPER:
                // FIXED: No timer to clean up - just log
                plugin.getLogger().info("Cleaned up copper effects for " + player.getName());
                break;
            case DIAMOND:
                // FIXED: No timer to clean up - just log
                plugin.getLogger().info("Cleaned up diamond effects for " + player.getName());
                break;
            default:
                plugin.getLogger().info("No specific cleanup needed for " + oreType.name());
                break;
        }
        player.sendMessage("§7Cleaned up " + oreType.getDisplayName() + " ore tracking data.");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType != OreType.NETHERITE) {
            return;
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType() != Material.ANCIENT_DEBRIS) {
            return;
        }

        // Convert Ancient Debris to Netherite Ingots
        int amount = handItem.getAmount();
        ItemStack netheriteIngots = new ItemStack(Material.NETHERITE_INGOT, amount);

        player.getInventory().setItemInMainHand(netheriteIngots);
        player.sendMessage("§4Converted " + amount + " Ancient Debris to Netherite Ingots!");
        player.playSound(player.getLocation(), Sound.UI_STONECUTTER_TAKE_RESULT, 1.0f, 0.5f);

        // Prevent block breaking if clicking on a block
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
        }
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

        // Check emerald weakness and dirt armor effects periodically
        if (oreType == OreType.EMERALD) {
            handleEmeraldWeakness(player);
        }

        if (oreType == OreType.DIRT) {
            handleDirtArmorCheck(player);
        }
    }

    // FIXED: Handle dirt armor checking with proper timing
    private void handleDirtArmorCheck(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check every 3 seconds to avoid spam
        if (!dirtArmorCheck.containsKey(uuid) ||
                currentTime - dirtArmorCheck.get(uuid) > 3000) {

            dirtArmorCheck.put(uuid, currentTime);
            checkAndApplyDirtEffects(player);
        }
    }

    // FIXED: Complete dirt effects implementation
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
            // Give diamond-level armor protection (20 armor points)
            if (armorAttribute != null && armorAttribute.getBaseValue() < 20) {
                armorAttribute.setBaseValue(20);
                player.sendMessage("§6Dirt blessing! Full leather armor provides diamond-level protection!");
            }

            // Remove mining fatigue if present
            if (player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) {
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                player.sendMessage("§aDirt blessing! Mining fatigue removed with full leather armor!");
            }
        } else {
            // Reset armor to normal leather protection
            if (armorAttribute != null && armorAttribute.getBaseValue() > 7) {
                armorAttribute.setBaseValue(0); // Let armor calculate normally
                player.sendMessage("§cDirt curse! Lost diamond-level protection without full leather armor!");
            }

            // Apply mining fatigue
            if (!player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, Integer.MAX_VALUE, 0, false, false));
                player.sendMessage("§cMining fatigue applied - you need full leather armor!");
            }
        }
    }

    private void handleEmeraldWeakness(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check every 5 seconds
        if (!emeraldWeaknessCheck.containsKey(uuid) ||
                currentTime - emeraldWeaknessCheck.get(uuid) > 5000) {

            emeraldWeaknessCheck.put(uuid, currentTime);

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
                player.sendMessage("§cYou need at least 4 stacks of emeralds");
            } else if (hasEnoughEmeralds && hasWeakness) {
                player.removePotionEffect(PotionEffectType.WEAKNESS);
                player.sendMessage("§aYou have enough emeralds!");
            }
        }
    }

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
        }

        // Gold ore - golden apples give 2x absorption
        if (oreType == OreType.GOLD && item.getType() == Material.GOLDEN_APPLE) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1)); // Extra absorption
                }
            }.runTaskLater(plugin, 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == null) return;

        Material blockType = event.getBlock().getType();

        // Diamond ore downside - 50% chance to not drop ore
        if (oreType == OreType.DIAMOND && isOre(blockType)) {
            if (random.nextDouble() < 0.5) {
                event.setDropItems(false);
                player.sendMessage("§cDiamond downside prevented ore drop!");
            }
        }

        // REMOVED: Netherite automatic ancient debris conversion on mining
        // Now handled by shift+left-click interaction instead
    }

    // FIXED: Handle armor changes for dirt ore
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        // Check for armor slot changes for dirt ore
        if (oreType == OreType.DIRT && isArmorSlot(event.getSlot())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    checkAndApplyDirtEffects(player);
                }
            }.runTaskLater(plugin, 1);
        }
    }

    private boolean isArmorSlot(int slot) {
        // Armor slots in player inventory are 36-39 (boots, leggings, chestplate, helmet)
        return slot >= 36 && slot <= 39;
    }

    private void handleMovementEffects(Player player, OreType oreType, Material blockBelow) {
        UUID uuid = player.getUniqueId();

        switch (oreType) {
            case STONE:
                boolean isOnStone = isStoneType(blockBelow);
                Boolean wasOnStonePreviously = wasOnStone.get(uuid);

                if (isOnStone && (wasOnStonePreviously == null || !wasOnStonePreviously)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0));
                    player.sendMessage("§7Stone Skin activated! Regeneration while on stone.");
                } else if (!isOnStone && wasOnStonePreviously != null && wasOnStonePreviously) {
                    player.removePotionEffect(PotionEffectType.REGENERATION);
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                    player.sendMessage("§7Stone Skin has ended.");
                }

                wasOnStone.put(uuid, isOnStone);
                break;
        }
    }


    private void applyAllPassiveEffectsFixed(Player player) {
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);
        if (oreType == null) return;

        switch (oreType) {
            case DIRT:
                // Apply dirt effects immediately
                checkAndApplyDirtEffects(player);
                break;

            case IRON:
                AttributeInstance armor = player.getAttribute(Attribute.ARMOR);
                if (armor != null) {
                    double currentBase = armor.getBaseValue();
                    armor.setBaseValue(currentBase + 2.0);
                    player.sendMessage("§7Iron passive: +1 armor bar applied!");
                    plugin.getLogger().info("Applied iron armor bonus to " + player.getName() +
                            " - Base armor: " + currentBase + " -> " + (currentBase + 2.0));
                }
                plugin.getAbilityManager().startIronDropTimer(player);
                break;

            case NETHERITE:
                player.sendMessage("§4Netherite passive: Complete fire immunity!");
                break;

            case AMETHYST:
                // Start persistent glowing effect immediately with proper method call
                plugin.getAbilityManager().startAmethystGlowing(player);
                break;

            case EMERALD:
                // FIXED: Apply Hero of the Village level 10 (effect level 9 = display level 10)
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 9, false, false));
                player.sendMessage("§aEmerald passive: Hero of the Village 10 applied!");
                // Initial emerald weakness check
                handleEmeraldWeakness(player);
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

            case REDSTONE:
            case LAPIS:
            case GOLD:
            case WOOD:
            case STONE:
                break;
        }
    }

    private boolean isStoneType(Material material) {
        return material == Material.STONE || material == Material.COBBLESTONE ||
                material == Material.STONE_BRICKS || material == Material.DEEPSLATE ||
                material == Material.COBBLED_DEEPSLATE || material.name().contains("STONE");
    }

    private boolean isOre(Material material) {
        return material.name().contains("_ORE") || material.name().contains("RAW_") ||
                material == Material.ANCIENT_DEBRIS;
    }
}