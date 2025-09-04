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
            case "help":
                showHelp(player);
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

        player.sendMessage("§6=== Ore Abilities Info ===");
        player.sendMessage("§eYour Ore Type: §a" + oreType.getDisplayName());
        player.sendMessage("§eAbility Cooldown: §b" + oreType.getCooldown() + " seconds");

        if (dataManager.isOnCooldown(player)) {
            long remaining = dataManager.getRemainingCooldown(player);
            player.sendMessage("§cCurrent Cooldown: §4" + remaining + " seconds");
        } else {
            player.sendMessage("§aAbility Ready! §7(Right-click to use)");
        }

        player.sendMessage("§7Use /oreabilities help for ability details");
    }

    private void showHelp(Player player) {
        player.sendMessage("§6=== Ore Abilities Help ===");
        player.sendMessage("§e/oreabilities info §7- Show your current ore info");
        player.sendMessage("§e/trust <player> §7- Send trust request");
        player.sendMessage("§e/untrust <player> §7- Remove trust");
        player.sendMessage("§e/trustlist §7- List trusted players");
        player.sendMessage("");
        player.sendMessage("§6How to use:");
        player.sendMessage("§7- Right-click to activate your ore ability");
        player.sendMessage("§7- Craft ore items to unlock new ore types");
        player.sendMessage("§7- Trust players to prevent friendly fire");
        player.sendMessage("§7- Each ore has unique abilities and effects");
        player.sendMessage("");
        player.sendMessage("§cNote: §725% chance to shatter when crafting ores!");
    }
}