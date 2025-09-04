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

        if (args.length == 0) {
            showPlayerInfo(player, dataManager);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info":
                showPlayerInfo(player, dataManager);
                break;
            case "oreinfo":
                handleOreInfoCommand(player, args);
                break;
            case "help":
                showHelp(player);
                break;
            case "set":
                handleSetOreCommand(player, dataManager, args);
                break;
            case "bedrock":
                handleBedrockCommand(player);
                break;
            case "ability":
                handleAbilityCommand(player);
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
                if (player.hasPermission("oreabilities.admin") && args.length == 2) {
                    Player target = plugin.getServer().getPlayer(args[1]);
                    if (target != null) {
                        dataManager.setPlayerOre(target, OreType.getRandomStarter());
                        player.sendMessage("§aReset " + target.getName() + "'s ore type!");
                        target.sendMessage("§eYour ore type has been reset by an admin!");

                        // Restart action bar to show new ore type
                        plugin.getActionBarManager().stopActionBar(target);
                        plugin.getActionBarManager().startActionBar(target);
                    } else {
                        player.sendMessage("§cPlayer not found!");
                    }
                } else {
                    player.sendMessage("§cUsage: /oreabilities reset <player> (Admin only)");
                }
                break;
            default:
                player.sendMessage("§cUnknown command. Use /oreabilities help for help.");
                break;
        }

        return true;
    }

    private void showPlayerInfo(Player player, PlayerDataManager dataManager) {
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == null) {
            player.sendMessage("§cYou don't have an ore type assigned!");
            return;
        }

        player.sendMessage("§6=== Your Ore Abilities Info ===");
        player.sendMessage("§eYour Ore Type: §a" + oreType.getDisplayName());
        player.sendMessage("§eAbility Cooldown: §b" + oreType.getCooldown() + " seconds");

        if (dataManager.isOnCooldown(player)) {
            long remaining = dataManager.getRemainingCooldown(player);
            player.sendMessage("§cCurrent Cooldown: §4" + remaining + " seconds");
        } else {
            AbilityActivationManager activationManager = plugin.getActivationManager();
            if (activationManager.isBedrockMode(player)) {
                player.sendMessage("§aAbility Ready! §7(/ability to use)");
            } else {
                player.sendMessage("§aAbility Ready! §7(Shift+Right-click to use)");
            }
        }

        player.sendMessage("§7Use §e/oreabilities oreinfo " + oreType.getDisplayName().toLowerCase() + " §7for detailed info");
        player.sendMessage("§7Use §e/oreabilities help §7for all commands");
    }

    private void handleOreInfoCommand(Player player, String[] args) {
        if (args.length < 2) {
            // Show list of all ores
            player.sendMessage("§6=== All Ore Types ===");
            player.sendMessage("§eStarter Ores: §aDirt, Wood, Stone");
            player.sendMessage("§eCraftable Ores: §bCoal, Copper, Iron, Gold, Redstone, Lapis, Emerald, Amethyst, Diamond, Netherite");
            player.sendMessage("§7Use §e/oreabilities oreinfo <orename> §7for detailed information");
            return;
        }

        String oreName = args[1].toUpperCase();
        OreType oreType;

        try {
            oreType = OreType.valueOf(oreName);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cUnknown ore type: " + args[1]);
            player.sendMessage("§7Available ores: Dirt, Wood, Stone, Coal, Copper, Iron, Gold, Redstone, Lapis, Emerald, Amethyst, Diamond, Netherite");
            return;
        }

        showDetailedOreInfo(player, oreType);
    }

    private void showDetailedOreInfo(Player player, OreType oreType) {
        player.sendMessage("§6=== " + oreType.getDisplayName() + " Ore Information ===");
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
    }

    private String[] getOreAbilityInfo(OreType oreType) {
        // Returns [Ability Name, Ability Description, Upside, Downside]
        switch (oreType) {
            case DIRT:
                return new String[]{
                        "Earth's Blessing",
                        "If standing on grass or dirt, get +2 hearts for 15 seconds",
                        "Leather armor acts like diamond and is unbreakable",
                        "When wearing leather, have mining fatigue 1"
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
                        "Give resistance 1 for 10 seconds",
                        "Standing on stone gives regeneration 1",
                        "Slowness 1 when on stone"
                };
            case COAL:
                return new String[]{
                        "Sizzle",
                        "Smelt the ore in your hand (works with stacks)",
                        "Do +1 damage when on fire",
                        "Going in water does damage"
                };
            case COPPER:
                return new String[]{
                        "Channel The Clouds",
                        "For 10 seconds, all players on hit get struck with lightning",
                        "Auto enchants with channeling",
                        "Armor breaks 2x as fast"
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
                        "Splashing exp gives regeneration 1 for 5 seconds (lasts 30 seconds)",
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
                        "Amethyst shards in offhand give +1.5 attack damage",
                        "Permanent glowing (purple glow)"
                };
            case DIAMOND:
                return new String[]{
                        "Gleaming Power",
                        "When using a diamond sword, do 2x damage for 5 seconds",
                        "Armor takes 2x longer to break",
                        "Every ore you break has 50% chance of not dropping"
                };
            case NETHERITE:
                return new String[]{
                        "Debris, Debris, Debris",
                        "Upgrades the item in your hand to netherite",
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
            player.sendMessage("§cUsage: /oreabilities set <oretype> [player]");
            player.sendMessage("§7Available ores: Dirt, Wood, Stone, Coal, Copper, Iron, Gold, Redstone, Lapis, Emerald, Amethyst, Diamond, Netherite");
            return;
        }

        String oreName = args[1].toUpperCase();
        OreType oreType;

        try {
            oreType = OreType.valueOf(oreName);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cUnknown ore type: " + args[1]);
            player.sendMessage("§7Available ores: Dirt, Wood, Stone, Coal, Copper, Iron, Gold, Redstone, Lapis, Emerald, Amethyst, Diamond, Netherite");
            return;
        }

        Player target = player; // Default to self
        if (args.length >= 3) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                player.sendMessage("§cPlayer '" + args[2] + "' not found or not online!");
                return;
            }
        }

        // Set the ore type
        dataManager.setPlayerOre(target, oreType);

        // Restart action bar to show new ore type immediately
        plugin.getActionBarManager().stopActionBar(target);
        plugin.getActionBarManager().startActionBar(target);

        if (target.equals(player)) {
            player.sendMessage("§aYou have been given the " + oreType.getDisplayName() + " ore type!");
        } else {
            player.sendMessage("§aGave " + target.getName() + " the " + oreType.getDisplayName() + " ore type!");
            target.sendMessage("§eYou have been given the " + oreType.getDisplayName() + " ore type by an admin!");
        }
    }

    private void handleBedrockCommand(Player player) {
        AbilityActivationManager activationManager = plugin.getActivationManager();
        boolean currentMode = activationManager.isBedrockMode(player);
        activationManager.setBedrockMode(player, !currentMode);
    }

    private void handleAbilityCommand(Player player) {
        // This is for bedrock players to activate abilities
        AbilityActivationManager activationManager = plugin.getActivationManager();
        if (!activationManager.isBedrockMode(player)) {
            player.sendMessage("§cThis command is only for Bedrock Edition players!");
            player.sendMessage("§7Use §e/oreabilities bedrock §7to enable Bedrock mode");
            return;
        }

        boolean success = plugin.getAbilityManager().useAbility(player);
        if (success) {
            player.sendMessage("§8Ability activated via command");
        }
    }

    private void showHelp(Player player) {
        player.sendMessage("§6=== Ore Abilities Help ===");
        player.sendMessage("§e/oreabilities info §7- Show your current ore info");
        player.sendMessage("§e/oreabilities oreinfo [orename] §7- Show detailed ore information");
        player.sendMessage("§e/oreabilities bedrock §7- Toggle Bedrock Edition mode");
        player.sendMessage("§e/oreabilities ability §7- Use ability (Bedrock mode only)");
        player.sendMessage("");

        if (player.hasPermission("oreabilities.admin")) {
            player.sendMessage("§c=== Admin Commands ===");
            player.sendMessage("§e/oreabilities set <ore> [player] §7- Set ore type");
            player.sendMessage("§e/oreabilities reset <player> §7- Reset player's ore");
            player.sendMessage("§e/oreabilities reload §7- Reload plugin");
            player.sendMessage("");
        }

        player.sendMessage("§6Other Commands:");
        player.sendMessage("§e/trust <player> §7- Send trust request");
        player.sendMessage("§e/untrust <player> §7- Remove trust");
        player.sendMessage("§e/trustlist §7- List trusted players");
        player.sendMessage("");
        player.sendMessage("§6How to use:");

        AbilityActivationManager activationManager = plugin.getActivationManager();
        if (activationManager.isBedrockMode(player)) {
            player.sendMessage("§7- Use §e/oreabilities ability §7to activate abilities");
        } else {
            player.sendMessage("§7- §eShift + Right-click §7to activate abilities");
        }

        player.sendMessage("§7- Craft ore items to unlock new ore types");
        player.sendMessage("§7- Trust players to prevent friendly fire");
        player.sendMessage("§7- Each ore has unique abilities and effects");
        player.sendMessage("");
        player.sendMessage("§cNote: §725% chance to shatter when crafting ores!");
    }
}