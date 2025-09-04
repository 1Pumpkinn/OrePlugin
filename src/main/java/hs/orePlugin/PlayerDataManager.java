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
    private final Map<UUID, Long> ironDropTimer = new HashMap<>();

    public PlayerDataManager(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
        loadPlayerData();
    }

    public void loadPlayerData() {
        FileConfiguration config = plugin.getPlayerDataConfig();
        if (config.getConfigurationSection("players") != null) {
            for (String uuidString : config.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                String oreTypeName = config.getString("players." + uuidString + ".ore");
                if (oreTypeName != null) {
                    try {
                        OreType oreType = OreType.valueOf(oreTypeName.toUpperCase());
                        playerOres.put(uuid, oreType);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid ore type for player " + uuid + ": " + oreTypeName);
                    }
                }

                long ironTimer = config.getLong("players." + uuidString + ".ironTimer", 0);
                if (ironTimer > 0) {
                    ironDropTimer.put(uuid, ironTimer);
                }
            }
        }
    }

    public void savePlayerData() {
        FileConfiguration config = plugin.getPlayerDataConfig();
        for (Map.Entry<UUID, OreType> entry : playerOres.entrySet()) {
            String uuidString = entry.getKey().toString();
            config.set("players." + uuidString + ".ore", entry.getValue().name());

            if (ironDropTimer.containsKey(entry.getKey())) {
                config.set("players." + uuidString + ".ironTimer", ironDropTimer.get(entry.getKey()));
            }
        }
        plugin.savePlayerData();
    }

    public OreType getPlayerOre(Player player) {
        return playerOres.get(player.getUniqueId());
    }

    public void setPlayerOre(Player player, OreType oreType) {
        playerOres.put(player.getUniqueId(), oreType);
        savePlayerData();
    }

    public boolean hasPlayerOre(Player player) {
        return playerOres.containsKey(player.getUniqueId());
    }

    public void assignRandomStarterOre(Player player) {
        if (!hasPlayerOre(player)) {
            OreType starterOre = OreType.getRandomStarter();
            setPlayerOre(player, starterOre);
        }
    }

    public boolean isOnCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownEnd = cooldowns.get(uuid);
        return currentTime < cooldownEnd;
    }

    public void setCooldown(Player player, int seconds) {
        long currentTime = System.currentTimeMillis();
        cooldowns.put(player.getUniqueId(), currentTime + (seconds * 1000L));
    }

    public long getRemainingCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownEnd = cooldowns.get(uuid);
        return Math.max(0, (cooldownEnd - currentTime) / 1000);
    }

    public void setIronDropTimer(Player player) {
        ironDropTimer.put(player.getUniqueId(), System.currentTimeMillis() + 600000); // 10 minutes
    }

    public boolean shouldDropIronItem(Player player) {
        UUID uuid = player.getUniqueId();
        if (!ironDropTimer.containsKey(uuid)) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime >= ironDropTimer.get(uuid)) {
            setIronDropTimer(player); // Reset timer
            return true;
        }
        return false;
    }
}