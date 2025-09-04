package hs.orePlugin;

import org.bukkit.Material;
import org.bukkit.ChatColor;

public enum OreType {
    // Starter Ores
    DIRT(Material.DIRT, "Dirt", ChatColor.DARK_GREEN, 30, true),
    WOOD(Material.OAK_LOG, "Wood", ChatColor.GOLD, 30, true),
    STONE(Material.STONE, "Stone", ChatColor.GRAY, 30, true),

    // Craftable Ores
    COAL(Material.COAL, "Coal", ChatColor.BLACK, 10, false),
    COPPER(Material.COPPER_INGOT, "Copper", ChatColor.YELLOW, 45, false),
    IRON(Material.IRON_INGOT, "Iron", ChatColor.WHITE, 45, false),
    GOLD(Material.GOLD_INGOT, "Gold", ChatColor.GOLD, 75, false),
    REDSTONE(Material.REDSTONE, "Redstone", ChatColor.RED, 80, false),
    LAPIS(Material.LAPIS_LAZULI, "Lapis", ChatColor.BLUE, 240, false),
    EMERALD(Material.EMERALD, "Emerald", ChatColor.GREEN, 270, false),
    AMETHYST(Material.AMETHYST_SHARD, "Amethyst", ChatColor.LIGHT_PURPLE, 120, false),
    DIAMOND(Material.DIAMOND, "Diamond", ChatColor.AQUA, 360, false),
    NETHERITE(Material.NETHERITE_INGOT, "Netherite", ChatColor.DARK_PURPLE, 600, false);

    private final Material material;
    private final String displayName;
    private final ChatColor color;
    private final int cooldown;
    private final boolean isStarter;

    OreType(Material material, String displayName, ChatColor color, int cooldown, boolean isStarter) {
        this.material = material;
        this.displayName = displayName;
        this.color = color;
        this.cooldown = cooldown;
        this.isStarter = isStarter;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    public int getCooldown() {
        return cooldown;
    }

    public boolean isStarter() {
        return isStarter;
    }

    public String getAbilityName() {
        switch (this) {
            case DIRT: return "Earthen Boost";
            case WOOD: return "Lumberjack's Fury";
            case STONE: return "Stone Shield";
            case COAL: return "Sizzle";
            case COPPER: return "Channel The Clouds";
            case IRON: return "Bucket Roulette";
            case GOLD: return "Goldrush";
            case REDSTONE: return "Sticky Slime";
            case LAPIS: return "Level Replenish";
            case EMERALD: return "Bring Home The Effects";
            case AMETHYST: return "Crystal Cluster";
            case DIAMOND: return "Gleaming Power";
            case NETHERITE: return "Debris, Debris Debris";
            default: return "Unknown";
        }
    }

    public static OreType[] getStarterOres() {
        return new OreType[]{DIRT, WOOD, STONE};
    }

    public static OreType[] getCraftableOres() {
        return new OreType[]{COAL, COPPER, IRON, GOLD, REDSTONE, LAPIS, EMERALD, AMETHYST, DIAMOND, NETHERITE};
    }
}