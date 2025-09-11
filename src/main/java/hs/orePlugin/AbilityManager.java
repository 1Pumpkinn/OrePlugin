package hs.orePlugin;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.ChatColor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class AbilityManager {

    private final OreAbilitiesPlugin plugin;
    private final Map<UUID, Boolean> activeEffects = new HashMap<>();
    private final Map<UUID, Boolean> copperLightningActive = new HashMap<>();
    private final Map<UUID, Integer> noJumpEffects = new HashMap<>();
    private final Map<UUID, BukkitTask> ironDropTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> amethystGlowTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> noJumpTasks = new HashMap<>();
    private final Random random = new Random();

    public AbilityManager(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean useAbility(Player player) {
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreConfigs configs = plugin.getOreConfigs();

        if (dataManager.isOnCooldown(player)) {
            long remaining = dataManager.getRemainingCooldown(player);
            String message = configs != null ? configs.getMessage("ability-on-cooldown", "time", String.valueOf(remaining))
                    : "§cAbility on cooldown! " + remaining + " seconds remaining.";
            message = ChatColor.translateAlternateColorCodes('&', message);
            player.sendMessage(message);
            return false;
        }

        OreType oreType = dataManager.getPlayerOre(player);
        if (oreType == null) {
            String message = configs != null ? configs.getMessage("no-ore-type")
                    : "§cYou don't have an ore type assigned!";
            message = ChatColor.translateAlternateColorCodes('&', message);
            player.sendMessage(message);
            return false;
        }

        if (oreType == OreType.EMERALD && !hasRequiredEmeralds(player)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 0, false, false));
        }

        if (configs != null && !configs.isOreEnabled(oreType)) {
            player.sendMessage("§cThis ore type is currently disabled!");
            return false;
        }

        executeAbility(player, oreType);

        int cooldown = configs != null ? configs.getCooldown(oreType) : oreType.getCooldown();
        dataManager.setCooldown(player, cooldown);

        plugin.getActionBarManager().updateCooldownDisplay(player);

        return true;
    }

    private boolean hasRequiredEmeralds(Player player) {
        int emeraldCount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.EMERALD) {
                emeraldCount += item.getAmount();
            }
        }
        return emeraldCount >= 256;
    }

    private void executeAbility(Player player, OreType oreType) {
        switch (oreType) {
            case DIRT:
                dirtAbility(player);
                break;
            case WOOD:
                woodAbility(player);
                break;
            case STONE:
                stoneAbility(player);
                break;
            case COAL:
                coalAbility(player);
                break;
            case COPPER:
                copperAbility(player);
                break;
            case IRON:
                ironAbility(player);
                break;
            case GOLD:
                goldAbility(player);
                break;
            case REDSTONE:
                redstoneAbility(player);
                break;
            case LAPIS:
                lapisAbility(player);
                break;
            case EMERALD:
                emeraldAbility(player);
                break;
            case AMETHYST:
                amethystAbility(player);
                break;
            case DIAMOND:
                diamondAbility(player);
                break;
            case NETHERITE:
                netheriteAbility(player);
                break;
        }
    }

    private void dirtAbility(Player player) {
        Location loc = player.getLocation();
        Material below = loc.subtract(0, 1, 0).getBlock().getType();
        loc.add(0, 1, 0);

        if (below == Material.GRASS_BLOCK || below == Material.DIRT) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300, 3));
            player.sendMessage("§aEarth's Blessing activated! +4 absorption hearts for 15 seconds!");
            player.playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0f, 1.0f);
        } else {
            player.sendMessage("§cYou must be standing on grass or dirt!");
        }
    }

    private void woodAbility(Player player) {
        activeEffects.put(player.getUniqueId(), true);
        player.sendMessage("§6Lumberjack's Fury activated! Axes deal 1.5x damage for 5 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                activeEffects.remove(player.getUniqueId());
                player.sendMessage("§eLumberjack's Fury has ended.");
            }
        }.runTaskLater(plugin, 100);
    }

    private void stoneAbility(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0));
        player.sendMessage("§7Stone Skin activated! Resistance 1 for 5 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
    }

    private void coalAbility(Player player) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType() == Material.AIR) {
            player.sendMessage("§cYou must hold an item to smelt!");
            return;
        }

        Material smeltResult = getSmeltResult(handItem.getType());
        if (smeltResult == null) {
            player.sendMessage("§cThis item cannot be smelted!");
            return;
        }

        int amount = handItem.getAmount();
        player.getInventory().setItemInMainHand(new ItemStack(smeltResult, amount));
        player.sendMessage("§6Sizzle! Smelted " + amount + " items!");
        player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1.0f, 1.0f);
    }

    private void copperAbility(Player player) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem != null && handItem.getType() == Material.TRIDENT) {
            ItemMeta meta = handItem.getItemMeta();
            if (meta != null && !meta.hasEnchant(Enchantment.CHANNELING)) {
                meta.addEnchant(Enchantment.CHANNELING, 1, true);
                handItem.setItemMeta(meta);
                player.getInventory().setItemInMainHand(handItem);
                player.sendMessage("§3Your trident has been enchanted with Channeling!");
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
            }
        }

        copperLightningActive.put(player.getUniqueId(), true);
        activeEffects.put(player.getUniqueId(), true);
        player.sendMessage("§3Channel The Clouds activated! Lightning strikes on hit for 10 seconds!");
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                copperLightningActive.remove(player.getUniqueId());
                activeEffects.remove(player.getUniqueId());
                player.sendMessage("§bChannel The Clouds has ended.");
            }
        }.runTaskLater(plugin, 200);
    }

    private void ironAbility(Player player) {
        Material[] buckets = {Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.BUCKET};
        Material chosen = buckets[random.nextInt(buckets.length)];

        ItemStack bucket = new ItemStack(chosen);
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(bucket);
            player.sendMessage("§fBucket Roulette! You received a " + chosen.name().toLowerCase().replace("_", " ") + "!");
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0f, 1.0f);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), bucket);
            player.sendMessage("§fBucket Roulette! Bucket dropped (inventory full)!");
        }
    }

    private void goldAbility(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 200, 4));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2));
        player.sendMessage("§eGoldrush activated! Haste 5 and Speed 3 for 10 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_METAL_BREAK, 1.0f, 1.0f);
    }

    private void redstoneAbility(Player player) {
        activeEffects.put(player.getUniqueId(), true);
        player.sendMessage("§4Sticky Slime activated! Next hit prevents jumping for 10 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                activeEffects.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, 1200);
    }

    private void lapisAbility(Player player) {
        activeEffects.put(player.getUniqueId(), true);
        player.sendMessage("§9Level Replenish activated! EXP gives regeneration for 30 seconds!");
        player.sendMessage("§7Note: Anvil enchanting costs no XP as a passive ability!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                activeEffects.remove(player.getUniqueId());
                player.sendMessage("§bLevel Replenish has ended.");
            }
        }.runTaskLater(plugin, 600);
    }

    private void emeraldAbility(Player player) {
        if (!hasRequiredEmeralds(player)) {
            player.sendMessage("§cYou need at least 4 stacks of emeralds to use this ability!");
            return;
        }

        PotionEffectType[] beaconEffects = {
                PotionEffectType.SPEED, PotionEffectType.HASTE, PotionEffectType.RESISTANCE,
                PotionEffectType.JUMP_BOOST, PotionEffectType.STRENGTH, PotionEffectType.REGENERATION
        };

        for (PotionEffectType effect : beaconEffects) {
            player.addPotionEffect(new PotionEffect(effect, 400, 0));
        }

        player.sendMessage("§aBring Home The Effects! All beacon effects for 20 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    private void amethystAbility(Player player) {
        activeEffects.put(player.getUniqueId(), true);
        player.sendMessage("§dCrystal Cluster activated! No knockback and no damage for 10 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                activeEffects.remove(player.getUniqueId());
                player.sendMessage("§5Crystal Cluster has ended.");
            }
        }.runTaskLater(plugin, 200);
    }

    private void diamondAbility(Player player) {
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon != null && weapon.getType() == Material.DIAMOND_SWORD) {
            activeEffects.put(player.getUniqueId(), true);
            player.sendMessage("§bGleaming Power activated! Diamond sword deals 1.4x damage for 5 seconds!");
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 2.0f);

            new BukkitRunnable() {
                @Override
                public void run() {
                    activeEffects.remove(player.getUniqueId());
                    player.sendMessage("§3Gleaming Power has ended.");
                }
            }.runTaskLater(plugin, 100);
        } else {
            player.sendMessage("§cYou must be holding a diamond sword!");
        }
    }

    private void netheriteAbility(Player player) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType() == Material.AIR) {
            player.sendMessage("§cYou must hold an item to upgrade!");
            return;
        }

        Material upgraded = getNetheriteUpgrade(handItem.getType());
        if (upgraded == null) {
            player.sendMessage("§cThis item cannot be upgraded to netherite!");
            return;
        }

        ItemStack newItem = handItem.clone();
        newItem.setType(upgraded);
        player.getInventory().setItemInMainHand(newItem);
        player.sendMessage("§4Debris, Debris, Debris! Item upgraded to netherite!");
        player.playSound(player.getLocation(), Sound.UI_STONECUTTER_TAKE_RESULT, 1.0f, 0.5f);
    }

    public void addNoJumpEffect(UUID playerUUID, int durationTicks) {
        noJumpEffects.put(playerUUID, durationTicks);

        BukkitTask task = new BukkitRunnable() {
            int remaining = durationTicks;

            @Override
            public void run() {
                remaining--;
                if (remaining <= 0) {
                    noJumpEffects.remove(playerUUID);
                    cancel();
                    noJumpTasks.remove(playerUUID);
                } else {
                    noJumpEffects.put(playerUUID, remaining);
                }
            }
        }.runTaskTimer(plugin, 1, 1);

        noJumpTasks.put(playerUUID, task);
    }

    public boolean hasNoJumpEffect(UUID playerUUID) {
        return noJumpEffects.containsKey(playerUUID);
    }

    public boolean resetCooldown(Player player) {
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        if (dataManager.isOnCooldown(player)) {
            dataManager.clearCooldown(player);
            plugin.getActionBarManager().updateCooldownDisplay(player);
            return true;
        }
        return false;
    }

    public void startIronDropTimer(Player player) {
        UUID uuid = player.getUniqueId();

        if (ironDropTasks.containsKey(uuid)) {
            ironDropTasks.get(uuid).cancel();
            ironDropTasks.remove(uuid);
        }

        OreConfigs configs = plugin.getOreConfigs();
        int intervalMinutes = configs != null ? configs.getIronDropInterval() : 10;
        long intervalTicks = intervalMinutes * 60 * 20L;

        plugin.getLogger().info("Starting Iron drop timer for " + player.getName() + " - interval: " + intervalMinutes + " minutes (" + intervalTicks + " ticks)");

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    plugin.getLogger().info("Iron timer cancelled for " + player.getName() + " - player offline");
                    cancel();
                    ironDropTasks.remove(uuid);
                    return;
                }

                if (plugin.getPlayerDataManager().getPlayerOre(player) != OreType.IRON) {
                    plugin.getLogger().info("Iron timer cancelled for " + player.getName() + " - no longer iron ore type");
                    cancel();
                    ironDropTasks.remove(uuid);
                    return;
                }

                plugin.getLogger().info("Iron timer triggered for " + player.getName() + " - dropping random item");
                dropRandomItem(player);
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);

        ironDropTasks.put(uuid, task);
    }

    public void cancelIronDropTimer(Player player) {
        UUID uuid = player.getUniqueId();
        if (ironDropTasks.containsKey(uuid)) {
            ironDropTasks.get(uuid).cancel();
            ironDropTasks.remove(uuid);
            plugin.getLogger().info("Iron timer cancelled for " + player.getName());
        }
    }

    private void dropRandomItem(Player player) {
        ItemStack[] inventory = player.getInventory().getContents();
        java.util.List<Integer> validSlots = new java.util.ArrayList<>();

        for (int i = 0; i < 36; i++) {
            if (inventory[i] != null && inventory[i].getType() != Material.AIR) {
                validSlots.add(i);
            }
        }

        if (validSlots.isEmpty()) {
            player.sendMessage("§c⚠ Iron curse tried to drop an item, but your inventory is empty!");
            plugin.getLogger().info("Iron drop failed for " + player.getName() + " - no items in inventory");
            return;
        }

        int randomSlot = validSlots.get(random.nextInt(validSlots.size()));
        ItemStack itemStack = inventory[randomSlot];

        ItemStack itemToDrop = itemStack.clone();
        itemToDrop.setAmount(1);

        player.getWorld().dropItemNaturally(player.getLocation(), itemToDrop);

        if (itemStack.getAmount() > 1) {
            itemStack.setAmount(itemStack.getAmount() - 1);
        } else {
            player.getInventory().setItem(randomSlot, null);
        }

        player.sendMessage("§c⚠ Iron curse! A " + itemToDrop.getType().name().toLowerCase().replace("_", " ") + " dropped from your inventory!");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.5f);

        plugin.getLogger().info("Iron drop successful for " + player.getName() + " - dropped " + itemToDrop.getType().name());
    }

    public void startAmethystGlowing(Player player) {
        UUID uuid = player.getUniqueId();

        setupAmethystTeam(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));

        if (amethystGlowTasks.containsKey(uuid)) {
            amethystGlowTasks.get(uuid).cancel();
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    amethystGlowTasks.remove(uuid);
                    return;
                }

                if (plugin.getPlayerDataManager().getPlayerOre(player) != OreType.AMETHYST) {
                    cancel();
                    amethystGlowTasks.remove(uuid);
                    return;
                }

                if (!player.hasPotionEffect(PotionEffectType.GLOWING)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
                }

                setupAmethystTeam(player);
            }
        }.runTaskTimer(plugin, 600L, 600L);

        amethystGlowTasks.put(uuid, task);
    }

    private void setupAmethystTeam(Player player) {
        Scoreboard scoreboard = player.getServer().getScoreboardManager().getMainScoreboard();
        Team amethystTeam = scoreboard.getTeam("amethyst");

        if (amethystTeam == null) {
            amethystTeam = scoreboard.registerNewTeam("amethyst");
            amethystTeam.setColor(ChatColor.DARK_PURPLE);
            amethystTeam.setDisplayName("§5Amethyst");
        }

        if (!amethystTeam.hasEntry(player.getName())) {
            amethystTeam.addEntry(player.getName());
        }
    }

    public void cancelAmethystGlowing(Player player) {
        UUID uuid = player.getUniqueId();
        if (amethystGlowTasks.containsKey(uuid)) {
            amethystGlowTasks.get(uuid).cancel();
            amethystGlowTasks.remove(uuid);
        }
        player.removePotionEffect(PotionEffectType.GLOWING);

        Scoreboard scoreboard = player.getServer().getScoreboardManager().getMainScoreboard();
        Team amethystTeam = scoreboard.getTeam("amethyst");
        if (amethystTeam != null && amethystTeam.hasEntry(player.getName())) {
            amethystTeam.removeEntry(player.getName());
        }
    }

    public boolean hasActiveEffect(Player player) {
        return activeEffects.getOrDefault(player.getUniqueId(), false);
    }

    public boolean hasCopperLightningActive(Player player) {
        return copperLightningActive.getOrDefault(player.getUniqueId(), false);
    }

    public void removeActiveEffect(Player player) {
        activeEffects.remove(player.getUniqueId());
    }

    private Material getSmeltResult(Material input) {
        switch (input) {
            case IRON_ORE: case DEEPSLATE_IRON_ORE: return Material.IRON_INGOT;
            case GOLD_ORE: case DEEPSLATE_GOLD_ORE: return Material.GOLD_INGOT;
            case COPPER_ORE: case DEEPSLATE_COPPER_ORE: return Material.COPPER_INGOT;
            case COBBLESTONE: return Material.STONE;
            case SAND: return Material.GLASS;
            case RAW_IRON: return Material.IRON_INGOT;
            case RAW_GOLD: return Material.GOLD_INGOT;
            case RAW_COPPER: return Material.COPPER_INGOT;
            case CLAY_BALL: return Material.BRICK;
            case WET_SPONGE: return Material.SPONGE;
            case CACTUS: return Material.GREEN_DYE;
            case KELP: return Material.DRIED_KELP;
            case ANCIENT_DEBRIS: return Material.NETHERITE_INGOT;
            default: return null;
        }
    }

    private Material getNetheriteUpgrade(Material input) {
        switch (input) {
            case DIAMOND_SWORD: return Material.NETHERITE_SWORD;
            case DIAMOND_PICKAXE: return Material.NETHERITE_PICKAXE;
            case DIAMOND_AXE: return Material.NETHERITE_AXE;
            case DIAMOND_SHOVEL: return Material.NETHERITE_SHOVEL;
            case DIAMOND_HOE: return Material.NETHERITE_HOE;
            case DIAMOND_HELMET: return Material.NETHERITE_HELMET;
            case DIAMOND_CHESTPLATE: return Material.NETHERITE_CHESTPLATE;
            case DIAMOND_LEGGINGS: return Material.NETHERITE_LEGGINGS;
            case DIAMOND_BOOTS: return Material.NETHERITE_BOOTS;
            default: return null;
        }
    }

    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        activeEffects.remove(uuid);
        copperLightningActive.remove(uuid);
        noJumpEffects.remove(uuid);

        if (ironDropTasks.containsKey(uuid)) {
            ironDropTasks.get(uuid).cancel();
            ironDropTasks.remove(uuid);
        }

        if (amethystGlowTasks.containsKey(uuid)) {
            amethystGlowTasks.get(uuid).cancel();
            amethystGlowTasks.remove(uuid);
        }

        if (noJumpTasks.containsKey(uuid)) {
            noJumpTasks.get(uuid).cancel();
            noJumpTasks.remove(uuid);
        }
    }

    public void restartPlayerTimers(Player player) {
        OreType oreType = plugin.getPlayerDataManager().getPlayerOre(player);
        if (oreType == null) return;

        switch (oreType) {
            case IRON:
                startIronDropTimer(player);
                break;
            case AMETHYST:
                startAmethystGlowing(player);
                break;
        }
    }
}