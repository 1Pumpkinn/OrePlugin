package hs.orePlugin.Managers;

import hs.orePlugin.OrePlugin;
import hs.orePlugin.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.*;

public class TrustManager {
    private final OrePlugin plugin;
    private final Map<UUID, Set<UUID>> pendingTrustRequests;

    public TrustManager(OrePlugin plugin) {
        this.plugin = plugin;
        this.pendingTrustRequests = new HashMap<>();
    }

    public boolean sendTrustRequest(Player sender, Player target) {
        if (sender.equals(target)) {
            sender.sendMessage(ChatColor.RED + "You cannot trust yourself!");
            return false;
        }

        PlayerData senderData = plugin.getPlayerDataManager().getPlayerData(sender);
        if (senderData.isTrusted(target.getUniqueId())) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " is already trusted!");
            return false;
        }

        // Add to pending requests
        pendingTrustRequests.computeIfAbsent(target.getUniqueId(), k -> new HashSet<>())
                .add(sender.getUniqueId());

        // Send messages
        sender.sendMessage(ChatColor.GREEN + "Trust request sent to " + target.getName() + "!");
        target.sendMessage(ChatColor.YELLOW + sender.getName() + " wants to trust you!");
        target.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.WHITE + "/trust " + sender.getName() +
                ChatColor.YELLOW + " to accept!");

        // Auto-expire request after 60 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Set<UUID> requests = pendingTrustRequests.get(target.getUniqueId());
            if (requests != null) {
                if (requests.remove(sender.getUniqueId())) {
                    if (sender.isOnline()) {
                        sender.sendMessage(ChatColor.RED + "Trust request to " + target.getName() + " expired.");
                    }
                    if (target.isOnline()) {
                        target.sendMessage(ChatColor.RED + "Trust request from " + sender.getName() + " expired.");
                    }
                }
                if (requests.isEmpty()) {
                    pendingTrustRequests.remove(target.getUniqueId());
                }
            }
        }, 1200L); // 60 seconds

        return true;
    }

    public boolean acceptTrustRequest(Player accepter, Player requester) {
        Set<UUID> requests = pendingTrustRequests.get(accepter.getUniqueId());
        if (requests == null || !requests.contains(requester.getUniqueId())) {
            accepter.sendMessage(ChatColor.RED + "No pending trust request from " + requester.getName() + "!");
            return false;
        }

        // Remove from pending requests
        requests.remove(requester.getUniqueId());
        if (requests.isEmpty()) {
            pendingTrustRequests.remove(accepter.getUniqueId());
        }

        // Add mutual trust
        PlayerData requesterData = plugin.getPlayerDataManager().getPlayerData(requester);
        PlayerData accepterData = plugin.getPlayerDataManager().getPlayerData(accepter);

        requesterData.addTrustedPlayer(accepter.getUniqueId());
        accepterData.addTrustedPlayer(requester.getUniqueId());

        // Send success messages
        requester.sendMessage(ChatColor.GREEN + accepter.getName() + " accepted your trust request!");
        accepter.sendMessage(ChatColor.GREEN + "You are now trusted with " + requester.getName() + "!");

        return true;
    }

    public boolean removeTrust(Player player, Player target) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        PlayerData targetData = plugin.getPlayerDataManager().getPlayerData(target);

        if (!playerData.isTrusted(target.getUniqueId()) && !targetData.isTrusted(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are not trusted with " + target.getName() + "!");
            return false;
        }

        // Remove mutual trust (one-sided system as requested)
        playerData.removeTrustedPlayer(target.getUniqueId());
        targetData.removeTrustedPlayer(player.getUniqueId());

        // Send messages
        player.sendMessage(ChatColor.YELLOW + "You are no longer trusted with " + target.getName() + "!");
        if (target.isOnline()) {
            target.sendMessage(ChatColor.YELLOW + player.getName() + " removed trust with you!");
        }

        return true;
    }

    public List<String> getTrustedPlayerNames(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        List<String> names = new ArrayList<>();

        for (UUID trustedId : data.getTrustedPlayers()) {
            Player trustedPlayer = Bukkit.getPlayer(trustedId);
            if (trustedPlayer != null) {
                names.add(trustedPlayer.getName());
            } else {
                // Try to get offline player name
                names.add(Bukkit.getOfflinePlayer(trustedId).getName());
            }
        }

        return names;
    }

    public boolean areMutuallyTrusted(Player player1, Player player2) {
        PlayerData data1 = plugin.getPlayerDataManager().getPlayerData(player1);
        PlayerData data2 = plugin.getPlayerDataManager().getPlayerData(player2);

        return data1.isTrusted(player2.getUniqueId()) && data2.isTrusted(player1.getUniqueId());
    }

    public boolean hasPendingRequest(Player target, Player requester) {
        Set<UUID> requests = pendingTrustRequests.get(target.getUniqueId());
        return requests != null && requests.contains(requester.getUniqueId());
    }
}