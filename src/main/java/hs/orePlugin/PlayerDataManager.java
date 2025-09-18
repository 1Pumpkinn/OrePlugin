package hs.orePlugin;

import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final OreAbilitiesPlugin plugin;
    private final Map<UUID, OreType> playerOres = new HashMap<>();
    private final Map<UUID, OreType> starterOres = new HashMap<>(); // NEW: Store starter ores
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
                    String starterOreName = config.getString("players." + uuidString + ".starter");

                    if (oreTypeName != null) {
                        try {
                            OreType oreType = OreType.valueOf(oreTypeName.toUpperCase());
                            playerOres.put(uuid, oreType);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid ore type for player " + uuidString + ": " + oreTypeName);
                        }
                    }

                    // NEW: Load starter ore
                    if (starterOreName != null) {
                        try {
                            OreType starterOre = OreType.valueOf(starterOreName.toUpperCase());
                            starterOres.put(uuid, starterOre);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid starter ore type for player " + uuidString + ": " + starterOreName);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in player data: " + uuidString);
                }
            }
        }

        plugin.getLogger().info("Loaded ore data for " + playerOres.size() + " players");
        plugin.getLogger().info("Loaded starter ore data for " + starterOres.size() + " players");
    }

    public void savePlayerData() {
        FileConfiguration config = plugin.getPlayerDataConfig();

        // Clear existing data
        config.set("players", null);

        // Save all player ore data
        for (Map.Entry<UUID, OreType> entry : playerOres.entrySet()) {
            String uuidString = entry.getKey().toString();
            config.set("players." + uuidString + ".ore", entry.getValue().name());

            // NEW: Also save starter ore if it exists
            OreType starterOre = starterOres.get(entry.getKey());
            if (starterOre != null) {
                config.set("players." + uuidString + ".starter", starterOre.name());
            }
        }

        // NEW: Save starter ores for players who might not have current ore data
        for (Map.Entry<UUID, OreType> entry : starterOres.entrySet()) {
            String uuidString = entry.getKey().toString();
            if (!playerOres.containsKey(entry.getKey())) {
                config.set("players." + uuidString + ".starter", entry.getValue().name());
            }
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
            // NEW: Also set this as their starter ore
            starterOres.put(uuid, oreType);
            player.sendMessage("§6Welcome! You've been assigned the " + oreType.getDisplayName() + " ore type!");
            player.sendMessage("§7Use §e/ore help §7to learn more about abilities!");
            savePlayerData(); // Save immediately
        }

        return oreType;
    }

    public void setPlayerOre(Player player, OreType oreType) {
        UUID uuid = player.getUniqueId();
        OreType oldOreType = playerOres.get(uuid);

        playerOres.put(uuid, oreType);

        if (oreType.isStarter() && !starterOres.containsKey(uuid)) {
            starterOres.put(uuid, oreType);
        }

        // Auto-save after changes
        savePlayerData();

        // If changing from old ore type, clean up old effects
        if (oldOreType != null && oldOreType != oreType) {
            plugin.getAbilityListener().removeAllOreTypeEffectsFixed(player, oldOreType);
        }
    }

    // NEW: Get player's starter ore
    public OreType getPlayerStarterOre(Player player) {
        UUID uuid = player.getUniqueId();
        OreType starterOre = starterOres.get(uuid);

        // If no starter ore saved, their current ore if it's a starter, or assign random
        if (starterOre == null) {
            OreType currentOre = playerOres.get(uuid);
            if (currentOre != null && currentOre.isStarter()) {
                starterOres.put(uuid, currentOre);
                savePlayerData();
                return currentOre;
            } else {
                // Assign random starter and save
                starterOre = OreType.getRandomStarter();
                starterOres.put(uuid, starterOre);
                savePlayerData();
                return starterOre;
            }
        }

        return starterOre;
    }

    // NEW: Reset player to starter ore (used on death)
    public void resetToStarterOre(Player player) {
        UUID uuid = player.getUniqueId();
        OreType currentOre = playerOres.get(uuid);
        OreType starterOre = getPlayerStarterOre(player);

        if (currentOre != null && !currentOre.isStarter()) {
            // Clean up current ore effects
            plugin.getAbilityListener().removeAllOreTypeEffectsFixed(player, currentOre);

            // Set to starter ore
            playerOres.put(uuid, starterOre);
            savePlayerData();

            player.sendMessage("§cYou died! Reverted to your starter ore: " + starterOre.getDisplayName());
            // Apply starter ore effects
            plugin.getAbilityListener().applyAllOreTypeEffectsFixed(player, starterOre);
        }
    }

    // NEW: Check if current ore is crafted (not starter)
    public boolean hasCraftedOre(Player player) {
        OreType currentOre = playerOres.get(player.getUniqueId());
        return currentOre != null && !currentOre.isStarter();
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
    }

    public void clearCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.remove(uuid);
    }

    public void cleanup(Player player) {
        // Optional: Clean up cooldowns when player leaves
        // We'll keep them for now since they should persist across sessions
        UUID uuid = player.getUniqueId();
    }

    // Get all players with ore types (for debugging/admin purposes)
    public Map<UUID, OreType> getAllPlayerOres() {
        return new HashMap<>(playerOres);
    }

    // Remove a player's ore data completely (admin command)
    public void removePlayerData(UUID uuid) {
        playerOres.remove(uuid);
        starterOres.remove(uuid); // NEW: Also remove starter ore
        cooldowns.remove(uuid);
        savePlayerData();
    }

    // Check if player has an ore type assigned (without auto-assigning)
    public boolean hasPlayerOre(Player player) {
        return playerOres.containsKey(player.getUniqueId());
    }

    // Manually assign a random starter ore to a player
    public void assignRandomStarterOre(Player player) {
        OreType randomOre = OreType.getRandomStarter();
        UUID uuid = player.getUniqueId();

        playerOres.put(uuid, randomOre);
        starterOres.put(uuid, randomOre); // NEW: Also save as starter
        savePlayerData();

        player.sendMessage("§6Welcome! You've been assigned the " + randomOre.getDisplayName() + " ore type!");
        player.sendMessage("§7Use §e/ore help §7to learn more about abilities!");
        plugin.getLogger().info("Assigned random starter ore " + randomOre.getDisplayName() + " to " + player.getName());
    }

    // Get player ore without auto-assignment (returns null if none)
    public OreType getPlayerOreRaw(Player player) {
        return playerOres.get(player.getUniqueId());
    }
}