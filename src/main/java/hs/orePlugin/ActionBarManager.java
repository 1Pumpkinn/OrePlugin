package hs.orePlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionBarManager {

    private final OreAbilitiesPlugin plugin;
    private final Map<UUID, BukkitTask> actionBarTasks = new HashMap<>();

    public ActionBarManager(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    public void startActionBar(Player player) {
        stopActionBar(player); // Stop existing task if any

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    actionBarTasks.remove(player.getUniqueId());
                    return;
                }

                updateActionBar(player);
            }
        }.runTaskTimer(plugin, 0L, 20L); // Update every second

        actionBarTasks.put(player.getUniqueId(), task);
    }

    public void stopActionBar(Player player) {
        BukkitTask task = actionBarTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public void stopAllActionBars() {
        for (BukkitTask task : actionBarTasks.values()) {
            task.cancel();
        }
        actionBarTasks.clear();
    }

    private void updateActionBar(Player player) {
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == null) {
            sendActionBar(player, "§cNo Ore Type Assigned");
            return;
        }

        String message = buildActionBarMessage(player, oreType, dataManager);
        sendActionBar(player, message);
    }

    private String buildActionBarMessage(Player player, OreType oreType, PlayerDataManager dataManager) {
        StringBuilder message = new StringBuilder();

        // Ore type with color coding
        String oreColor = getOreColor(oreType);
        message.append(oreColor).append("⚡ ").append(oreType.getDisplayName()).append(" Ore");

        // Ability status
        if (dataManager.isOnCooldown(player)) {
            long remaining = dataManager.getRemainingCooldown(player);
            message.append(" §8| §c❌ Cooldown: §f").append(remaining).append("s");

            // Add progress bar
            String progressBar = createProgressBar(remaining, oreType.getCooldown());
            message.append(" §8[").append(progressBar).append("§8]");
        } else {
            message.append(" §8| §a✓ Ready! §7(Right-Click)");
        }

        // Add ability name
        String abilityName = getAbilityName(oreType);
        if (abilityName != null) {
            message.append(" §8| §6").append(abilityName);
        }

        return message.toString();
    }

    private String createProgressBar(long remaining, int totalCooldown) {
        int totalBars = 10;
        double progress = (double) (totalCooldown - remaining) / totalCooldown;
        int filledBars = (int) Math.round(progress * totalBars);

        StringBuilder bar = new StringBuilder();

        // Filled portion (green)
        for (int i = 0; i < filledBars; i++) {
            bar.append("§a█");
        }

        // Empty portion (red)
        for (int i = filledBars; i < totalBars; i++) {
            bar.append("§c█");
        }

        return bar.toString();
    }

    private String getOreColor(OreType oreType) {
        switch (oreType) {
            case DIRT: return "§6"; // Gold
            case WOOD: return "§e"; // Yellow
            case STONE: return "§7"; // Gray
            case COAL: return "§8"; // Dark Gray
            case COPPER: return "§c"; // Red
            case IRON: return "§f"; // White
            case GOLD: return "§e"; // Yellow
            case REDSTONE: return "§4"; // Dark Red
            case LAPIS: return "§9"; // Blue
            case EMERALD: return "§a"; // Green
            case AMETHYST: return "§d"; // Light Purple
            case DIAMOND: return "§b"; // Aqua
            case NETHERITE: return "§8"; // Dark Gray
            default: return "§7"; // Default Gray
        }
    }

    private String getAbilityName(OreType oreType) {
        switch (oreType) {
            case DIRT: return "Earth's Blessing";
            case WOOD: return "Lumberjack's Fury";
            case STONE: return "Stone Skin";
            case COAL: return "Sizzle";
            case COPPER: return "Channel The Clouds";
            case IRON: return "Bucket Roulette";
            case GOLD: return "Goldrush";
            case REDSTONE: return "Sticky Slime";
            case LAPIS: return "Level Replenish";
            case EMERALD: return "Bring Home The Effects";
            case AMETHYST: return "Crystal Cluster";
            case DIAMOND: return "Gleaming Power";
            case NETHERITE: return "Debris, Debris, Debris";
            default: return null;
        }
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    public void updateCooldownDisplay(Player player) {
        // Force immediate update when ability is used
        if (actionBarTasks.containsKey(player.getUniqueId())) {
            updateActionBar(player);
        }
    }
}