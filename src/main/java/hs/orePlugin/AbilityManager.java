package hs.orePlugin;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.inventory.EquipmentSlot;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class AbilityManager {

    private final OreAbilitiesPlugin plugin;
    private final Map<UUID, Boolean> activeEffects = new HashMap<>();
    private final Random random = new Random();

    public AbilityManager(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean useAbility(Player player) {
        PlayerDataManager dataManager = plugin.getPlayerDataManager();

        if (dataManager.isOnCooldown(player)) {
            long remaining = dataManager.getRemainingCooldown(player);
            player.sendMessage("§cAbility on cooldown! " + remaining + " seconds remaining.");
            return false;
        }

        OreType oreType = dataManager.getPlayerOre(player);
        if (oreType == null) {
            player.sendMessage("§cYou don't have an ore type assigned!");
            return false;
        }

        executeAbility(player, oreType);
        dataManager.setCooldown(player, oreType.getCooldown());

        // Update action bar immediately
        plugin.getActionBarManager().updateCooldownDisplay(player);

        return true;
    }

    private void executeAbility(Player player, OreType oreType) {
        switch (oreType) {
            case DIRT:
                dirtAbility(player);
                break;
            case WOOD:
                woodAbility(player);
                break;
            case STONE:
                stoneAbility(player);
                break;
            case COAL:
                coalAbility(player);
                break;
            case COPPER:
                copperAbility(player);
                break;
            case IRON:
                ironAbility(player);
                break;
            case GOLD:
                goldAbility(player);
                break;
            case REDSTONE:
                redstoneAbility(player);
                break;
            case LAPIS:
                lapisAbility(player);
                break;
            case EMERALD:
                emeraldAbility(player);
                break;
            case AMETHYST:
                amethystAbility(player);
                break;
            case DIAMOND:
                diamondAbility(player);
                break;
            case NETHERITE:
                netheriteAbility(player);
                break;
        }
    }

    private void dirtAbility(Player player) {
        Location loc = player.getLocation();
        Material below = loc.subtract(0, 1, 0).getBlock().getType();
        loc.add(0, 1, 0); // Reset location

        if (below == Material.GRASS_BLOCK || below == Material.DIRT) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300, 1)); // +2 hearts for 15s
            player.sendMessage("§aDirt ability activated! +2 hearts for 15 seconds!");
            player.playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0f, 1.0f);
        } else {
            player.sendMessage("§cYou must be standing on grass or dirt!");
        }
    }

    private void woodAbility(Player player) {
        activeEffects.put(player.getUniqueId(), true);
        player.sendMessage("§6Wood ability activated! Axes deal 1.5x damage for 5 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                activeEffects.remove(player.getUniqueId());
                player.sendMessage("§eWood ability effect ended.");
            }
        }.runTaskLater(plugin, 100); // 5 seconds
    }

    private void stoneAbility(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0)); // Resistance 1 for 10s
        player.sendMessage("§7Stone ability activated! Resistance 1 for 10 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
    }

    private void coalAbility(Player player) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType() == Material.AIR) {
            player.sendMessage("§cYou must hold an item to smelt!");
            return;
        }

        Material smeltResult = getSmeltResult(handItem.getType());
        if (smeltResult == null) {
            player.sendMessage("§cThis item cannot be smelted!");
            return;
        }

        int amount = handItem.getAmount();
        player.getInventory().setItemInMainHand(new ItemStack(smeltResult, amount));
        player.sendMessage("§6Sizzle! Smelted " + amount + " items!");
        player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1.0f, 1.0f);
    }

    private void copperAbility(Player player) {
        activeEffects.put(player.getUniqueId(), true);
        player.sendMessage("§3Channel The Clouds activated! Lightning strikes on hit for 10 seconds!");
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                activeEffects.remove(player.getUniqueId());
                player.sendMessage("§bChannel The Clouds effect ended.");
            }
        }.runTaskLater(plugin, 200); // 10 seconds
    }

    private void ironAbility(Player player) {
        Material[] buckets = {Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.BUCKET};
        Material chosen = buckets[random.nextInt(buckets.length)];

        ItemStack bucket = new ItemStack(chosen);
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(bucket);
            player.sendMessage("§fBucket Roulette! You received a " + chosen.name().toLowerCase() + "!");
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0f, 1.0f);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), bucket);
            player.sendMessage("§fBucket Roulette! Bucket dropped (inventory full)!");
        }
    }

    private void goldAbility(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 200, 4)); // Haste 5 for 10s
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2)); // Speed 3 for 10s
        player.sendMessage("§eGoldrush activated! Haste 5 and Speed 3 for 10 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_METAL_BREAK, 1.0f, 1.0f);
    }

    private void redstoneAbility(Player player) {
        activeEffects.put(player.getUniqueId(), true);
        player.sendMessage("§4Sticky Slime activated! Next hit prevents jumping for 10 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                activeEffects.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, 1200); // Effect lasts until used or 60 seconds pass
    }

    private void lapisAbility(Player player) {
        activeEffects.put(player.getUniqueId(), true);
        player.sendMessage("§9Level Replenish activated! EXP splashing gives regen for 30 seconds!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                activeEffects.remove(player.getUniqueId());
                player.sendMessage("§bLevel Replenish effect ended.");
            }
        }.runTaskLater(plugin, 600); // 30 seconds
    }

    private void emeraldAbility(Player player) {
        // All beacon effects level 1 for 20 seconds
        PotionEffectType[] beaconEffects = {
                PotionEffectType.SPEED, PotionEffectType.HASTE, PotionEffectType.RESISTANCE,
                PotionEffectType.JUMP_BOOST, PotionEffectType.STRENGTH, PotionEffectType.REGENERATION
        };

        for (PotionEffectType effect : beaconEffects) {
            player.addPotionEffect(new PotionEffect(effect, 400, 0));
        }

        player.sendMessage("§aBring Home The Effects! All beacon effects for 20 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    private void amethystAbility(Player player) {
        activeEffects.put(player.getUniqueId(), true);
        player.sendMessage("§dCrystal Cluster activated! No knockback and no damage for 10 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                activeEffects.remove(player.getUniqueId());
                player.sendMessage("§5Crystal Cluster effect ended.");
            }
        }.runTaskLater(plugin, 200); // 10 seconds
    }

    private void diamondAbility(Player player) {
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon != null && weapon.getType() == Material.DIAMOND_SWORD) {
            activeEffects.put(player.getUniqueId(), true);
            player.sendMessage("§bGleaming Power activated! Diamond sword deals 2x damage for 5 seconds!");
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 2.0f);

            new BukkitRunnable() {
                @Override
                public void run() {
                    activeEffects.remove(player.getUniqueId());
                    player.sendMessage("§3Gleaming Power effect ended.");
                }
            }.runTaskLater(plugin, 100); // 5 seconds
        } else {
            player.sendMessage("§cYou must be holding a diamond sword!");
        }
    }

    private void netheriteAbility(Player player) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType() == Material.AIR) {
            player.sendMessage("§cYou must hold an item to upgrade!");
            return;
        }

        Material upgraded = getNetheriteUpgrade(handItem.getType());
        if (upgraded == null) {
            player.sendMessage("§cThis item cannot be upgraded to netherite!");
            return;
        }

        ItemStack newItem = handItem.clone();
        newItem.setType(upgraded);
        player.getInventory().setItemInMainHand(newItem);
        player.sendMessage("§4Debris, Debris, Debris! Item upgraded to netherite!");
        player.playSound(player.getLocation(), Sound.UI_STONECUTTER_TAKE_RESULT, 1.0f, 0.5f);
    }

    public boolean hasActiveEffect(Player player) {
        return activeEffects.getOrDefault(player.getUniqueId(), false);
    }

    private Material getSmeltResult(Material input) {
        switch (input) {
            case IRON_ORE: case DEEPSLATE_IRON_ORE: return Material.IRON_INGOT;
            case GOLD_ORE: case DEEPSLATE_GOLD_ORE: return Material.GOLD_INGOT;
            case COPPER_ORE: case DEEPSLATE_COPPER_ORE: return Material.COPPER_INGOT;
            case COBBLESTONE: return Material.STONE;
            case SAND: return Material.GLASS;
            case RAW_IRON: return Material.IRON_INGOT;
            case RAW_GOLD: return Material.GOLD_INGOT;
            case RAW_COPPER: return Material.COPPER_INGOT;
            default: return null;
        }
    }

    private Material getNetheriteUpgrade(Material input) {
        switch (input) {
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
}