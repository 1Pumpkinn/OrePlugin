package hs.orePlugin;

public enum OreType {
    // Starter ores
    DIRT("Dirt", true, 30),
    WOOD("Wood", true, 30),
    STONE("Stone", true, 30),

    // Craftable ores
    COAL("Coal", false, 10),
    COPPER("Copper", false, 45),
    IRON("Iron", false, 45),
    GOLD("Gold", false, 75),
    REDSTONE("Redstone", false, 80),
    LAPIS("Lapis", false, 240),
    EMERALD("Emerald", false, 270),
    AMETHYST("Amethyst", false, 120),
    DIAMOND("Diamond", false, 360),
    NETHERITE("Netherite", false, 600);

    private final String displayName;
    private final boolean isStarter;
    private final int cooldown; // in seconds

    OreType(String displayName, boolean isStarter, int cooldown) {
        this.displayName = displayName;
        this.isStarter = isStarter;
        this.cooldown = cooldown;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isStarter() {
        return isStarter;
    }

    public int getCooldown() {
        return cooldown;
    }

    public static OreType[] getStarterOres() {
        return new OreType[]{DIRT, WOOD, STONE};
    }

    public static OreType getRandomStarter() {
        OreType[] starters = getStarterOres();
        return starters[(int) (Math.random() * starters.length)];
    }
}