package hs.orePlugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

public class RecipeManager {

    private final OreAbilitiesPlugin plugin;

    public RecipeManager(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAllRecipes() {
        // Register all ore recipes with actual crafting patterns
        registerCoalRecipe();
        registerCopperRecipe();
        registerIronRecipe();
        registerGoldRecipe();
        registerRedstoneRecipe();
        registerLapisRecipe();
        registerEmeraldRecipe();
        registerAmethystRecipe();
        registerDiamondRecipe();
        registerNetheriteRecipe();
    }

    private void registerCoalRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "coal_ore_recipe");
        ItemStack result = createDirectOreItem(OreType.COAL);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "CCC",
                "CDC",
                "CCC"
        );
        recipe.setIngredient('C', Material.COAL);
        recipe.setIngredient('D', Material.DIRT);

        Bukkit.addRecipe(recipe);
    }

    private void registerCopperRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "copper_ore_recipe");
        ItemStack result = createDirectOreItem(OreType.COPPER);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "III",
                "ICI",
                "III"
        );
        recipe.setIngredient('I', Material.COPPER_INGOT);
        recipe.setIngredient('C', Material.COAL);

        Bukkit.addRecipe(recipe);
    }

    private void registerIronRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "iron_ore_recipe");
        ItemStack result = createDirectOreItem(OreType.IRON);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "III",
                "ICI",
                "III"
        );
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('C', Material.COPPER_INGOT);

        Bukkit.addRecipe(recipe);
    }

    private void registerGoldRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "gold_ore_recipe");
        ItemStack result = createDirectOreItem(OreType.GOLD);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "GGG",
                "GIG",
                "GGG"
        );
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('I', Material.IRON_INGOT);

        Bukkit.addRecipe(recipe);
    }

    private void registerRedstoneRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "redstone_ore_recipe");
        ItemStack result = createDirectOreItem(OreType.REDSTONE);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "RRR",
                "RGR",
                "RRR"
        );
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('G', Material.GOLD_INGOT);

        Bukkit.addRecipe(recipe);
    }

    private void registerLapisRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "lapis_ore_recipe");
        ItemStack result = createDirectOreItem(OreType.LAPIS);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "LLL",
                "LRL",
                "LLL"
        );
        recipe.setIngredient('L', Material.LAPIS_LAZULI);
        recipe.setIngredient('R', Material.REDSTONE);

        Bukkit.addRecipe(recipe);
    }

    private void registerEmeraldRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "emerald_ore_recipe");
        ItemStack result = createDirectOreItem(OreType.EMERALD);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "EEE",
                "ELE",
                "EEE"
        );
        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('L', Material.LAPIS_LAZULI);

        Bukkit.addRecipe(recipe);
    }

    private void registerAmethystRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "amethyst_ore_recipe");
        ItemStack result = createDirectOreItem(OreType.AMETHYST);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "AAA",
                "AEA",
                "AAA"
        );
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('E', Material.EMERALD);

        Bukkit.addRecipe(recipe);
    }

    private void registerDiamondRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "diamond_ore_recipe");
        ItemStack result = createDirectOreItem(OreType.DIAMOND);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "DDD",
                "DAD",
                "DDD"
        );
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('A', Material.AMETHYST_SHARD);

        Bukkit.addRecipe(recipe);
    }

    private void registerNetheriteRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "netherite_ore_recipe");
        ItemStack result = createDirectOreItem(OreType.NETHERITE);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "NNN",
                "NDN",
                "NNN"
        );
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('D', Material.DIAMOND);

        Bukkit.addRecipe(recipe);
    }

    private ItemStack createDirectOreItem(OreType oreType) {
        Material material = getBaseMaterial(oreType);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String oreColor = getOreColor(oreType);
            String abilityName = getAbilityName(oreType);

            meta.setDisplayName(oreColor + oreType.getDisplayName() + " Ore Mastery");
            meta.setLore(java.util.Arrays.asList(
                    "§7Grants you the " + oreColor + oreType.getDisplayName() + " §7ore ability!",
                    "§7Ability: §6" + abilityName,
                    "§7Cooldown: §b" + oreType.getCooldown() + " seconds",
                    "",
                    "§c⚠ 25% chance to shatter during crafting!",
                    "§8Ore Abilities Plugin"
            ));
            item.setItemMeta(meta);
        }

        return item;
    }

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

    // Method to check if an item is a direct ore mastery item
    public static boolean isDirectOreItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        String displayName = item.getItemMeta().getDisplayName();
        return displayName.contains("Ore Mastery");
    }

    // Method to get ore type from direct ore item
    public static OreType getOreTypeFromDirectItem(ItemStack item) {
        if (!isDirectOreItem(item)) {
            return null;
        }

        String displayName = item.getItemMeta().getDisplayName();

        if (displayName.contains("Coal")) return OreType.COAL;
        if (displayName.contains("Copper")) return OreType.COPPER;
        if (displayName.contains("Iron")) return OreType.IRON;
        if (displayName.contains("Gold")) return OreType.GOLD;
        if (displayName.contains("Redstone")) return OreType.REDSTONE;
        if (displayName.contains("Lapis")) return OreType.LAPIS;
        if (displayName.contains("Emerald")) return OreType.EMERALD;
        if (displayName.contains("Amethyst")) return OreType.AMETHYST;
        if (displayName.contains("Diamond")) return OreType.DIAMOND;
        if (displayName.contains("Netherite")) return OreType.NETHERITE;

        return null;
    }

    // Method to remove all recipes (useful for reloading)
    public void removeAllRecipes() {
        String[] recipeNames = {
                "coal_ore_recipe", "copper_ore_recipe", "iron_ore_recipe", "gold_ore_recipe",
                "redstone_ore_recipe", "lapis_ore_recipe", "emerald_ore_recipe", "amethyst_ore_recipe",
                "diamond_ore_recipe", "netherite_ore_recipe"
        };

        for (String recipeName : recipeNames) {
            NamespacedKey key = new NamespacedKey(plugin, recipeName);
            Bukkit.removeRecipe(key);
        }
    }
}