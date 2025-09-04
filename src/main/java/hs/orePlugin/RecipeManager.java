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
        // Register all ore recipes
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
        // Example: Coal recipe - replace with actual recipe from recipes.txt
        NamespacedKey key = new NamespacedKey(plugin, "coal_ore_recipe");
        ItemStack result = createOreItem(Material.COAL, "Coal Ore", "§8A dark, combustible ore");

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "BBB",
                "BBB",
                "BBB"
        );
        recipe.setIngredient('B', Material.BARRIER); // Placeholder - replace with actual ingredients

        Bukkit.addRecipe(recipe);
    }

    private void registerCopperRecipe() {
        // Example: Copper recipe - replace with actual recipe from recipes.txt
        NamespacedKey key = new NamespacedKey(plugin, "copper_ore_recipe");
        ItemStack result = createOreItem(Material.COPPER_INGOT, "Copper Ore", "§cA conductive orange ore");

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "BBB",
                "BBB",
                "BBB"
        );
        recipe.setIngredient('B', Material.BARRIER); // Placeholder - replace with actual ingredients

        Bukkit.addRecipe(recipe);
    }

    private void registerIronRecipe() {
        // Example: Iron recipe - replace with actual recipe from recipes.txt
        NamespacedKey key = new NamespacedKey(plugin, "iron_ore_recipe");
        ItemStack result = createOreItem(Material.IRON_INGOT, "Iron Ore", "§fA sturdy metallic ore");

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "BBB",
                "BBB",
                "BBB"
        );
        recipe.setIngredient('B', Material.BARRIER); // Placeholder - replace with actual ingredients

        Bukkit.addRecipe(recipe);
    }

    private void registerGoldRecipe() {
        // Example: Gold recipe - replace with actual recipe from recipes.txt
        NamespacedKey key = new NamespacedKey(plugin, "gold_ore_recipe");
        ItemStack result = createOreItem(Material.GOLD_INGOT, "Gold Ore", "§eA precious golden ore");

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "BBB",
                "BBB",
                "BBB"
        );
        recipe.setIngredient('B', Material.BARRIER); // Placeholder - replace with actual ingredients

        Bukkit.addRecipe(recipe);
    }

    private void registerRedstoneRecipe() {
        // Example: Redstone recipe - replace with actual recipe from recipes.txt
        NamespacedKey key = new NamespacedKey(plugin, "redstone_ore_recipe");
        ItemStack result = createOreItem(Material.REDSTONE, "Redstone Ore", "§4A powerful electrical ore");

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "BBB",
                "BBB",
                "BBB"
        );
        recipe.setIngredient('B', Material.BARRIER); // Placeholder - replace with actual ingredients

        Bukkit.addRecipe(recipe);
    }

    private void registerLapisRecipe() {
        // Example: Lapis recipe - replace with actual recipe from recipes.txt
        NamespacedKey key = new NamespacedKey(plugin, "lapis_ore_recipe");
        ItemStack result = createOreItem(Material.LAPIS_LAZULI, "Lapis Ore", "§9A mystical blue ore");

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "BBB",
                "BBB",
                "BBB"
        );
        recipe.setIngredient('B', Material.BARRIER); // Placeholder - replace with actual ingredients

        Bukkit.addRecipe(recipe);
    }

    private void registerEmeraldRecipe() {
        // Example: Emerald recipe - replace with actual recipe from recipes.txt
        NamespacedKey key = new NamespacedKey(plugin, "emerald_ore_recipe");
        ItemStack result = createOreItem(Material.EMERALD, "Emerald Ore", "§aA rare green ore");

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "BBB",
                "BBB",
                "BBB"
        );
        recipe.setIngredient('B', Material.BARRIER); // Placeholder - replace with actual ingredients

        Bukkit.addRecipe(recipe);
    }

    private void registerAmethystRecipe() {
        // Example: Amethyst recipe - replace with actual recipe from recipes.txt
        NamespacedKey key = new NamespacedKey(plugin, "amethyst_ore_recipe");
        ItemStack result = createOreItem(Material.AMETHYST_SHARD, "Amethyst Ore", "§dA crystalline purple ore");

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "BBB",
                "BBB",
                "BBB"
        );
        recipe.setIngredient('B', Material.BARRIER); // Placeholder - replace with actual ingredients

        Bukkit.addRecipe(recipe);
    }

    private void registerDiamondRecipe() {
        // Example: Diamond recipe - replace with actual recipe from recipes.txt
        NamespacedKey key = new NamespacedKey(plugin, "diamond_ore_recipe");
        ItemStack result = createOreItem(Material.DIAMOND, "Diamond Ore", "§bA brilliant crystal ore");

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "BBB",
                "BBB",
                "BBB"
        );
        recipe.setIngredient('B', Material.BARRIER); // Placeholder - replace with actual ingredients

        Bukkit.addRecipe(recipe);
    }

    private void registerNetheriteRecipe() {
        // Example: Netherite recipe - replace with actual recipe from recipes.txt
        NamespacedKey key = new NamespacedKey(plugin, "netherite_ore_recipe");
        ItemStack result = createOreItem(Material.NETHERITE_INGOT, "Netherite Ore", "§8The ultimate ore");

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(
                "BBB",
                "BBB",
                "BBB"
        );
        recipe.setIngredient('B', Material.BARRIER); // Placeholder - replace with actual ingredients

        Bukkit.addRecipe(recipe);
    }

    private ItemStack createOreItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r" + name);
            meta.setLore(java.util.Arrays.asList(lore, "§7Right-click to unlock this ore type!"));
            item.setItemMeta(meta);
        }
        return item;
    }

    // Helper method to create shapeless recipes
    private ShapelessRecipe createShapelessRecipe(NamespacedKey key, ItemStack result, Material... ingredients) {
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        for (Material ingredient : ingredients) {
            recipe.addIngredient(ingredient);
        }
        return recipe;
    }

    // Helper method to create shaped recipes with a pattern
    private ShapedRecipe createShapedRecipe(NamespacedKey key, ItemStack result, String[] pattern, char ingredient, Material material) {
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(pattern);
        recipe.setIngredient(ingredient, material);
        return recipe;
    }

    // Method to remove all recipes (useful for reloading)
    public void removeAllRecipes() {
        // Remove all ore recipes
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