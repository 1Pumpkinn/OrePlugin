package hs.orePlugin;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashMap;
import java.util.Map;

public class OreConfigs {

    private final OreAbilitiesPlugin plugin;
    private final FileConfiguration config;
    private final Map<String, String> messageCache = new HashMap<>();

    public OreConfigs(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadMessages();
    }

    // Get shatter chance from config
    public double getShatterChance() {
        return config.getDouble("ores.shatter-chance", 0.25);
    }

    // Check if ore type is enabled
    public boolean isOreEnabled(OreType oreType) {
        String path = "ores.enabled-ores." + oreType.name().toLowerCase();
        return config.getBoolean(path, true);
    }

    // Get cooldown for ore type (uses config override or default)
    public int getCooldown(OreType oreType) {
        String path = "cooldowns." + oreType.name().toLowerCase();
        return config.getInt(path, oreType.getCooldown());
    }

    // Trust system settings
    public int getMaxTrustedPlayers() {
        return config.getInt("trust.max-trusted-players", 10);
    }

    public int getTrustRequestTimeout() {
        return config.getInt("trust.request-timeout", 60);
    }

    public boolean isTrustNotificationsEnabled() {
        return config.getBoolean("trust.enable-notifications", true);
    }

    // Effects settings
    public int getIronDropInterval() {
        return config.getInt("effects.iron-drop-interval", 10);
    }

    public boolean areParticlesEnabled() {
        return config.getBoolean("effects.enable-particles", true);
    }

    public boolean areSoundsEnabled() {
        return config.getBoolean("effects.enable-sounds", true);
    }

    // Action bar settings
    public boolean isActionBarEnabled() {
        return config.getBoolean("effects.action-bar.enabled", true);
    }

    public int getActionBarUpdateInterval() {
        return config.getInt("effects.action-bar.update-interval", 20);
    }

    public boolean showProgressBar() {
        return config.getBoolean("effects.action-bar.show-progress-bar", true);
    }

    // Copper settings
    public double getCopperArmorDurabilityMultiplier() {
        return config.getDouble("copper.armor-durability-multiplier", 2.0);
    }

    public int getCopperArmorDamageInterval() {
        return config.getInt("copper.armor-damage-interval", 100);
    }

    // Coal settings
    public double getCoalWaterDamage() {
        return config.getDouble("coal.water-damage", 2.0);
    }

    public int getCoalWaterDamageInterval() {
        return config.getInt("coal.water-damage-interval", 2000);
    }

    // Message system
    private void loadMessages() {
        if (config.getConfigurationSection("messages") != null) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                String message = config.getString("messages." + key, "");
                messageCache.put(key, message);
            }
        }
    }

    public String getMessage(String key) {
        return messageCache.getOrDefault(key, "§cMessage not found: " + key);
    }

    public String getMessage(String key, String placeholder, String value) {
        String message = getMessage(key);
        return message.replace("{" + placeholder + "}", value);
    }

    // Debug mode
    public boolean isDebugEnabled() {
        return config.getBoolean("plugin.debug", false);
    }

    // Auto-save interval
    public int getAutoSaveInterval() {
        return config.getInt("plugin.auto-save-interval", 5);
    }

    // Reload configuration
    public void reload() {
        plugin.reloadConfig();
        messageCache.clear();
        loadMessages();
    }
}