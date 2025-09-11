package hs.orePlugin;

import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final OreAbilitiesPlugin plugin;
    private final Map<UUID, OreType> playerOres = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PlayerDataManager(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
        loadPlayerData();
    }

    public void loadPlayerData() {
        FileConfiguration config = plugin.getPlayerDataConfig();

        if (config.getConfigurationSection("players") != null) {
            for (String uuidString : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String oreTypeName = config.getString("players." + uuidString + ".ore");

                    if (oreTypeName != null) {
                        try {
                            OreType oreType = OreType.valueOf(oreTypeName.toUpperCase());
                            playerOres.put(uuid, oreType);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid ore type for player " + uuidString + ": " + oreTypeName);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in player data: " + uuidString);
                }
            }
        }

        plugin.getLogger().info("Loaded ore data for " + playerOres.size() + " players");
    }

    public void savePlayerData() {
        FileConfiguration config = plugin.getPlayerDataConfig();

        // Clear existing data
        config.set("players", null);

        // Save all player ore data
        for (Map.Entry<UUID, OreType> entry : playerOres.entrySet()) {
            String uuidString = entry.getKey().toString();
            config.set("players." + uuidString + ".ore", entry.getValue().name());
        }

        plugin.savePlayerData();
    }

    public OreType getPlayerOre(Player player) {
        UUID uuid = player.getUniqueId();
        OreType oreType = playerOres.get(uuid);

        // If player doesn't have an ore type, assign a random starter ore
        if (oreType == null) {
            oreType = OreType.getRandomStarter();
            setPlayerOre(player, oreType);
            player.sendMessage("§6Welcome! You've been assigned the " + oreType.getDisplayName() + " ore type!");
            player.sendMessage("§7Use §e/ore help §7to learn more about abilities!");
        }

        return oreType;
    }

    public void setPlayerOre(Player player, OreType oreType) {
        UUID uuid = player.getUniqueId();
        playerOres.put(uuid, oreType);

        // Auto-save after changes
        savePlayerData();

        plugin.getLogger().info("Set " + player.getName() + " ore type to " + oreType.getDisplayName());
    }

    public boolean isOnCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) {
            return false;
        }

        long cooldownEnd = cooldowns.get(uuid);
        long currentTime = System.currentTimeMillis();

        if (currentTime >= cooldownEnd) {
            cooldowns.remove(uuid);
            return false;
        }

        return true;
    }

    public long getRemainingCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) {
            return 0;
        }

        long cooldownEnd = cooldowns.get(uuid);
        long currentTime = System.currentTimeMillis();

        if (currentTime >= cooldownEnd) {
            cooldowns.remove(uuid);
            return 0;
        }

        return (cooldownEnd - currentTime) / 1000; // Convert to seconds
    }

    public void setCooldown(Player player, int seconds) {
        UUID uuid = player.getUniqueId();
        long cooldownEnd = System.currentTimeMillis() + (seconds * 1000L);
        cooldowns.put(uuid, cooldownEnd);

        plugin.getLogger().info("Set cooldown for " + player.getName() + ": " + seconds + " seconds");
    }

    // NEW: Clear cooldown method for admin command
    public void clearCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.remove(uuid);
        plugin.getLogger().info("Cleared cooldown for " + player.getName());
    }

    public void cleanup(Player player) {
        // Optional: Clean up cooldowns when player leaves
        // We'll keep them for now since they should persist across sessions
        UUID uuid = player.getUniqueId();
        plugin.getLogger().info("Player " + player.getName() + " left - keeping cooldown data");
    }

    // Get all players with ore types (for debugging/admin purposes)
    public Map<UUID, OreType> getAllPlayerOres() {
        return new HashMap<>(playerOres);
    }

    // Remove a player's ore data completely (admin command)
    public void removePlayerData(UUID uuid) {
        playerOres.remove(uuid);
        cooldowns.remove(uuid);
        savePlayerData();
    }

    // ADDED: Check if player has an ore type assigned (without auto-assigning)
    public boolean hasPlayerOre(Player player) {
        return playerOres.containsKey(player.getUniqueId());
    }

    // ADDED: Manually assign a random starter ore to a player
    public void assignRandomStarterOre(Player player) {
        OreType randomOre = OreType.getRandomStarter();
        setPlayerOre(player, randomOre);
        player.sendMessage("§6Welcome! You've been assigned the " + randomOre.getDisplayName() + " ore type!");
        player.sendMessage("§7Use §e/ore help §7to learn more about abilities!");
        plugin.getLogger().info("Assigned random starter ore " + randomOre.getDisplayName() + " to " + player.getName());
    }

    // ADDED: Get player ore without auto-assignment (returns null if none)
    public OreType getPlayerOreRaw(Player player) {
        return playerOres.get(player.getUniqueId());
    }
}