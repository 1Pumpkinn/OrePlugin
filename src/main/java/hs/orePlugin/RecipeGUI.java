package hs.orePlugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeGUI implements Listener {

    private final OreAbilitiesPlugin plugin;
    private final Map<Player, Inventory> openGUIs = new HashMap<>();

    // Recipe definitions matching your RecipeManager
    private final Map<OreType, RecipeData> recipes = new HashMap<>();

    public RecipeGUI(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
        initializeRecipes();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void initializeRecipes() {
        // Coal recipe
        recipes.put(OreType.COAL, new RecipeData(
                new Material[][]{
                        {Material.BLAST_FURNACE, Material.FIRE_CHARGE, Material.BLAST_FURNACE},
                        {Material.COAL_ORE, Material.COAL, Material.DEEPSLATE_COAL_ORE},
                        {Material.BLAST_FURNACE, Material.FIRE_CHARGE, Material.BLAST_FURNACE}
                },
                "§8Coal §7Ore Recipe",
                Arrays.asList("§7Craft this to unlock §8Coal §7abilities!", "§6Ability: §eSizzle", "§7Smelt items in your hand")
        ));

        // Copper recipe
        recipes.put(OreType.COPPER, new RecipeData(
                new Material[][]{
                        {Material.COPPER_BLOCK, Material.LIGHTNING_ROD, Material.OXIDIZED_COPPER},
                        {Material.RAW_COPPER_BLOCK, Material.COPPER_INGOT, Material.RAW_COPPER_BLOCK},
                        {Material.OXIDIZED_COPPER, Material.LIGHTNING_ROD, Material.COPPER_BLOCK}
                },
                "§cCopper §7Ore Recipe",
                Arrays.asList("§7Craft this to unlock §cCopper §7abilities!", "§6Ability: §eChannel The Clouds", "§7Lightning strikes on hit for 10s")
        ));

        // Iron recipe
        recipes.put(OreType.IRON, new RecipeData(
                new Material[][]{
                        {Material.IRON_HELMET, Material.IRON_BLOCK, Material.IRON_CHESTPLATE},
                        {Material.IRON_ORE, Material.IRON_INGOT, Material.DEEPSLATE_IRON_ORE},
                        {Material.IRON_LEGGINGS, Material.RAW_IRON_BLOCK, Material.IRON_BOOTS}
                },
                "§fIron §7Ore Recipe",
                Arrays.asList("§7Craft this to unlock §fIron §7abilities!", "§6Ability: §eBucket Roulette", "§7Get random bucket + permanent +2 armor")
        ));

        // Gold recipe
        recipes.put(OreType.GOLD, new RecipeData(
                new Material[][]{
                        {Material.NETHER_GOLD_ORE, Material.ENCHANTED_GOLDEN_APPLE, Material.GOLD_ORE},
                        {Material.GOLDEN_SWORD, Material.GOLD_INGOT, Material.GOLDEN_PICKAXE},
                        {Material.DEEPSLATE_GOLD_ORE, Material.GOLDEN_APPLE, Material.RAW_GOLD_BLOCK}
                },
                "§eGold §7Ore Recipe",
                Arrays.asList("§7Craft this to unlock §eGold §7abilities!", "§6Ability: §eGoldrush", "§7Haste 5 + Speed 3 for 10s")
        ));

        // Redstone recipe
        recipes.put(OreType.REDSTONE, new RecipeData(
                new Material[][]{
                        {Material.REDSTONE_LAMP, Material.PISTON, Material.REDSTONE_LAMP},
                        {Material.REDSTONE_ORE, Material.REDSTONE, Material.DEEPSLATE_REDSTONE_ORE},
                        {Material.REDSTONE_LAMP, Material.PISTON, Material.REDSTONE_LAMP}
                },
                "§4Redstone §7Ore Recipe",
                Arrays.asList("§7Craft this to unlock §4Redstone §7abilities!", "§6Ability: §eSticky Slime", "§7Next hit prevents jumping for 10s")
        ));

        // Lapis recipe
        recipes.put(OreType.LAPIS, new RecipeData(
                new Material[][]{
                        {Material.EXPERIENCE_BOTTLE, Material.ENCHANTING_TABLE, Material.EXPERIENCE_BOTTLE},
                        {Material.BOOK, Material.LAPIS_LAZULI, Material.BOOK},
                        {Material.EXPERIENCE_BOTTLE, Material.ENCHANTING_TABLE, Material.EXPERIENCE_BOTTLE}
                },
                "§9Lapis §7Ore Recipe",
                Arrays.asList("§7Craft this to unlock §9Lapis §7abilities!", "§6Ability: §eLevel Replenish", "§7EXP gives regen + free enchanting")
        ));

        // Emerald recipe
        recipes.put(OreType.EMERALD, new RecipeData(
                new Material[][]{
                        {Material.EMERALD_BLOCK, Material.LECTERN, Material.EMERALD_BLOCK},
                        {Material.STONECUTTER, Material.EMERALD, Material.GRINDSTONE},
                        {Material.EMERALD_BLOCK, Material.FLETCHING_TABLE, Material.EMERALD_BLOCK}
                },
                "§aEmerald §7Ore Recipe",
                Arrays.asList("§7Craft this to unlock §aEmerald §7abilities!", "§6Ability: §eBring Home The Effects", "§7All beacon effects for 20s")
        ));

        // Amethyst recipe
        recipes.put(OreType.AMETHYST, new RecipeData(
                new Material[][]{
                        {Material.AMETHYST_BLOCK, Material.SMALL_AMETHYST_BUD, Material.AMETHYST_BLOCK},
                        {Material.AMETHYST_CLUSTER, Material.AMETHYST_SHARD, Material.MEDIUM_AMETHYST_BUD},
                        {Material.AMETHYST_BLOCK, Material.LARGE_AMETHYST_BUD, Material.AMETHYST_BLOCK}
                },
                "§dAmethyst §7Ore Recipe",
                Arrays.asList("§7Craft this to unlock §dAmethyst §7abilities!", "§6Ability: §eCrystal Cluster", "§7No knockback + no damage for 10s")
        ));

        // Diamond recipe
        recipes.put(OreType.DIAMOND, new RecipeData(
                new Material[][]{
                        {Material.DIAMOND_BLOCK, Material.DIAMOND_HORSE_ARMOR, Material.DIAMOND_BLOCK},
                        {Material.DIAMOND_ORE, Material.DIAMOND, Material.DEEPSLATE_DIAMOND_ORE},
                        {Material.DIAMOND_BLOCK, Material.DIAMOND_SWORD, Material.DIAMOND_BLOCK}
                },
                "§bDiamond §7Ore Recipe",
                Arrays.asList("§7Craft this to unlock §bDiamond §7abilities!", "§6Ability: §eGleaming Power", "§7Diamond sword 1.4x damage for 5s")
        ));

        // Netherite recipe
        recipes.put(OreType.NETHERITE, new RecipeData(
                new Material[][]{
                        {Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Material.NETHERITE_SWORD, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE},
                        {Material.NETHERITE_AXE, Material.NETHERITE_INGOT, Material.NETHERITE_SHOVEL},
                        {Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Material.NETHERITE_PICKAXE, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE}
                },
                "§8Netherite §7Ore Recipe",
                Arrays.asList("§7Craft this to unlock §8Netherite §7abilities!", "§6Ability: §eDebris, Debris, Debris", "§7Convert Ancient Debris to Netherite")
        ));
    }

    public void openRecipeGUI(Player player, OreType oreType) {
        RecipeData recipeData = recipes.get(oreType);
        if (recipeData == null) {
            player.sendMessage("§cNo recipe found for " + oreType.getDisplayName() + " ore!");
            return;
        }

        // Create 27-slot inventory (3 rows x 9 columns) - exact same as image
        Inventory gui = Bukkit.createInventory(null, 27, recipeData.title);

        // Fill entire GUI with gray glass panes first
        ItemStack grayGlass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, grayGlass);
        }

        // Recipe slots in center 3x3 grid (slots 10, 11, 12, 19, 20, 21, 28, 29, 30)
        // But since we only have 27 slots (0-26), the bottom row is 18, 19, 20
        int[] recipeSlots = {10, 11, 12, 19, 20, 21}; // Only 2 rows visible in 27-slot inventory

        // Actually, let me map this correctly for a 27-slot inventory to match the image exactly:
        // Top row: slots 3, 4, 5
        // Middle row: slots 12, 13, 14
        // Bottom row: slots 21, 22, 23
        int[] correctRecipeSlots = {3, 4, 5, 12, 13, 14, 21, 22, 23};

        // Place recipe items in the 3x3 grid
        int recipeIndex = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (recipeIndex < correctRecipeSlots.length) {
                    Material material = recipeData.recipe[row][col];
                    if (material != null && material != Material.AIR) {
                        ItemStack item = new ItemStack(material);
                        gui.setItem(correctRecipeSlots[recipeIndex], item);
                    }
                    recipeIndex++;
                }
            }
        }

        // Result item (slot 7 - top right area)
        ItemStack result = plugin.getRecipeManager().createOreItem(oreType);
        gui.setItem(16, result);

        // Arrow pointing right (slot 6 - next to result)
        ItemStack arrow = createItem(Material.RED_STAINED_GLASS_PANE, "§c→", Arrays.asList("§7Crafting Result"));
        gui.setItem(15, arrow);

        // Info panel (slot 25 - bottom right)
        gui.setItem(25, createItem(Material.BOOK, "§6Recipe Information", recipeData.description));

        openGUIs.put(player, gui);
        player.openInventory(gui);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    public void openAllRecipesGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6§lAll Ore Recipes");

        // Fill with light gray glass panes
        ItemStack lightGrayGlass = createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, lightGrayGlass);
        }

        // Add ore items for selection in a compact grid layout
        int[] slots = {1, 2, 3, 5, 6, 7, 10, 11, 12, 14, 15, 16, 19, 20, 21, 23, 24, 25};
        int index = 0;

        for (OreType oreType : OreType.values()) {
            if (!oreType.isStarter() && index < slots.length) {
                ItemStack oreItem = plugin.getRecipeManager().createOreItem(oreType);
                ItemMeta meta = oreItem.getItemMeta();
                if (meta != null) {
                    meta.setLore(Arrays.asList("§7Click to view the recipe for", "§6" + oreType.getDisplayName() + " §7ore!", "", "§eClick to open recipe!"));
                    oreItem.setItemMeta(meta);
                }
                gui.setItem(slots[index], oreItem);
                index++;
            }
        }

        // Close button (center bottom)
        gui.setItem(22, createItem(Material.BARRIER, "§cClose", Arrays.asList("§7Click to close this menu")));

        // Info about starter ores (bottom left corner)
        gui.setItem(18, createItem(Material.GRASS_BLOCK, "§aStarter Ores",
                Arrays.asList("§7Dirt, Wood, and Stone ores", "§7are given when you first join!", "§7No recipe needed for these.")));

        openGUIs.put(player, gui);
        player.openInventory(gui);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!openGUIs.containsKey(player)) return;

        event.setCancelled(true); // Prevent taking items

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Handle close button
        if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // Handle ore selection in "all recipes" GUI
        if (event.getView().getTitle().equals("§6§lAll Ore Recipes")) {
            for (OreType oreType : OreType.values()) {
                ItemStack oreItem = plugin.getRecipeManager().createOreItem(oreType);
                if (clickedItem.isSimilar(oreItem) ||
                        (clickedItem.hasItemMeta() && oreItem.hasItemMeta() &&
                                clickedItem.getItemMeta().getDisplayName().equals(oreItem.getItemMeta().getDisplayName()))) {

                    player.closeInventory();
                    // Small delay to prevent inventory conflicts
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        openRecipeGUI(player, oreType);
                    }, 2L);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            openGUIs.remove(player);
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static class RecipeData {
        final Material[][] recipe;
        final String title;
        final List<String> description;

        RecipeData(Material[][] recipe, String title, List<String> description) {
            this.recipe = recipe;
            this.title = title;
            this.description = description;
        }
    }

    public void cleanup() {
        openGUIs.clear();
    }
}