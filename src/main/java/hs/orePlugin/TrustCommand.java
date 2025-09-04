package hs.orePlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;

public class TrustCommand implements CommandExecutor {

    private final OreAbilitiesPlugin plugin;

    public TrustCommand(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use trust commands!");
            return true;
        }

        Player player = (Player) sender;
        TrustManager trustManager = plugin.getTrustManager();

        switch (label.toLowerCase()) {
            case "trust":
                return handleTrustCommand(player, trustManager, args);
            case "untrust":
                return handleUntrustCommand(player, trustManager, args);
            case "trustlist":
                return handleTrustListCommand(player, trustManager, args);
            default:
                return false;
        }
    }

    private boolean handleTrustCommand(Player player, TrustManager trustManager, String[] args) {
        if (args.length != 1) {
            player.sendMessage("§cUsage: /trust <player>");
            return true;
        }

        String targetName = args[0];
        Player target = plugin.getServer().getPlayer(targetName);

        if (target == null) {
            player.sendMessage("§cPlayer '" + targetName + "' not found or not online!");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("§cYou cannot trust yourself!");
            return true;
        }

        trustManager.sendTrustRequest(player, target);
        return true;
    }

    private boolean handleUntrustCommand(Player player, TrustManager trustManager, String[] args) {
        if (args.length != 1) {
            player.sendMessage("§cUsage: /untrust <player>");
            return true;
        }

        String targetName = args[0];
        Player target = plugin.getServer().getPlayer(targetName);

        if (target == null) {
            player.sendMessage("§cPlayer '" + targetName + "' not found or not online!");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("§cYou cannot untrust yourself!");
            return true;
        }

        trustManager.removeTrust(player, target);
        return true;
    }

    private boolean handleTrustListCommand(Player player, TrustManager trustManager, String[] args) {
        List<String> trustedPlayers = trustManager.getTrustedPlayerNames(player);

        if (trustedPlayers.isEmpty()) {
            player.sendMessage("§eYou don't have any trusted players.");
            return true;
        }

        player.sendMessage("§aTrusted Players (" + trustedPlayers.size() + "):");
        for (String name : trustedPlayers) {
            player.sendMessage("§7- " + name);
        }

        return true;
    }
}