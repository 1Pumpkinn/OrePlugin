package hs.orePlugin;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Bee;
import org.bukkit.entity.FallingBlock;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.Location;
import org.bukkit.block.Block;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class AbilityListener implements Listener {

    private final OreAbilitiesPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> lastWaterDamageTime = new HashMap<>();
    private final Map<UUID, BukkitRunnable> copperArmorTasks = new HashMap<>();

    public AbilityListener(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        TrustManager trustManager = plugin.getTrustManager();
        AbilityManager abilityManager = plugin.getAbilityManager();
        OreType oreType = dataManager.getPlayerOre(attacker);

        if (oreType == null) return;

        if (event.getEntity() instanceof Player) {
            Player target = (Player) event.getEntity();
            if (trustManager.isTrusted(attacker, target)) {
                event.setCancelled(true);
                return;
            }
        }

        switch (oreType) {
            case WOOD:
                if (abilityManager.hasActiveEffect(attacker)) {
                    ItemStack weapon = attacker.getInventory().getItemInMainHand();
                    if (weapon != null && weapon.getType().name().contains("AXE")) {
                        double newDamage = event.getDamage() * 1.5;
                        event.setDamage(newDamage);
                        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                    }
                }
                break;

            case COPPER:
                if (abilityManager.hasActiveEffect(attacker) && event.getEntity() instanceof LivingEntity) {
                    Location strikeLocation = event.getEntity().getLocation();
                    event.getEntity().getWorld().strikeLightning(strikeLocation);

                    // FIXED: Better lightning protection for copper user
                    if (attacker.getLocation().distance(strikeLocation) < 10) {
                        // Cancel any lightning damage to the copper user
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                attacker.setFireTicks(0); // Remove fire
                                attacker.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0));
                            }
                        }.runTaskLater(plugin, 1);
                    }
                }
                break;

            case DIAMOND:
                if (abilityManager.hasActiveEffect(attacker)) {
                    ItemStack weapon = attacker.getInventory().getItemInMainHand();
                    if (weapon != null && weapon.getType() == Material.DIAMOND_SWORD) {
                        event.setDamage(event.getDamage() * 2);
                        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 2.0f);
                    }
                }
                break;

            case REDSTONE:
                if (abilityManager.hasActiveEffect(attacker) && event.getEntity() instanceof Player) {
                    Player target = (Player) event.getEntity();
                    // FIXED: Better jump prevention - use higher negative amplifier
                    target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 200, -128, false, false));
                    target.sendMessage("§4You cannot jump for 10 seconds!");
                    attacker.sendMessage("§cSticky Slime effect applied to " + target.getName() + "!");

                    // Remove the active effect after use
                    plugin.getAbilityManager().removeActiveEffect(attacker);
                }
                break;

            case AMETHYST:
                ItemStack offhand = attacker.getInventory().getItemInOffHand();
                if (offhand != null && offhand.getType() == Material.AMETHYST_SHARD) {
                    event.setDamage(event.getDamage() + 1.5);
                }
                break;

            case COAL:
                if (attacker.getFireTicks() > 0) {
                    event.setDamage(event.getDamage() + 1);
                    attacker.playSound(attacker.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.0f);
                }
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        AbilityManager abilityManager = plugin.getAbilityManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == null) return;

        switch (oreType) {
            case COAL:
                // FIXED: Water damage only when player is actually in water
                if (event.getCause() == EntityDamageEvent.DamageCause.CUSTOM && player.isInWater()) {
                    // This will be handled in PlayerMoveEvent
                    return;
                }
                break;

            case REDSTONE:
                if (event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
                    if (damageEvent.getDamager() instanceof Bee || damageEvent.getDamager() instanceof Slime) {
                        event.setDamage(event.getDamage() * 5);
                        player.sendMessage("§cRedstone weakness to " + damageEvent.getDamager().getType().name().toLowerCase() + "!");
                    }
                }
                // FIXED: Better dripstone/stalactite damage prevention
                if (isDripstoneOrStalactiteDamage(event)) {
                    event.setCancelled(true);
                    player.sendMessage("§4Redstone protection from dripstone/stalactite!");
                }
                break;

            case AMETHYST:
                if (abilityManager.hasActiveEffect(player)) {
                    event.setCancelled(true);
                    player.setVelocity(player.getVelocity().multiply(0));
                }
                break;

            case COPPER:
                // FIXED: Prevent lightning damage to copper users
                if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
                    event.setCancelled(true);
                    player.sendMessage("§3Copper protection from lightning!");
                }
                break;
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == OreType.COAL) {
            handleCoalWaterDamage(player);
        }
    }

    // FIXED: Proper coal water damage handling
    private void handleCoalWaterDamage(Player player) {
        if (player.isInWater()) {
            UUID uuid = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            OreConfigs configs = plugin.getOreConfigs();

            int damageInterval = configs != null ? configs.getCoalWaterDamageInterval() : 2000;

            if (!lastWaterDamageTime.containsKey(uuid) ||
                    currentTime - lastWaterDamageTime.get(uuid) > damageInterval) {

                double damage = configs != null ? configs.getCoalWaterDamage() : 2.0;
                player.damage(damage);
                player.sendMessage("§8Coal curse! Water burns you!");
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.5f);
                lastWaterDamageTime.put(uuid, currentTime);
            }
        } else {
            // Reset timer when not in water
            lastWaterDamageTime.remove(player.getUniqueId());
        }
    }

    // FIXED: Better method to detect dripstone damage
    private boolean isDripstoneOrStalactiteDamage(EntityDamageEvent event) {
        // Check for falling block damage (dripstone)
        if (event.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent fbEvent = (EntityDamageByEntityEvent) event;
                if (fbEvent.getDamager() instanceof FallingBlock) {
                    FallingBlock fb = (FallingBlock) fbEvent.getDamager();
                    return fb.getBlockData().getMaterial() == Material.POINTED_DRIPSTONE;
                }
            }
            return true; // Assume all falling block damage for redstone protection
        }

        // Check for contact damage with dripstone
        if (event.getCause() == EntityDamageEvent.DamageCause.CONTACT) {
            Player player = (Player) event.getEntity();
            Location loc = player.getLocation();
            // Check surrounding blocks for dripstone
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 2; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block block = loc.clone().add(x, y, z).getBlock();
                        if (block.getType() == Material.POINTED_DRIPSTONE) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreConfigs configs = plugin.getOreConfigs();
        OreType currentOreType = dataManager.getPlayerOre(player);

        ItemStack result = event.getRecipe().getResult();

        // Check if crafting a direct ore mastery item
        if (RecipeManager.isDirectOreItem(result)) {
            OreType newOreType = RecipeManager.getOreTypeFromDirectItem(result);

            if (newOreType == null) {
                event.setCancelled(true);
                player.sendMessage("§cError: Invalid ore type!");
                return;
            }

            // Check if already have this ore type
            if (currentOreType == newOreType) {
                event.setCancelled(true);
                player.sendMessage("§cYou already have the " + newOreType.getDisplayName() + " ore ability!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            // Use config-based shatter chance
            double shatterChance = configs != null ? configs.getShatterChance() : 0.25;
            if (random.nextDouble() < shatterChance) {
                event.setCancelled(true);
                player.sendMessage("§cThe " + newOreType.getDisplayName() + " ore mastery shattered during crafting!");
                player.sendMessage("§7Try again - you might get lucky next time!");
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                return;
            }

            // Cancel the event to prevent getting the item
            event.setCancelled(true);

            // Remove old ore effects if switching
            if (currentOreType != null) {
                removeOreTypeEffects(player, currentOreType);
                player.sendMessage("§e⚠ Replacing your " + currentOreType.getDisplayName() + " ore ability!");
            }

            // Set new ore type directly
            dataManager.setPlayerOre(player, newOreType);

            // Apply new ore effects
            applyOreTypeEffects(player, newOreType);

            // Success messages and effects
            String oreColor = getOreColor(newOreType);
            player.sendMessage("§a✓ Successfully mastered the " + oreColor + newOreType.getDisplayName() + " §aore!");
            player.sendMessage("§7Your new ability: §6" + getAbilityName(newOreType));

            // Use config-based cooldown in message
            int cooldown = configs != null ? configs.getCooldown(newOreType) : newOreType.getCooldown();
            player.sendMessage("§7Cooldown: §b" + cooldown + " seconds");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

            // Update action bar to show new ore type
            plugin.getActionBarManager().stopActionBar(player);
            plugin.getActionBarManager().startActionBar(player);

            return;
        }

        // FIXED: Handle gold ore curse for tools/weapons/armor crafting
        if (currentOreType == OreType.GOLD && isToolWeaponOrArmor(result.getType())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Apply gold curse to newly crafted items
                    applyGoldCurse(player, result.getType());
                }
            }.runTaskLater(plugin, 2);
        }
    }

    // FIXED: Better gold curse implementation
    private void applyGoldCurse(Player player, Material itemType) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == itemType) {
                short maxDurability = item.getType().getMaxDurability();
                if (maxDurability > 0) {
                    // Randomize durability between 10% and 90% of max
                    int minDurability = (int) (maxDurability * 0.1);
                    int maxRandomDurability = (int) (maxDurability * 0.9);
                    int randomDurability = random.nextInt(maxRandomDurability - minDurability) + minDurability;

                    short damageValue = (short) (maxDurability - randomDurability);
                    item.setDurability(damageValue);

                    player.sendMessage("§6Gold curse! Item durability set to " + randomDurability + "/" + maxDurability + "!");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 0.8f);
                    break;
                }
            }
        }
    }

    private void applyOreTypeEffects(Player player, OreType oreType) {
        switch (oreType) {
            case IRON:
                // FIXED: Apply +2 armor bonus properly
                AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);
                if (armorAttribute != null) {
                    double currentBase = armorAttribute.getBaseValue();
                    armorAttribute.setBaseValue(currentBase + 2);
                }
                plugin.getPlayerDataManager().setIronDropTimer(player);
                break;
            case AMETHYST:
                // FIXED: Apply permanent glowing
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
                break;
            case EMERALD:
                // Infinite hero of the village
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 9, false, false));
                break;
            case COPPER:
                // FIXED: Apply armor durability curse
                startArmorDurabilityTimer(player);
                break;
            case STONE:
                // Stone effects are handled in PlayerListener based on movement
                break;
            case DIRT:
                // Dirt effects are handled in PlayerListener based on armor
                break;
        }
    }

    private void removeOreTypeEffects(Player player, OreType oreType) {
        switch (oreType) {
            case IRON:
                // FIXED: Reset armor attribute properly
                AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);
                if (armorAttribute != null) {
                    double currentBase = armorAttribute.getBaseValue();
                    armorAttribute.setBaseValue(Math.max(0, currentBase - 2));
                }
                break;
            case AMETHYST:
                player.removePotionEffect(PotionEffectType.GLOWING);
                break;
            case EMERALD:
                player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
                break;
            case COPPER:
                // Stop armor durability timer
                stopArmorDurabilityTimer(player);
                break;
            case STONE:
                player.removePotionEffect(PotionEffectType.REGENERATION);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                break;
            case DIRT:
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                break;
        }
    }

    // FIXED: Copper armor durability curse with proper cleanup
    private void startArmorDurabilityTimer(Player player) {
        // Stop any existing timer first
        stopArmorDurabilityTimer(player);

        OreConfigs configs = plugin.getOreConfigs();
        int damageInterval = configs != null ? configs.getCopperArmorDamageInterval() : 100;
        double multiplier = configs != null ? configs.getCopperArmorDurabilityMultiplier() : 2.0;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                PlayerDataManager dataManager = plugin.getPlayerDataManager();
                if (dataManager.getPlayerOre(player) != OreType.COPPER || !player.isOnline()) {
                    cancel();
                    copperArmorTasks.remove(player.getUniqueId());
                    return;
                }

                // Damage armor pieces
                ItemStack[] armorContents = player.getInventory().getArmorContents();
                boolean armorDamaged = false;

                for (ItemStack armor : armorContents) {
                    if (armor != null && armor.getType() != Material.AIR) {
                        short maxDurability = armor.getType().getMaxDurability();
                        if (maxDurability > 0) {
                            short currentDurability = armor.getDurability();
                            int damageToApply = (int) Math.ceil(multiplier);
                            armor.setDurability((short) Math.min(maxDurability, currentDurability + damageToApply));
                            armorDamaged = true;
                        }
                    }
                }

                if (armorDamaged) {
                    player.getInventory().setArmorContents(armorContents);
                }
            }
        };

        task.runTaskTimer(plugin, damageInterval, damageInterval);
        copperArmorTasks.put(player.getUniqueId(), task);
    }

    private void stopArmorDurabilityTimer(Player player) {
        BukkitRunnable task = copperArmorTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private String getOreColor(OreType oreType) {
        switch (oreType) {
            case DIRT: return "§6";
            case WOOD: return "§e";
            case STONE: return "§7";
            case COAL: return "§8";
            case COPPER: return "§c";
            case IRON: return "§f";
            case GOLD: return "§e";
            case REDSTONE: return "§4";
            case LAPIS: return "§9";
            case EMERALD: return "§a";
            case AMETHYST: return "§d";
            case DIAMOND: return "§b";
            case NETHERITE: return "§8";
            default: return "§7";
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
            default: return "Unknown Ability";
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == OreType.LAPIS && event.getInventory().getType().name().contains("ANVIL")) {
            if (event.getSlot() == 2) { // Result slot
                event.setCancelled(true);
                player.sendMessage("§cLapis prevents you from using levels with anvils!");
            }
        }
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        AbilityManager abilityManager = plugin.getAbilityManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == OreType.LAPIS && abilityManager.hasActiveEffect(player)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
        }
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == OreType.NETHERITE && event.getBucket() == Material.WATER_BUCKET) {
            if (random.nextDouble() < 0.5) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.getInventory().setItemInMainHand(new ItemStack(Material.LAVA_BUCKET));
                        player.sendMessage("§4Netherite curse! Your water bucket turned to lava!");
                    }
                }.runTaskLater(plugin, 1);
            }
        }
    }

    private boolean isToolWeaponOrArmor(Material material) {
        String name = material.name();
        return name.contains("_SWORD") || name.contains("_AXE") ||
                name.contains("_PICKAXE") || name.contains("_SHOVEL") ||
                name.contains("_HOE") || name.contains("_HELMET") ||
                name.contains("_CHESTPLATE") || name.contains("_LEGGINGS") ||
                name.contains("_BOOTS");
    }

    // Cleanup method for player logout
    public void cleanup(Player player) {
        lastWaterDamageTime.remove(player.getUniqueId());
        stopArmorDurabilityTimer(player);
    }
}