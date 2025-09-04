package hs.orePlugin.Commands;

import hs.orePlugin.Managers.OreManager;
import hs.orePlugin.Managers.PlayerDataManager;
import hs.orePlugin.OreType;
import hs.orePlugin.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OreCommand implements CommandExecutor {
    private final OreManager oreManager;
    private final PlayerDataManager playerDataManager;

    public OreCommand(OreManager oreManager, PlayerDataManager playerDataManager) {
        this.oreManager = oreManager;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "ability":
            case "use":
                oreManager.useOreAbility(player);
                break;

            case "info":
            case "status":
                showPlayerInfo(player);
                break;

            case "list":
                showOreList(player);
                break;

            case "cooldown":
            case "cd":
                showCooldowns(player);
                break;

            default:
                showHelp(player);
                break;
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Ore Plugin Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/ore ability" + ChatColor.WHITE + " - Use your ore ability");
        player.sendMessage(ChatColor.YELLOW + "/ore info" + ChatColor.WHITE + " - Show your current ore info");
        player.sendMessage(ChatColor.YELLOW + "/ore list" + ChatColor.WHITE + " - List all available ores");
        player.sendMessage(ChatColor.YELLOW + "/ore cooldown" + ChatColor.WHITE + " - Show ability cooldowns");
    }

    private void showPlayerInfo(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player);
        OreType ore = data.getCurrentOre();

        player.sendMessage(ChatColor.GOLD + "=== Your Ore Info ===");

        if (ore == null) {
            player.sendMessage(ChatColor.RED + "You don't have an ore assigned!");
            return;
        }

        player.sendMessage(ChatColor.WHITE + "Current Ore: " + ore.getColor() + ore.getDisplayName());
        player.sendMessage(ChatColor.WHITE + "Ability: " + ore.getColor() + ore.getAbilityName());
        player.sendMessage(ChatColor.WHITE + "Cooldown: " + ChatColor.YELLOW + ore.getCooldown() + " seconds");

        if (data.isOnCooldown(ore)) {
            long remaining = data.getRemainingCooldown(ore);
            player.sendMessage(ChatColor.RED + "On cooldown for " + (remaining / 1000) + " seconds");
        } else {
            player.sendMessage(ChatColor.GREEN + "Ready to use!");
        }

        showOreDetails(player, ore);
    }

    private void showOreDetails(Player player, OreType ore) {
        player.sendMessage(ChatColor.GOLD + "=== " + ore.getDisplayName() + " Details ===");

        switch (ore) {
            case DIRT:
                player.sendMessage(ChatColor.WHITE + "Ability: +2 hearts for 15s when on grass/dirt");
                player.sendMessage(ChatColor.GREEN + "Upside: Leather armor acts like diamond and is unbreakable");
                player.sendMessage(ChatColor.RED + "Downside: Mining fatigue 1 when wearing leather");
                break;
            case WOOD:
                player.sendMessage(ChatColor.WHITE + "Ability: Axes deal 1.5x damage for 5s");
                player.sendMessage(ChatColor.GREEN + "Upside: Apples act like golden apples");
                player.sendMessage(ChatColor.RED + "Downside: Max efficiency for axes is level 3");
                break;
            case STONE:
                player.sendMessage(ChatColor.WHITE + "Ability: Resistance 1 for 10s");
                player.sendMessage(ChatColor.GREEN + "Upside: Standing on stone gives regeneration 1");
                player.sendMessage(ChatColor.RED + "Downside: Slowness 1 when on stone");
                break;
            case COAL:
                player.sendMessage(ChatColor.WHITE + "Ability: Smelt ore in hand (works with stacks)");
                player.sendMessage(ChatColor.GREEN + "Upside: +1 damage when on fire");
                player.sendMessage(ChatColor.RED + "Downside: Going in water does damage");
                break;
            case COPPER:
                player.sendMessage(ChatColor.WHITE + "Ability: Players hit get struck by lightning for 10s");
                player.sendMessage(ChatColor.GREEN + "Upside: Auto enchants with Channeling");
                player.sendMessage(ChatColor.RED + "Downside: Armor breaks 2x as fast");
                break;
            case IRON:
                player.sendMessage(ChatColor.WHITE + "Ability: Get random bucket (water/lava/empty)");
                player.sendMessage(ChatColor.GREEN + "Upside: Permanent +2 armor attribute");
                player.sendMessage(ChatColor.RED + "Downside: Random item drops every 10 minutes");
                break;
            case GOLD:
                player.sendMessage(ChatColor.WHITE + "Ability: Haste 5 and Speed 3 for 10s");
                player.sendMessage(ChatColor.GREEN + "Upside: Golden apples give 2x absorption hearts");
                player.sendMessage(ChatColor.RED + "Downside: Crafted items get random durability 1-100");
                break;
            case REDSTONE:
                player.sendMessage(ChatColor.WHITE + "Ability: Next player hit can't jump for 10s");
                player.sendMessage(ChatColor.GREEN + "Upside: Dripstone doesn't damage you");
                player.sendMessage(ChatColor.RED + "Downside: Bees and slimes do 5x damage");
                break;
            case LAPIS:
                player.sendMessage(ChatColor.WHITE + "Ability: Splashing exp gives regen for 30s");
                player.sendMessage(ChatColor.GREEN + "Upside: Using anvils doesn't use levels");
                player.sendMessage(ChatColor.RED + "Downside: Cannot trade with villagers");
                break;
            case EMERALD:
                player.sendMessage(ChatColor.WHITE + "Ability: All beacon effects level 1 for 20s");
                player.sendMessage(ChatColor.GREEN + "Upside: Infinite Hero of the Village 10");
                player.sendMessage(ChatColor.RED + "Downside: Weakness 1 without 4 stacks of emeralds");
                break;
            case AMETHYST:
                player.sendMessage(ChatColor.WHITE + "Ability: Crystal mode - no knockback/damage for 10s");
                player.sendMessage(ChatColor.GREEN + "Upside: +1.5 attack damage with amethyst shards in offhand");
                player.sendMessage(ChatColor.RED + "Downside: Permanent purple glow");
                break;
            case DIAMOND:
                player.sendMessage(ChatColor.WHITE + "Ability: 2x damage with diamond sword for 5s");
                player.sendMessage(ChatColor.GREEN + "Upside: Armor takes 2x longer to break");
                player.sendMessage(ChatColor.RED + "Downside: 50% chance ores don't drop when broken");
                break;
            case NETHERITE:
                player.sendMessage(ChatColor.WHITE + "Ability: Upgrade item in hand to netherite");
                player.sendMessage(ChatColor.GREEN + "Upside: No fire damage");
                player.sendMessage(ChatColor.RED + "Downside: 50% chance water buckets turn to lava when placed");
                break;
        }
    }

    private void showOreList(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Available Ores ===");
        player.sendMessage(ChatColor.YELLOW + "Starter Ores (random on join):");
        for (OreType ore : OreType.getStarterOres()) {
            player.sendMessage(ChatColor.WHITE + "- " + ore.getColor() + ore.getDisplayName() +
                    ChatColor.WHITE + " (" + ore.getAbilityName() + ")");
        }

        player.sendMessage(ChatColor.YELLOW + "Craftable Ores:");
        for (OreType ore : OreType.getCraftableOres()) {
            player.sendMessage(ChatColor.WHITE + "- " + ore.getColor() + ore.getDisplayName() +
                    ChatColor.WHITE + " (" + ore.getAbilityName() + ")");
        }
    }

    private void showCooldowns(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player);
        OreType ore = data.getCurrentOre();

        if (ore == null) {
            player.sendMessage(ChatColor.RED + "You don't have an ore assigned!");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Ability Cooldowns ===");

        if (data.isOnCooldown(ore)) {
            long remaining = data.getRemainingCooldown(ore);
            player.sendMessage(ore.getColor() + ore.getDisplayName() + ChatColor.WHITE + ": " +
                    ChatColor.RED + (remaining / 1000) + "s remaining");
        } else {
            player.sendMessage(ore.getColor() + ore.getDisplayName() + ChatColor.WHITE + ": " +
                    ChatColor.GREEN + "Ready!");
        }
    }
}