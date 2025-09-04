package hs.orePlugin.Commands;

import hs.orePlugin.Managers.TrustManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TrustCommand implements CommandExecutor {
    private final TrustManager trustManager;

    public TrustCommand(TrustManager trustManager) {
        this.trustManager = trustManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /trust <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found!");
            return true;
        }

        // Check if there's a pending request from the target to this player
        if (trustManager.hasPendingRequest(player, target)) {
            // Accept the trust request
            trustManager.acceptTrustRequest(player, target);
        } else {
            // Send a new trust request
            trustManager.sendTrustRequest(player, target);
        }

        return true;
    }
}