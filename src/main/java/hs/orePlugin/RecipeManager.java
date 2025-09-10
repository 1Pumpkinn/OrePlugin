package hs.orePlugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.CraftingInventory;

import java.util.*;

public class RecipeManager implements Listener {

    private final OreAbilitiesPlugin plugin;
    private final Random random = new Random();
    private final List<NamespacedKey> registeredRecipes = new ArrayList<>();

    // Centralized recipe definitions
    private final Map<OreType, RecipeDefinition> recipeDefinitions = new HashMap<>();

    public RecipeManager(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadRecipeDefinitions();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        ItemStack result = event.getRecipe().getResult();
        if (!isDirectOreItem(result)) return;

        Player player = (Player) event.getWhoClicked();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType craftedOreType = getOreTypeFromDirectItem(result);

        if (craftedOreType == null) {
            event.setCancelled(true);
            player.sendMessage("§cError: Invalid ore type!");
            return;
        }

        // Always cancel the event to handle item consumption manually
        event.setCancelled(true);

        OreConfigs configs = plugin.getOreConfigs();
        double shatterChance = configs != null ? configs.getShatterChance() : 0.25;

        // CRITICAL: Check if ore shatters BEFORE consuming materials or doing anything else
        boolean shattered = random.nextDouble() < shatterChance;

        // Consume the crafting materials regardless of success/failure
        consumeCraftingMaterials(event.getInventory());

        // If ore shattered, exit immediately - do NOT apply any effects
        if (shattered) {
            String oreColor = getOreColor(craftedOreType);
            player.sendMessage("§c💥 The " + oreColor + craftedOreType.getDisplayName() + " §ore shattered during crafting!");
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

            // IMPORTANT: Add additional safeguards to ensure no ore effects are applied
            plugin.getLogger().info("Ore ore shattered for " + player.getName() + " - no effects applied");
            return; // EXIT IMMEDIATELY - no ore effects should be applied
        }

        // Only reach this point if crafting was successful (no shatter)
        OreType currentOreType = dataManager.getPlayerOre(player);

        // Success: Remove old ore effects and apply new ones
        if (currentOreType != null) {
            if (currentOreType == craftedOreType) {
                player.sendMessage("§e⚡ Resetting your " + craftedOreType.getDisplayName() + " ore!");
            } else {
                player.sendMessage("§e⚠ Replacing your " + currentOreType.getDisplayName() + " ore ability with " + craftedOreType.getDisplayName() + "!");
            }
            removeOreTypeEffects(player, currentOreType);
        }

        // Give the new ore mastery
        dataManager.setPlayerOre(player, craftedOreType);
        applyOreTypeEffects(player, craftedOreType);

        // Update UI
        plugin.getActionBarManager().stopActionBar(player);
        plugin.getActionBarManager().startActionBar(player);
        plugin.getAbilityManager().restartPlayerTimers(player);

        // Success messages
        String oreColor = getOreColor(craftedOreType);
        if (currentOreType == craftedOreType) {
            player.sendMessage("§a✨ Successfully refreshed the " + oreColor + craftedOreType.getDisplayName() + " §aOre!");
            player.sendMessage("§7All effects have been reset and reapplied!");
        } else {
            player.sendMessage("§a✨ Successfully obtained the " + oreColor + craftedOreType.getDisplayName() + " §aOre!");
            player.sendMessage("§7Your new ability: §6" + getAbilityName(craftedOreType));
        }

        int cooldown = configs != null ? configs.getCooldown(craftedOreType) : craftedOreType.getCooldown();
        player.sendMessage("§7Cooldown: §b" + cooldown + " seconds");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        // Log successful crafting for debugging
        plugin.getLogger().info("Successfully applied " + craftedOreType.name() + " ore to " + player.getName());
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        // REMOVED: No longer prevent crafting if player already has the ore
        // This allows players to craft the same ore multiple times

        // Keep the event handler but don't do any prevention logic
        if (event.getRecipe() == null) return;
        ItemStack result = event.getRecipe().getResult();
        if (!isDirectOreItem(result)) return;

        // All ore masteries can now always be crafted
    }

