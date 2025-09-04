package hs.orePlugin.Commands;

import hs.orePlugin.Managers.TrustManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class TrustListCommand implements CommandExecutor {
    private final TrustManager trustManager;

    public TrustListCommand(TrustManager trustManager) {
        this.trustManager = trustManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        List<String> trustedPlayers = trustManager.getTrustedPlayerNames(player);

        if (trustedPlayers.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no trusted players.");
            return true;
        }

        player.sendMessage(ChatColor.GREEN + "Trusted players:");
        for (String name : trustedPlayers) {
            player.sendMessage(ChatColor.WHITE + "- " + name);
        }

        return true;
    }
}