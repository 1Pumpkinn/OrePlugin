package hs.orePlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OreAbilitiesCommand implements CommandExecutor {

    private final OreAbilitiesPlugin plugin;

    public OreAbilitiesCommand(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        PlayerDataManager dataManager = plugin.getPlayerDataManager();

        // Handle different command labels for easier usage
        if (label.equalsIgnoreCase("ore")) {
            if (args.length == 0) {
                showPlayerInfo(player, dataManager);
                return true;
            }
        }

        if (label.equalsIgnoreCase("ability")) {
            handleAbilityCommand(player);
            return true;
        }

        if (label.equalsIgnoreCase("bedrock")) {
            handleBedrockCommand(player);
            return true;
        }

        // FIXED: Handle orecd command
        if (label.equalsIgnoreCase("orecd")) {
            handleOreCooldownCommand(player, args);
            return true;
        }

        // FIXED: Handle recipes command - this is the main fix!
        if (label.equalsIgnoreCase("recipes") || label.equalsIgnoreCase("recipe") || label.equalsIgnoreCase("orerecipes")) {
            handleRecipeCommand(player, args);
            return true;
        }

        // Main command handling
        if (args.length == 0) {
            showPlayerInfo(player, dataManager);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info":
            case "me":
            case "stats":
                showPlayerInfo(player, dataManager);
                break;

            case "list":
            case "all":
            case "ores":
                showAllOres(player);
                break;

            case "help":
            case "?":
                showHelp(player);
                break;

            case "change":
            case "set":
                handleSetOreCommand(player, dataManager, args);
                break;

            case "bedrock":
            case "be":
                handleBedrockCommand(player);
                break;

            case "ability":
            case "use":
                handleAbilityCommand(player);
                break;

            case "recipes":
            case "recipe":
                handleRecipeCommand(player, args);
                break;

            case "reload":
                if (player.hasPermission("oreabilities.admin")) {
                    plugin.reloadConfig();
                    dataManager.loadPlayerData();
                    player.sendMessage("§aOre Abilities plugin reloaded!");
                } else {
                    player.sendMessage("§cYou don't have permission to reload the plugin!");
                }
                break;

            case "reset":
                handleResetCommand(player, args);
                break;

            default:
                // Try to interpret as ore info request
                handleOreInfoCommand(player, new String[]{"oreinfo", subCommand});
                break;
        }

        return true;
    }

    // FIXED: Handle recipe command with proper argument handling
    private void handleRecipeCommand(Player player, String[] args) {
        // Check if this was called directly as /recipes command
        if (args.length == 0) {
            // Show all recipes GUI
            plugin.getRecipeGUI().openAllRecipesGUI(player);
            return;
        }

        // If called with arguments (either /recipes <ore> or /ore recipes <ore>)
        String oreName;
        if (args.length == 1) {
            // Direct call like /recipes coal
            oreName = args[0].toUpperCase();
        } else {
            // Subcommand call like /ore recipes coal
            oreName = args[1].toUpperCase();
        }

        OreType oreType;

        try {
            oreType = OreType.valueOf(oreName);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cUnknown ore type: §e" + oreName);
            player.sendMessage("§7Use §e/recipes §7to see all available recipes");
            return;
        }

        if (oreType.isStarter()) {
            player.sendMessage("§c" + oreType.getDisplayName() + " §cis a starter ore - no recipe needed!");
            player.sendMessage("§7Starter ores are assigned when you first join");
            return;
        }

        // Open specific recipe GUI
        plugin.getRecipeGUI().openRecipeGUI(player, oreType);
    }

    // Handle ore cooldown command
    private void handleOreCooldownCommand(Player player, String[] args) {
        if (!player.hasPermission("oreabilities.admin")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("reset")) {
            player.sendMessage("§cUsage: §e/orecd reset [player]");
            player.sendMessage("§7Resets the ore ability cooldown for a player");
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("§cPlayer '" + args[1] + "' not found or not online!");
                return;
            }
        } else {
            target = player;
        }

        boolean hadCooldown = plugin.getAbilityManager().resetCooldown(target);

        if (hadCooldown) {
            if (target.equals(player)) {
                player.sendMessage("§aYour ore ability cooldown has been reset!");
            } else {
                player.sendMessage("§aReset ore ability cooldown for " + target.getName() + "!");
                target.sendMessage("§eYour ore ability cooldown has been reset by an admin!");
            }
        } else {
            if (target.equals(player)) {
                player.sendMessage("§cYou don't have an active cooldown!");
            } else {
                player.sendMessage("§c" + target.getName() + " doesn't have an active cooldown!");
            }
        }
    }

    private void showPlayerInfo(Player player, PlayerDataManager dataManager) {
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == null) {
            player.sendMessage("§cYou don't have an ore type assigned!");
            return;
        }

        player.sendMessage("§6=== Your Ore Info ===");
        player.sendMessage("§eOre Type: §a" + oreType.getDisplayName());
        player.sendMessage("§eCooldown: §b" + oreType.getCooldown() + " seconds");

        if (dataManager.isOnCooldown(player)) {
            long remaining = dataManager.getRemainingCooldown(player);
            player.sendMessage("§cCurrent Cooldown: §4" + remaining + " seconds");
        } else {
            AbilityActivationManager activationManager = plugin.getActivationManager();
            if (activationManager.isBedrockMode(player)) {
                player.sendMessage("§aAbility Ready! §7(/ability to use)");
            } else {
                player.sendMessage("§aAbility Ready! §7(Shift+Left-click to use)");
            }
        }

        player.sendMessage("§7Type §e/ore " + oreType.getDisplayName().toLowerCase() + " §7for details");
        player.sendMessage("§7Type §e/ore help §7for all commands");
    }

    private void showAllOres(Player player) {
        player.sendMessage("§6=== All Ore Types ===");
        player.sendMessage("§aStarter Ores:");
        player.sendMessage("  §7• §eDirt §7- Earth's Blessing");
        player.sendMessage("  §7• §eWood §7- Lumberjack's Fury");
        player.sendMessage("  §7• §eStone §7- Stone Skin");
        player.sendMessage("");
        player.sendMessage("§bCraftable Ores:");
        player.sendMessage("  §7• §eCoal §7- Sizzle");
        player.sendMessage("  §7• §eCopper §7- Channel The Clouds");
        player.sendMessage("  §7• §eIron §7- Bucket Roulette");
        player.sendMessage("  §7• §eGold §7- Goldrush");
        player.sendMessage("  §7• §eRedstone §7- Sticky Slime");
        player.sendMessage("  §7• §eLapis §7- Level Replenish");
        player.sendMessage("  §7• §eEmerald §7- Bring Home The Effects");
        player.sendMessage("  §7• §eAmethyst §7- Crystal Cluster");
        player.sendMessage("  §7• §eDiamond §7- Gleaming Power");
        player.sendMessage("  §7• §eNetherite §7- Debris, Debris, Debris");
        player.sendMessage("");
        player.sendMessage("§7Use §e/ore <orename> §7for detailed information");
        player.sendMessage("§7Use §e/recipes §7to view crafting recipes");
    }

    private void handleOreInfoCommand(Player player, String[] args) {
        if (args.length < 2) {
            showAllOres(player);
            return;
        }

        String oreName = args[1].toUpperCase();
        OreType oreType;

        try {
            oreType = OreType.valueOf(oreName);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cUnknown ore: §e" + args[1]);
            player.sendMessage("§7Use §e/ore list §7to see all ores");
            return;
        }

        showDetailedOreInfo(player, oreType);
    }

    private void showDetailedOreInfo(Player player, OreType oreType) {
        player.sendMessage("§6=== " + oreType.getDisplayName() + " Ore ===");
        player.sendMessage("§eCooldown: §b" + oreType.getCooldown() + " seconds");
        player.sendMessage("§eType: " + (oreType.isStarter() ? "§aStarter Ore" : "§bCraftable Ore"));

        // Get ability info
        String[] abilityInfo = getOreAbilityInfo(oreType);
        player.sendMessage("");
        player.sendMessage("§6Ability: §e" + abilityInfo[0]);
        player.sendMessage("§7" + abilityInfo[1]);
        player.sendMessage("");
        player.sendMessage("§a✓ Upside: §7" + abilityInfo[2]);
        player.sendMessage("§c✗ Downside: §7" + abilityInfo[3]);

        // Add recipe hint for craftable ores
        if (!oreType.isStarter()) {
            player.sendMessage("");
            player.sendMessage("§7Use §e/recipes " + oreType.getDisplayName().toLowerCase() + " §7to view crafting recipe");
        }

        // Add admin command hint
        if (player.hasPermission("oreabilities.admin")) {
            player.sendMessage("");
            player.sendMessage("§7Admin: §e/ore change " + oreType.getDisplayName().toLowerCase());
        }
    }

    private String[] getOreAbilityInfo(OreType oreType) {
        // Returns [Ability Name, Ability Description, Upside, Downside]
        switch (oreType) {
            case DIRT:
                return new String[]{
                        "Earth's Blessing",
                        "If standing on grass or dirt, get +8 absorption hearts for 15 seconds",
                        "Leather armor acts like diamond and is unbreakable",
                        "Without full leather armor, have mining fatigue 1"
                };
            case WOOD:
                return new String[]{
                        "Lumberjack's Fury",
                        "Axes deal 1.5x damage for 5 seconds",
                        "Apples act like golden apples",
                        "Max efficiency for axes is level 3"
                };
            case STONE:
                return new String[]{
                        "Stone Skin",
                        "Give resistance 1 for 5 seconds",
                        "Standing on stone gives regeneration 1",
                        "Slowness 1 when on stone"
                };
            case COAL:
                return new String[]{
                        "Sizzle",
                        "Smelt the item in your hand (works with stacks)",
                        "Do +1 damage when on fire",
                        "Going in water does damage, rain burns you - find shelter!"
                };
            case COPPER:
                return new String[]{
                        "Channel The Clouds",
                        "For 10 seconds, all players on hit get struck with lightning",
                        "Auto enchants tridents with channeling",
                        "Armor breaks 1.5x as fast"
                };
            case IRON:
                return new String[]{
                        "Bucket Roulette",
                        "Give yourself randomly 1 water, lava, or normal bucket",
                        "Gives permanent +2 armor attribute",
                        "Every 10 minutes one item randomly drops from inventory"
                };
            case GOLD:
                return new String[]{
                        "Goldrush",
                        "Gain haste 5 and speed 3 for 10 seconds",
                        "Golden apples give 2x the amount of absorption hearts",
                        "Crafted tools/weapons/armor get random durability (1-100)"
                };
            case REDSTONE:
                return new String[]{
                        "Sticky Slime",
                        "The next player you hit cannot jump for 10 seconds",
                        "Dripstone doesn't do damage to you",
                        "Bees and slimes do 5x damage"
                };
            case LAPIS:
                return new String[]{
                        "Level Replenish",
                        "Enchanting costs no levels and EXP gives regen (lasts 30 seconds)",
                        "Using anvils doesn't use levels",
                        "You cannot trade with villagers"
                };
            case EMERALD:
                return new String[]{
                        "Bring Home The Effects",
                        "Get every beacon effect at level one for 20 seconds",
                        "Infinite hero of the village 10",
                        "Need at least 4 stacks of emeralds in inventory or get weakness 1"
                };
            case AMETHYST:
                return new String[]{
                        "Crystal Cluster",
                        "For 10 seconds go into crystal mode: no knockback and no damage",
                        "Amethyst shards in offhand give +1.1x attack damage",
                        "Permanent glowing (purple glow)"
                };
            case DIAMOND:
                return new String[]{
                        "Gleaming Power",
                        "When using a diamond sword, do 1.4x damage for 5 seconds",
                        "Armor takes 1.5x longer to break",
                        "Every ore you break has 50% chance of not dropping"
                };
            case NETHERITE:
                return new String[]{
                        "Debris, Debris, Debris",
                        "Convert Ancient Debris in hand to Netherite Ingots",
                        "No fire damage",
                        "50% chance water buckets turn into lava buckets when placed"
                };
            default:
                return new String[]{"Unknown", "Unknown ability", "Unknown upside", "Unknown downside"};
        }
    }

    private void handleSetOreCommand(Player player, PlayerDataManager dataManager, String[] args) {
        if (!player.hasPermission("oreabilities.admin")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: §e/ore change <oretype> [player]");
            player.sendMessage("§7Available ores: Dirt, Wood, Stone, Coal, Copper, Iron, Gold, Redstone, Lapis, Emerald, Amethyst, Diamond, Netherite");
            return;
        }

        String oreName = args[1].toUpperCase();
        OreType oreType;

        try {
            oreType = OreType.valueOf(oreName);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cUnknown ore type: §e" + args[1]);
            player.sendMessage("§7Use §e/ore list §7to see all available ores");
            return;
        }

        Player target; // Default to self
        if (args.length >= 3) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                player.sendMessage("§cPlayer '" + args[2] + "' not found or not online!");
                return;
            }
        } else {
            target = player;
        }

        // IMPORTANT: Clean up old ore effects before setting new one
        OreType oldOreType = dataManager.getPlayerOre(target);
        if (oldOreType != null) {
            plugin.getPlayerListener().cleanupPlayerEffects(target, oldOreType);
            target.sendMessage("§7Removed " + oldOreType.getDisplayName() + " ore effects...");
        }

        // Set the new ore type
        dataManager.setPlayerOre(target, oreType);

        // Apply new ore effects
        target.sendMessage("§eApplying " + oreType.getDisplayName() + " ore effects...");

        // Restart action bar to show new ore type immediately
        plugin.getActionBarManager().stopActionBar(target);
        plugin.getActionBarManager().startActionBar(target);

        // Apply new passive effects after a short delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // This calls the fixed method from PlayerListener
            plugin.getAbilityManager().restartPlayerTimers(target);
        }, 5L);

        if (target.equals(player)) {
            player.sendMessage("§aChanged your ore to §e" + oreType.getDisplayName() + "§a!");
        } else {
            player.sendMessage("§aChanged " + target.getName() + "'s ore to §e" + oreType.getDisplayName() + "§a!");
            target.sendMessage("§eAn admin changed your ore to §a" + oreType.getDisplayName() + "§e!");
        }
    }

    private void handleResetCommand(Player player, String[] args) {
        if (!player.hasPermission("oreabilities.admin")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: §e/ore reset <player>");
            return;
        }

        // Make target effectively final from the start
        final Player finalTarget = plugin.getServer().getPlayer(args[1]);
        if (finalTarget == null) {
            player.sendMessage("§cPlayer '" + args[1] + "' not found or not online!");
            return;
        }

        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oldOreType = dataManager.getPlayerOre(finalTarget);

        // Clean up old effects
        if (oldOreType != null) {
            plugin.getPlayerListener().cleanupPlayerEffects(finalTarget, oldOreType);
        }

        // Reset to random starter ore
        OreType newOreType = OreType.getRandomStarter();
        dataManager.setPlayerOre(finalTarget, newOreType);

        // Restart action bar and apply new effects
        plugin.getActionBarManager().stopActionBar(finalTarget);
        plugin.getActionBarManager().startActionBar(finalTarget);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getAbilityManager().restartPlayerTimers(finalTarget);
        }, 5L);

        player.sendMessage("§aReset " + finalTarget.getName() + "'s ore to §e" + newOreType.getDisplayName() + "§a!");
        finalTarget.sendMessage("§eYour ore has been reset to §a" + newOreType.getDisplayName() + " §eby an admin!");
    }

    private void handleBedrockCommand(Player player) {
        AbilityActivationManager activationManager = plugin.getActivationManager();
        boolean currentMode = activationManager.isBedrockMode(player);
        activationManager.setBedrockMode(player, !currentMode);

        if (!currentMode) {
            player.sendMessage("§aBedrock mode enabled! Use §e/ability §eto activate abilities.");
        } else {
            player.sendMessage("§7Bedrock mode disabled. Use §eShift+Left-click §7to activate abilities.");
        }
    }

    private void handleAbilityCommand(Player player) {
        // This is for bedrock players to activate abilities
        AbilityActivationManager activationManager = plugin.getActivationManager();
        if (!activationManager.isBedrockMode(player)) {
            player.sendMessage("§cThis command is only for Bedrock Edition players!");
            player.sendMessage("§7Use §e/bedrock §7to enable Bedrock mode");
            return;
        }

        boolean success = plugin.getAbilityManager().useAbility(player);
        if (success) {
            player.sendMessage("§8Ability activated!");
        }
    }

    private void showHelp(Player player) {
        player.sendMessage("§6=== Ore Abilities Help ===");
        player.sendMessage("§eBasic Commands:");
        player.sendMessage("  §e/ore §7- Show your current ore info");
        player.sendMessage("  §e/ore list §7- Show all available ores");
        player.sendMessage("  §e/ore <orename> §7- Get detailed ore info");
        player.sendMessage("  §e/ore recipes [ore] §7- View crafting recipes");
        player.sendMessage("  §e/recipes [ore] §7- View crafting recipes (direct command)");
        player.sendMessage("  §e/ability §7- Use ability (Bedrock mode only)");
        player.sendMessage("  §e/bedrock §7- Toggle Bedrock Edition mode");
        player.sendMessage("");

        if (player.hasPermission("oreabilities.admin")) {
            player.sendMessage("§cAdmin Commands:");
            player.sendMessage("  §e/ore change <ore> [player] §7- Set ore type");
            player.sendMessage("  §e/ore reset <player> §7- Reset player's ore");
            player.sendMessage("  §e/ore reload §7- Reload plugin");
            player.sendMessage("  §e/orecd reset [player] §7- Reset ore cooldown");
            player.sendMessage("");
        }

        player.sendMessage("§6Trust System:");
        player.sendMessage("  §e/trust <player> §7- Send trust request");
        player.sendMessage("  §e/untrust <player> §7- Remove trust");
        player.sendMessage("  §e/trustlist §7- List trusted players");
        player.sendMessage("");


        player.sendMessage("§6How to Play:");
        AbilityActivationManager activationManager = plugin.getActivationManager();
        if (activationManager.isBedrockMode(player)) {
            player.sendMessage("  §7- Use §e/ability §7to activate abilities");
        } else {
            player.sendMessage("  §7- §eShift + Left-click §7to activate abilities");
        }
        player.sendMessage("  §7- Craft ore items to unlock new ore types");
        player.sendMessage("  §7- Trust players to prevent friendly fire");
        player.sendMessage("  §7- Each ore has unique abilities and effects");
        player.sendMessage("  §7- View recipes with §e/recipes §7command");
        player.sendMessage("");
        player.sendMessage("§cNote: §725% chance to shatter when crafting ores!");
    }
}