    /**
     * Consumes all crafting materials from the crafting grid
     */
    private void consumeCraftingMaterials(CraftingInventory inventory) {
        // Get the crafting matrix (excluding result slot)
        ItemStack[] matrix = inventory.getMatrix();

        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];
            if (item != null && !item.getType().isAir()) {
                // Reduce stack by 1 or set to null if only 1 item
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    matrix[i] = null;
                }
            }
        }

        // Update the crafting matrix
        inventory.setMatrix(matrix);
    }

    /**
     * Remove old ore type effects when switching
     */
    private void removeOreTypeEffects(Player player, OreType oreType) {
        // Use AbilityListener's method for comprehensive cleanup
        plugin.getAbilityListener().removeAllOreTypeEffectsFixed(player, oreType);

        // Also cleanup PlayerListener tracking
        PlayerListener playerListener = plugin.getPlayerListener();
        if (playerListener != null) {
            playerListener.cleanupPlayerEffects(player, oreType);
        }
    }

    /**
     * Apply new ore type effects when switching
     */
    private void applyOreTypeEffects(Player player, OreType oreType) {
        // Use AbilityListener's method for comprehensive application
        plugin.getAbilityListener().applyAllOreTypeEffectsFixed(player, oreType);
    }

    // Recipe System
    private void loadRecipeDefinitions() {
        recipeDefinitions.put(OreType.COAL, new RecipeDefinition(
                new String[]{"BFB", "OCD", "BFB"},
                Map.of(
                        'B', Material.BLAST_FURNACE,
                        'F', Material.FIRE_CHARGE,
                        'O', Material.COAL_ORE,
                        'C', Material.COAL,
                        'D', Material.DEEPSLATE_COAL_ORE)

        ));
        recipeDefinitions.put(OreType.COPPER, new RecipeDefinition(
                new String[]{"BLO", "RCR", "OLB"},
                Map.of(
                        'B', Material.COPPER_BLOCK,
                        'L', Material.LIGHTNING_ROD,
                        'O', Material.OXIDIZED_COPPER,
                        'R', Material.RAW_COPPER_BLOCK,
                        'C', Material.COPPER_INGOT)
        ));
        recipeDefinitions.put(OreType.IRON, new RecipeDefinition(
                new String[]{"HNC", "OID", "LRB"},
                Map.of(
                        'H', Material.IRON_HELMET,
                        'N', Material.IRON_BLOCK,
                        'C', Material.IRON_CHESTPLATE,
                        'O', Material.IRON_ORE,
                        'I', Material.IRON_INGOT,
                        'D', Material.DEEPSLATE_IRON_ORE,
                        'L', Material.IRON_LEGGINGS,
                        'R', Material.RAW_IRON_BLOCK,
                        'B', Material.IRON_BOOTS)
        ));
        recipeDefinitions.put(OreType.GOLD, new RecipeDefinition(
                new String[]{"NEO", "SGP", "DAR"},
                Map.of('N', Material.NETHER_GOLD_ORE,
                        'E', Material.ENCHANTED_GOLDEN_APPLE,
                        'O', Material.GOLD_ORE,
                        'S', Material.GOLDEN_SWORD,
                        'G', Material.GOLD_INGOT,
                        'P', Material.GOLDEN_PICKAXE,
                        'D', Material.DEEPSLATE_GOLD_ORE,
                        'A', Material.GOLDEN_APPLE,
                        'R', Material.RAW_GOLD_BLOCK)
        ));
        recipeDefinitions.put(OreType.REDSTONE, new RecipeDefinition(
                new String[]{"LPL", "ORD", "LPL"},
                Map.of('L', Material.REDSTONE_LAMP,
                        'P', Material.PISTON,
                        'O', Material.REDSTONE_ORE,
                        'R', Material.REDSTONE,
                        'D', Material.DEEPSLATE_REDSTONE_ORE)
        ));
        recipeDefinitions.put(OreType.LAPIS, new RecipeDefinition(
                new String[]{"XEX", "BLB", "XEX"},
                Map.of('X', Material.EXPERIENCE_BOTTLE,
                        'E', Material.ENCHANTING_TABLE,
                        'B', Material.BOOK,
                        'L', Material.LAPIS_LAZULI
                )
        ));
        recipeDefinitions.put(OreType.EMERALD, new RecipeDefinition(
                new String[]{"BLB", "SEG", "BFB"},
                Map.of('B', Material.EMERALD_BLOCK,
                        'L', Material.LECTERN,
                        'S', Material.STONECUTTER,
                        'G', Material.GRINDSTONE,
                        'F', Material.FLETCHING_TABLE,
                        'E', Material.EMERALD)
        ));
        recipeDefinitions.put(OreType.AMETHYST, new RecipeDefinition(
                new String[]{"BSB", "CAM", "BLB"},
                Map.of('B', Material.AMETHYST_BLOCK,
                        'S', Material.SMALL_AMETHYST_BUD,
                        'M', Material.MEDIUM_AMETHYST_BUD,
                        'L', Material.LARGE_AMETHYST_BUD,
                        'C', Material.AMETHYST_CLUSTER,
                        'A', Material.AMETHYST_SHARD)
        ));
        recipeDefinitions.put(OreType.DIAMOND, new RecipeDefinition(
                new String[]{"BHB", "OAD", "BSB"},
                Map.of('B', Material.DIAMOND_BLOCK,
                        'H', Material.DIAMOND_HORSE_ARMOR,
                        'O', Material.DIAMOND_ORE,
                        'A', Material.DIAMOND,
                        'D', Material.DEEPSLATE_DIAMOND_ORE,
                        'S', Material.DIAMOND_SWORD)
        ));
        recipeDefinitions.put(OreType.NETHERITE, new RecipeDefinition(
                new String[]{"USU", "ANH", "UPU"},
                Map.of('U', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                        'S', Material.NETHERITE_SWORD,
                        'A', Material.NETHERITE_AXE,
                        'N', Material.NETHERITE_INGOT,
                        'H', Material.NETHERITE_SHOVEL,
                        'P', Material.NETHERITE_PICKAXE)
        ));
    }

    public void registerAllRecipes() {
        plugin.getLogger().info("Registering ore recipes...");

        for (OreType oreType : recipeDefinitions.keySet()) {
            registerRecipe(oreType, recipeDefinitions.get(oreType));
        }

        plugin.getLogger().info("Registered " + registeredRecipes.size() + " ore recipes!");
    }

    private void registerRecipe(OreType oreType, RecipeDefinition def) {
        NamespacedKey key = new NamespacedKey(plugin, oreType.name().toLowerCase() + "_ore_mastery");
        ItemStack result = createDirectOreItem(oreType);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(def.shape);

        def.ingredients.forEach(recipe::setIngredient);

        if (Bukkit.addRecipe(recipe)) {
            registeredRecipes.add(key);
            plugin.getLogger().info("Registered " + oreType.getDisplayName() + " recipe");
        } else {
            plugin.getLogger().warning("Failed to register " + oreType.getDisplayName() + " recipe");
        }
    }

    // Inner helper class
    private static class RecipeDefinition {
        final String[] shape;
        final Map<Character, Material> ingredients;

        RecipeDefinition(String[] shape, Map<Character, Material> ingredients) {
            this.shape = shape;
            this.ingredients = ingredients;
        }
    }

    // Item Creation
    private ItemStack createDirectOreItem(OreType oreType) {
        Material material = getBaseMaterial(oreType);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String oreColor = getOreColor(oreType);
            String abilityName = getAbilityName(oreType);

            meta.setDisplayName(oreColor + oreType.getDisplayName() + " Ore Mastery");

            OreConfigs configs = plugin.getOreConfigs();
            int shatterPercent = configs != null ? (int)(configs.getShatterChance() * 100) : 25;

            meta.setLore(List.of(
                    "§7Grants " + oreColor + oreType.getDisplayName() + " §7ore ability!",
                    "§7Ability: §6" + abilityName,
                    "§7Cooldown: §b" + oreType.getCooldown() + "s",
                    "",
                    "§c⚠ " + shatterPercent + "% chance to shatter during crafting!",
                    "§7Materials will be consumed even if shattered",
                    "§a✓ Can be crafted multiple times to refresh effects",
                    "§8Ore Abilities Plugin"
            ));
            meta.setCustomModelData(1000 + oreType.ordinal());
            item.setItemMeta(meta);
        }

        return item;
    }

    // Helpers
    private Material getBaseMaterial(OreType oreType) {
        switch (oreType) {
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

    private String getOreColor(OreType oreType) {
        switch (oreType) {
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

    public static boolean isDirectOreItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        return item.getItemMeta().getDisplayName().contains("Ore Mastery");
    }

    public static OreType getOreTypeFromDirectItem(ItemStack item) {
        if (!isDirectOreItem(item)) return null;
        String name = item.getItemMeta().getDisplayName();
        for (OreType type : OreType.values()) {
            if (name.contains(type.getDisplayName())) return type;
        }
        return null;
    }

    public int getRecipeCount() {
        return registeredRecipes.size();
    }

    public ItemStack createOreItem(OreType oreType) {
        return createDirectOreItem(oreType);
    }
}