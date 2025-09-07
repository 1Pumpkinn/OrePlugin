package hs.orePlugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryClickEvent;
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

        // FIXED: Apply ALL passive effects immediately upon join with proper method calls
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
        plugin.getAbilityListener().cleanup(player);  // FIXED: Also cleanup AbilityListener
        emeraldWeaknessCheck.remove(player.getUniqueId());
        dirtArmorCheck.remove(player.getUniqueId());
        lastMoveCheck.remove(player.getUniqueId());
        wasOnStone.remove(player.getUniqueId());
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
                player.sendMessage("§cDirt curse! Mining fatigue applied - you need full leather armor!");
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
                player.sendMessage("§cEmerald curse! You need at least 4 stacks of emeralds or you'll have weakness!");
            } else if (hasEnoughEmeralds && hasWeakness) {
                player.removePotionEffect(PotionEffectType.WEAKNESS);
                player.sendMessage("§aEmerald blessing! Weakness removed - you have enough emeralds!");
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
            player.sendMessage("§6Apple enhanced by Wood ore!");
        }

        // Gold ore - golden apples give 2x absorption
        if (oreType == OreType.GOLD && item.getType() == Material.GOLDEN_APPLE) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1)); // Extra absorption
                    player.sendMessage("§6Gold ore enhanced your golden apple!");
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
                player.sendMessage("§cDiamond curse prevented ore drop!");
            }
        }

        // Netherite upside - ancient debris turns into netherite ingots
        if (oreType == OreType.NETHERITE && blockType == Material.ANCIENT_DEBRIS) {
            event.setDropItems(false);
            ItemStack netheriteIngot = new ItemStack(Material.NETHERITE_INGOT, 1);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), netheriteIngot);
            player.sendMessage("§4Netherite blessing! Ancient debris became a netherite ingot!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 1.0f, 0.5f);
        }
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
                    player.sendMessage("§7Stone blessing activated! Regeneration while on stone.");
                } else if (!isOnStone && wasOnStonePreviously != null && wasOnStonePreviously) {
                    player.removePotionEffect(PotionEffectType.REGENERATION);
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                    player.sendMessage("§7Stone blessing ended.");
                }

                wasOnStone.put(uuid, isOnStone);
                break;
        }
    }

    // FIXED: Apply ALL passive effects immediately and correctly with proper method calls
    private void applyAllPassiveEffectsFixed(Player player) {
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);
        if (oreType == null) return;

        player.sendMessage("§eApplying " + oreType.getDisplayName() + " ore effects...");

        switch (oreType) {
            case DIRT:
                // Apply dirt effects immediately
                checkAndApplyDirtEffects(player);
                player.sendMessage("§6Dirt ore effects applied! Full leather armor = diamond protection, else mining fatigue!");
                break;

            case IRON:
                // Permanent +2 armor attribute
                AttributeInstance armor = player.getAttribute(Attribute.ARMOR);
                if (armor != null) {
                    armor.setBaseValue(armor.getBaseValue() + 2);
                }
                // FIXED: Start the iron drop timer properly
                plugin.getAbilityManager().startIronDropTimer(player);
                player.sendMessage("§fIron ore effects applied! +2 armor and random item drops!");
                break;

            case NETHERITE:
                // No fire damage
                player.setFireTicks(0);
                player.sendMessage("§4Netherite ore effects applied! Fire immunity!");
                break;

            case AMETHYST:
                // FIXED: Start persistent glowing effect immediately with proper method call
                plugin.getAbilityManager().startAmethystGlowing(player);
                player.sendMessage("§dAmethyst ore effects applied! Permanent purple glowing!");
                break;

            case EMERALD:
                // Infinite hero of the village AND check emerald requirement
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 9, false, false));
                // Initial emerald weakness check
                handleEmeraldWeakness(player);
                player.sendMessage("§aEmerald ore effects applied! Hero of village + emerald requirement!");
                break;

            case COPPER:
                // FIXED: Start copper armor durability timer
                plugin.getAbilityListener().startCopperArmorDurabilityTimer(player);
                player.sendMessage("§cCopper ore effects applied! Lightning immunity + armor breaks 2x faster!");
                break;

            case DIAMOND:
                // FIXED: Start diamond armor protection timer
                plugin.getAbilityListener().startDiamondArmorProtectionTimer(player);
                player.sendMessage("§bDiamond ore effects applied! Armor lasts 2x longer + 50% ore drop fail!");
                break;

            case COAL:
                player.sendMessage("§8Coal ore effects applied! +1 fire damage + water damage!");
                break;

            case REDSTONE:
                player.sendMessage("§4Redstone ore effects applied! Dripstone immunity + 5x bee/slime damage!");
                break;

            case LAPIS:
                player.sendMessage("§9Lapis ore effects applied! Free anvils + no villager trading!");
                break;

            case GOLD:
                player.sendMessage("§eGold ore effects applied! 2x golden apple absorption + random durability!");
                break;

            case WOOD:
                player.sendMessage("§eWood ore effects applied! Apples = golden apples!");
                break;

            case STONE:
                player.sendMessage("§7Stone ore effects applied! Regeneration on stone blocks!");
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