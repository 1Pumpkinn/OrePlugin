package hs.orePlugin;

import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.*;

public class TrustManager {

    private final OreAbilitiesPlugin plugin;
    private final Map<UUID, Set<UUID>> trustRelations = new HashMap<>();
    private final Map<UUID, Set<UUID>> pendingRequests = new HashMap<>();

    public TrustManager(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
        loadTrustData();
    }

    public void loadTrustData() {
        FileConfiguration config = plugin.getPlayerDataConfig();
        if (config.getConfigurationSection("trust") != null) {
            for (String uuidString : config.getConfigurationSection("trust").getKeys(false)) {
                UUID playerUuid = UUID.fromString(uuidString);
                List<String> trustedList = config.getStringList("trust." + uuidString + ".trusted");

                Set<UUID> trustedSet = new HashSet<>();
                for (String trustedUuidString : trustedList) {
                    try {
                        trustedSet.add(UUID.fromString(trustedUuidString));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in trust data: " + trustedUuidString);
                    }
                }

                if (!trustedSet.isEmpty()) {
                    trustRelations.put(playerUuid, trustedSet);
                }
            }
        }
    }

    public void saveTrustData() {
        FileConfiguration config = plugin.getPlayerDataConfig();

        // Clear existing trust data
        config.set("trust", null);

        for (Map.Entry<UUID, Set<UUID>> entry : trustRelations.entrySet()) {
            String playerUuid = entry.getKey().toString();
            List<String> trustedList = new ArrayList<>();

            for (UUID trustedUuid : entry.getValue()) {
                trustedList.add(trustedUuid.toString());
            }

            if (!trustedList.isEmpty()) {
                config.set("trust." + playerUuid + ".trusted", trustedList);
            }
        }

        plugin.savePlayerData();
    }

    public void sendTrustRequest(Player sender, Player target) {
        UUID senderUuid = sender.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        if (isTrusted(sender, target)) {
            sender.sendMessage("§cYou are already trusted with " + target.getName() + "!");
            return;
        }

        if (hasPendingRequest(target, sender)) {
            // Target already has a pending request from sender, auto-accept
            acceptTrustRequest(target, sender);
            return;
        }

        // Add to pending requests
        pendingRequests.computeIfAbsent(targetUuid, k -> new HashSet<>()).add(senderUuid);

        sender.sendMessage("§eTrust request sent to " + target.getName() + "!");
        target.sendMessage("§a" + sender.getName() + " wants to trust you! Use /trust " + sender.getName() + " to accept.");

        // Remove request after 60 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (hasPendingRequest(target, sender)) {
                removePendingRequest(target, sender);
                sender.sendMessage("§cTrust request to " + target.getName() + " expired.");
                target.sendMessage("§cTrust request from " + sender.getName() + " expired.");
            }
        }, 1200L); // 60 seconds
    }

    public void acceptTrustRequest(Player accepter, Player requester) {
        if (!hasPendingRequest(accepter, requester)) {
            accepter.sendMessage("§cYou don't have a trust request from " + requester.getName() + "!");
            return;
        }

        // Create mutual trust relationship
        addTrustRelation(accepter, requester);
        addTrustRelation(requester, accepter);

        // Remove pending request
        removePendingRequest(accepter, requester);

        // Notify both players
        accepter.sendMessage("§aYou are now trusted with " + requester.getName() + "!");
        requester.sendMessage("§a" + accepter.getName() + " accepted your trust request!");

        saveTrustData();
    }

    public void removeTrust(Player player, Player target) {
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        if (!isTrusted(player, target)) {
            player.sendMessage("§cYou are not trusted with " + target.getName() + "!");
            return;
        }

        // Remove mutual trust (one-sided untrust removes both)
        removeTrustRelation(player, target);
        removeTrustRelation(target, player);

        player.sendMessage("§eYou are no longer trusted with " + target.getName() + "!");
        if (target.isOnline()) {
            target.sendMessage("§e" + player.getName() + " removed trust with you!");
        }

        saveTrustData();
    }

    public boolean isTrusted(Player player1, Player player2) {
        UUID uuid1 = player1.getUniqueId();
        UUID uuid2 = player2.getUniqueId();

        Set<UUID> trusted1 = trustRelations.get(uuid1);
        Set<UUID> trusted2 = trustRelations.get(uuid2);

        return (trusted1 != null && trusted1.contains(uuid2)) &&
                (trusted2 != null && trusted2.contains(uuid1));
    }

    public Set<UUID> getTrustedPlayers(Player player) {
        return trustRelations.getOrDefault(player.getUniqueId(), new HashSet<>());
    }

    public List<String> getTrustedPlayerNames(Player player) {
        List<String> names = new ArrayList<>();
        Set<UUID> trusted = getTrustedPlayers(player);

        for (UUID uuid : trusted) {
            Player trustedPlayer = plugin.getServer().getPlayer(uuid);
            if (trustedPlayer != null) {
                names.add(trustedPlayer.getName());
            } else {
                // Try to get offline player name
                String name = plugin.getServer().getOfflinePlayer(uuid).getName();
                if (name != null) {
                    names.add(name + " (offline)");
                }
            }
        }

        return names;
    }

    private void addTrustRelation(Player player, Player target) {
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        trustRelations.computeIfAbsent(playerUuid, k -> new HashSet<>()).add(targetUuid);
    }

    private void removeTrustRelation(Player player, Player target) {
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        Set<UUID> trusted = trustRelations.get(playerUuid);
        if (trusted != null) {
            trusted.remove(targetUuid);
            if (trusted.isEmpty()) {
                trustRelations.remove(playerUuid);
            }
        }
    }

    private boolean hasPendingRequest(Player target, Player requester) {
        UUID targetUuid = target.getUniqueId();
        UUID requesterUuid = requester.getUniqueId();

        Set<UUID> requests = pendingRequests.get(targetUuid);
        return requests != null && requests.contains(requesterUuid);
    }

    private void removePendingRequest(Player target, Player requester) {
        UUID targetUuid = target.getUniqueId();
        UUID requesterUuid = requester.getUniqueId();

        Set<UUID> requests = pendingRequests.get(targetUuid);
        if (requests != null) {
            requests.remove(requesterUuid);
            if (requests.isEmpty()) {
                pendingRequests.remove(targetUuid);
            }
        }
    }
}