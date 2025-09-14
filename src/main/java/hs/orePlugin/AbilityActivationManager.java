package hs.orePlugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityActivationManager implements Listener {

    private final OreAbilitiesPlugin plugin;
    private final Map<UUID, Boolean> bedrockMode = new HashMap<>();
    private final Map<UUID, Long> lastActivation = new HashMap<>();
    private final Map<UUID, Long> lastCooldownMessage = new HashMap<>();

    // Cooldown to prevent accidental double activation
    private static final long ACTIVATION_COOLDOWN = 500; // 0.5 seconds
    // Cooldown message spam protection
    private static final long COOLDOWN_MESSAGE_DELAY = 3000; // 3 seconds between messages

    public AbilityActivationManager(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
        loadBedrockData();
    }

    // UPDATED: Added mining check and cooldown spam protection
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Skip if player is in bedrock mode (they use commands)
        if (isBedrockMode(player)) {
            return;
        }

        // UPDATED: Check for Shift + Left-click on both air and blocks
        if (player.isSneaking() &&
                (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {

            // UPDATED: Prevent activation if player is mining a block
            if (event.getAction() == Action.LEFT_CLICK_BLOCK && isMiningBlock(event.getClickedBlock())) {
                return; // Don't activate ability while mining
            }

            // Try to activate ability
            boolean abilityActivated = tryActivateAbility(player, "Shift+Left-Click");

            // Only cancel block interaction events if we successfully activated an ability
            // This prevents interference with normal block interactions if ability fails
            // Always allow air interactions to pass through since they don't interfere
            if (abilityActivated && event.getAction() == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
            }
        }
    }

    // UPDATED: Added cooldown message spam protection
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

    // UPDATED: Check if the block being clicked is mineable (to prevent activation while mining)
    private boolean isMiningBlock(Block block) {
        if (block == null) return false;

        Material blockType = block.getType();

        // Check if it's a mineable block (not air, not interactive blocks)
        return blockType.isSolid() &&
                blockType != Material.AIR &&
                !isInteractiveBlock(blockType) &&
                blockType.getHardness() > 0; // Has mining hardness
    }

    // UPDATED: Helper method to identify interactive blocks that shouldn't prevent ability activation
    private boolean isInteractiveBlock(Material material) {
        switch (material) {
            // Doors and gates
            case OAK_DOOR:
            case BIRCH_DOOR:
            case SPRUCE_DOOR:
            case JUNGLE_DOOR:
            case ACACIA_DOOR:
            case DARK_OAK_DOOR:
            case CRIMSON_DOOR:
            case WARPED_DOOR:
            case IRON_DOOR:
            case OAK_FENCE_GATE:
            case BIRCH_FENCE_GATE:
            case SPRUCE_FENCE_GATE:
            case JUNGLE_FENCE_GATE:
            case ACACIA_FENCE_GATE:
            case DARK_OAK_FENCE_GATE:
            case CRIMSON_FENCE_GATE:
            case WARPED_FENCE_GATE:

                // Buttons and levers
            case STONE_BUTTON:
            case OAK_BUTTON:
            case BIRCH_BUTTON:
            case SPRUCE_BUTTON:
            case JUNGLE_BUTTON:
            case ACACIA_BUTTON:
            case DARK_OAK_BUTTON:
            case CRIMSON_BUTTON:
            case WARPED_BUTTON:
            case POLISHED_BLACKSTONE_BUTTON:
            case LEVER:

                // Containers and functional blocks
            case CHEST:
            case TRAPPED_CHEST:
            case ENDER_CHEST:
            case BARREL:
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
            case CRAFTING_TABLE:
            case ENCHANTING_TABLE:
            case ANVIL:
            case CHIPPED_ANVIL:
            case DAMAGED_ANVIL:
            case BREWING_STAND:
            case CAULDRON:
            case WATER_CAULDRON:
            case LAVA_CAULDRON:
            case POWDER_SNOW_CAULDRON:

                // Redstone components
            case REDSTONE_WIRE:
            case REPEATER:
            case COMPARATOR:
            case DAYLIGHT_DETECTOR:
            case TRIPWIRE_HOOK:
                return true;

            default:
                return false;
        }
    }

    public boolean isBedrockMode(Player player) {
        return bedrockMode.getOrDefault(player.getUniqueId(), false);
    }

    public void setBedrockMode(Player player, boolean bedrock) {
        bedrockMode.put(player.getUniqueId(), bedrock);
        saveBedrockData();

        if (bedrock) {
            player.sendMessage("§eBedrock mode enabled! Use §6/ability §eto activate abilities.");
            player.sendMessage("§7Shift + Left-click activation has been disabled.");
        } else {
            player.sendMessage("§aJava mode enabled! Use §6Shift + Left-click §eto activate abilities.");
            player.sendMessage("§7Works on both air and blocks! Won't interfere with normal interactions or mining.");
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
        lastCooldownMessage.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > 300000); // Remove entries older than 5 minutes
    }
}