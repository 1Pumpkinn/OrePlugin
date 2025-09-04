package hs.orePlugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityActivationManager implements Listener {

    private final OreAbilitiesPlugin plugin;
    private final Map<UUID, Boolean> bedrockMode = new HashMap<>();
    private final Map<UUID, Long> lastActivation = new HashMap<>();

    // Cooldown to prevent accidental double activation
    private static final long ACTIVATION_COOLDOWN = 500; // 0.5 seconds

    public AbilityActivationManager(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
        loadBedrockData();
    }

    // Main activation method - Shift + Right-click (works on air and blocks)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Skip if player is in bedrock mode (they use commands)
        if (isBedrockMode(player)) {
            return;
        }

        // Check for Shift + Right-click on both air and blocks
        if (player.isSneaking() &&
                (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {

            // Try to activate ability
            boolean abilityActivated = tryActivateAbility(player, "Shift+Right-Click");

            // Only cancel block interaction events if we successfully activated an ability
            // This prevents interference with normal block interactions if ability fails
            // Always allow air interactions to pass through since they don't interfere
            if (abilityActivated && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
            }
        }
    }

    private boolean tryActivateAbility(Player player, String method) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check activation cooldown to prevent spam
        if (lastActivation.containsKey(uuid)) {
            long timeDiff = currentTime - lastActivation.get(uuid);
            if (timeDiff < ACTIVATION_COOLDOWN) {
                return false; // Too soon, ignore
            }
        }

        lastActivation.put(uuid, currentTime);

        // Try to use ability
        boolean success = plugin.getAbilityManager().useAbility(player);

        if (success) {
            // Optional: Show activation method in action bar briefly
            player.sendMessage("§8Ability activated via " + method);
        }

        return success;
    }

    public boolean isBedrockMode(Player player) {
        return bedrockMode.getOrDefault(player.getUniqueId(), false);
    }

    public void setBedrockMode(Player player, boolean bedrock) {
        bedrockMode.put(player.getUniqueId(), bedrock);
        saveBedrockData();

        if (bedrock) {
            player.sendMessage("§eBedrock mode enabled! Use §6/ability §eto activate abilities.");
            player.sendMessage("§7Shift + Right-click activation has been disabled.");
        } else {
            player.sendMessage("§aJava mode enabled! Use §6Shift + Right-click §eto activate abilities.");
            player.sendMessage("§7Works on both air and blocks! Won't interfere with normal interactions.");
        }
    }

    public void loadBedrockData() {
        FileConfiguration config = plugin.getPlayerDataConfig();
        if (config.getConfigurationSection("bedrock") != null) {
            for (String uuidString : config.getConfigurationSection("bedrock").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                boolean isBedrock = config.getBoolean("bedrock." + uuidString, false);
                bedrockMode.put(uuid, isBedrock);
            }
        }
    }

    public void saveBedrockData() {
        FileConfiguration config = plugin.getPlayerDataConfig();

        for (Map.Entry<UUID, Boolean> entry : bedrockMode.entrySet()) {
            String uuidString = entry.getKey().toString();
            config.set("bedrock." + uuidString, entry.getValue());
        }

        plugin.savePlayerData();
    }

    // Clean up data for offline players periodically
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        lastActivation.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > 300000); // Remove entries older than 5 minutes
    }
}