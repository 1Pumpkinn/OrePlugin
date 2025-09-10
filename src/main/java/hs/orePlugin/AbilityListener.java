package hs.orePlugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class AbilityListener implements Listener {

    private final OreAbilitiesPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> lastWaterDamageTime = new HashMap<>();
    private final Map<UUID, BukkitRunnable> copperArmorTasks = new HashMap<>();
    private final Map<UUID, BukkitRunnable> diamondArmorTasks = new HashMap<>();

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

                    // Better lightning protection for copper user
                    if (attacker.getLocation().distance(strikeLocation) < 10) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                attacker.setFireTicks(0);
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
                        event.setDamage(event.getDamage() * 2.0); // 2x damage for diamond
                        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 2.0f);
                    }
                }
                break;

            case REDSTONE:
                if (abilityManager.hasActiveEffect(attacker) && event.getEntity() instanceof Player) {
                    Player target = (Player) event.getEntity();
                    // FIXED: Use negative jump boost to prevent jumping entirely
                    target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 200, -10, false, false));
                    target.sendMessage("§4You cannot jump for 10 seconds!");
                    attacker.sendMessage("§cSticky Slime effect applied to " + target.getName() + "!");
                    plugin.getAbilityManager().removeActiveEffect(attacker);
                }
                break;

            case AMETHYST:
                ItemStack offhand = attacker.getInventory().getItemInOffHand();
                if (offhand != null && offhand.getType() == Material.AMETHYST_SHARD) {
                    event.setDamage(event.getDamage() + 1.5); // +1.5 damage
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
                if (event.getCause() == EntityDamageEvent.DamageCause.CUSTOM && player.isInWater()) {
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
                if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
                    event.setCancelled(true);
                    player.sendMessage("§3Copper protection from lightning!");
                }
                break;

            case NETHERITE:
                if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                        event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                        event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                    event.setCancelled(true);
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

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == OreType.COPPER) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    ItemStack item = player.getInventory().getItem(event.getNewSlot());
                    enchantTridentWithChanneling(player, item);
                }
            }.runTaskLater(plugin, 1);
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == OreType.COPPER && event.getItem().getItemStack().getType() == Material.TRIDENT) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    enchantTridentWithChanneling(player, event.getItem().getItemStack());
                }
            }.runTaskLater(plugin, 2);
        }
    }

    private void enchantTridentWithChanneling(Player player, ItemStack item) {
        if (item != null && item.getType() == Material.TRIDENT) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && !meta.hasEnchant(Enchantment.CHANNELING)) {
                meta.addEnchant(Enchantment.CHANNELING, 1, true);
                item.setItemMeta(meta);
                player.sendMessage("§3Your trident has been enchanted with Channeling!");
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
            }
        }
    }

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
            lastWaterDamageTime.remove(player.getUniqueId());
        }
    }

    private boolean isDripstoneOrStalactiteDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent fbEvent = (EntityDamageByEntityEvent) event;
                if (fbEvent.getDamager() instanceof FallingBlock) {
                    FallingBlock fb = (FallingBlock) fbEvent.getDamager();
                    return fb.getBlockData().getMaterial() == Material.POINTED_DRIPSTONE;
                }
            }
            return true;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.CONTACT) {
            Player player = (Player) event.getEntity();
            Location loc = player.getLocation();
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
        OreType currentOreType = dataManager.getPlayerOre(player);

        ItemStack result = event.getRecipe().getResult();

        // CRITICAL FIX: Only handle non-mastery items here
        // Let RecipeManager handle all ore mastery crafting exclusively
        if (RecipeManager.isDirectOreItem(result)) {
            // This is an ore mastery item - RecipeManager will handle it completely
            // DO NOT apply any effects here - let RecipeManager do the shatter check first
            return;
        }

        // Handle gold curse for regular items (non-mastery items)
        if (currentOreType == OreType.GOLD && isToolWeaponOrArmor(result.getType())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    applyGoldCurse(player, result.getType());
                }
            }.runTaskLater(plugin, 2);
        }
    }

    private void applyGoldCurse(Player player, Material itemType) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == itemType) {
                if (item.getItemMeta() instanceof Damageable) {
                    Damageable damageable = (Damageable) item.getItemMeta();
                    int maxDurability = item.getType().getMaxDurability();
                    if (maxDurability > 0) {
                        int randomDurability = random.nextInt(100) + 1;
                        if (randomDurability > maxDurability) {
                            randomDurability = maxDurability;
                        }

                        int damageValue = maxDurability - randomDurability;
                        damageable.setDamage(damageValue);
                        item.setItemMeta((ItemMeta) damageable);

                        player.sendMessage("§6Gold curse! Item durability set to " + randomDurability + "/" + maxDurability + "!");
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 0.8f);
                        break;
                    }
                }
            }
        }
    }

    // FIXED: Complete and comprehensive ore type effect application - MADE PUBLIC
    public void applyAllOreTypeEffectsFixed(Player player, OreType oreType) {
        switch (oreType) {
            case DIRT:
                // FIXED: Dirt gets diamond-level armor protection (20 armor points) but only with full leather armor
                checkAndApplyDirtArmor(player);
                // FIXED: Apply mining fatigue when not wearing full leather
                checkAndApplyDirtMiningFatigue(player);
                break;
            case IRON:
                AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);
                if (armorAttribute != null) {
                    double currentBase = armorAttribute.getBaseValue();
                    armorAttribute.setBaseValue(currentBase + 2);
                }
                plugin.getAbilityManager().startIronDropTimer(player);
                break;
            case AMETHYST:
                plugin.getAbilityManager().startAmethystGlowing(player);
                break;
            case EMERALD:
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 9, false, false));
                break;
            case COPPER:
                startCopperArmorDurabilityTimer(player);
                break;
            case DIAMOND:
                startDiamondArmorProtectionTimer(player);
                break;
        }
    }

    // MADE PUBLIC so RecipeManager can access it
    public void removeAllOreTypeEffectsFixed(Player player, OreType oreType) {
        switch (oreType) {
            case DIRT:
                // FIXED: Remove all dirt effects completely
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                // Reset armor to normal
                AttributeInstance dirtArmor = player.getAttribute(Attribute.ARMOR);
                if (dirtArmor != null) {
                    dirtArmor.setBaseValue(0); // Reset to default
                }
                player.sendMessage("§7Dirt ore effects removed.");
                break;
            case IRON:
                AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);
                if (armorAttribute != null) {
                    double currentBase = armorAttribute.getBaseValue();
                    armorAttribute.setBaseValue(Math.max(0, currentBase - 2));
                }
                plugin.getAbilityManager().cancelIronDropTimer(player);
                player.sendMessage("§7Iron ore effects removed.");
                break;
            case AMETHYST:
                plugin.getAbilityManager().cancelAmethystGlowing(player);
                player.sendMessage("§7Amethyst ore effects removed.");
                break;
            case EMERALD:
                player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
                player.removePotionEffect(PotionEffectType.WEAKNESS);
                player.sendMessage("§7Emerald ore effects removed.");
                break;
            case COPPER:
                stopCopperArmorDurabilityTimer(player);
                player.sendMessage("§7Copper ore effects removed.");
                break;
            case DIAMOND:
                stopDiamondArmorProtectionTimer(player);
                player.sendMessage("§7Diamond ore effects removed.");
                break;
            case STONE:
                // FIXED: Properly remove stone effects
                player.removePotionEffect(PotionEffectType.REGENERATION);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.sendMessage("§7Stone ore effects removed.");
                break;
            case COAL:
                // Remove coal water damage tracking
                lastWaterDamageTime.remove(player.getUniqueId());
                player.sendMessage("§7Coal ore effects removed.");
                break;
            case NETHERITE:
                // No persistent effects to remove for netherite
                player.sendMessage("§7Netherite ore effects removed.");
                break;
            case REDSTONE:
                // No persistent effects to remove for redstone
                player.sendMessage("§7Redstone ore effects removed.");
                break;
            case LAPIS:
                // No persistent effects to remove for lapis
                player.sendMessage("§7Lapis ore effects removed.");
                break;
            case GOLD:
                // No persistent effects to remove for gold
                player.sendMessage("§7Gold ore effects removed.");
                break;
            case WOOD:
                // No persistent effects to remove for wood
                player.sendMessage("§7Wood ore effects removed.");
                break;
        }
    }

    // FIXED: Dirt ore effects implementation
    private void checkAndApplyDirtArmor(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean hasFullLeatherArmor = true;

        for (ItemStack piece : armor) {
            if (piece == null || !piece.getType().name().contains("LEATHER")) {
                hasFullLeatherArmor = false;
                break;
            }
        }

        AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);
        if (armorAttribute != null) {
            if (hasFullLeatherArmor) {
                // Give diamond-level armor (20 armor points)
                armorAttribute.setBaseValue(20);
                player.sendMessage("§6Dirt blessing! Full leather armor is now unbreakable and diamond-strong!");
            } else {
                // Reset to normal armor calculation
                armorAttribute.setBaseValue(0);
            }
        }
    }

    private void checkAndApplyDirtMiningFatigue(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean hasFullLeatherArmor = true;

        for (ItemStack piece : armor) {
            if (piece == null || !piece.getType().name().contains("LEATHER")) {
                hasFullLeatherArmor = false;
                break;
            }
        }

        if (!hasFullLeatherArmor && !player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, Integer.MAX_VALUE, 0, false, false));
            player.sendMessage("§cDirt curse! You need full leather armor or you'll mine slowly!");
        } else if (hasFullLeatherArmor && player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) {
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            player.sendMessage("§aDirt blessing! Mining fatigue removed!");
        }
    }

    // FIXED: Copper armor durability timer (2x faster breaking) - Made public so other classes can access
    public void startCopperArmorDurabilityTimer(Player player) {
        stopCopperArmorDurabilityTimer(player);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                PlayerDataManager dataManager = plugin.getPlayerDataManager();
                if (dataManager.getPlayerOre(player) != OreType.COPPER || !player.isOnline()) {
                    cancel();
                    copperArmorTasks.remove(player.getUniqueId());
                    return;
                }

                // Damage armor pieces at 2x rate
                ItemStack[] armorContents = player.getInventory().getArmorContents();
                boolean armorDamaged = false;

                for (ItemStack armor : armorContents) {
                    if (armor != null && armor.getType() != Material.AIR) {
                        if (armor.getItemMeta() instanceof Damageable) {
                            Damageable damageable = (Damageable) armor.getItemMeta();
                            int currentDamage = damageable.getDamage();
                            int maxDurability = armor.getType().getMaxDurability();

                            if (maxDurability > 0 && currentDamage < maxDurability) {
                                damageable.setDamage(currentDamage + 2); // 2x faster degradation
                                armor.setItemMeta((ItemMeta) damageable);
                                armorDamaged = true;
                            }
                        }
                    }
                }

                if (armorDamaged) {
                    player.getInventory().setArmorContents(armorContents);
                }
            }
        };

        task.runTaskTimer(plugin, 100, 100); // Every 5 seconds
        copperArmorTasks.put(player.getUniqueId(), task);
    }

    // FIXED: Made public so other classes can access
    public void stopCopperArmorDurabilityTimer(Player player) {
        BukkitRunnable task = copperArmorTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    // FIXED: Diamond armor protection timer (2x slower breaking) - Made public so other classes can access
    public void startDiamondArmorProtectionTimer(Player player) {
        stopDiamondArmorProtectionTimer(player);

        BukkitRunnable task = new BukkitRunnable() {
            private int skipCounter = 0;

            @Override
            public void run() {
                PlayerDataManager dataManager = plugin.getPlayerDataManager();
                if (dataManager.getPlayerOre(player) != OreType.DIAMOND || !player.isOnline()) {
                    cancel();
                    diamondArmorTasks.remove(player.getUniqueId());
                    return;
                }

                // Skip every other damage tick to make armor last 2x longer
                skipCounter++;
                if (skipCounter % 2 == 0) {
                    // Repair armor slightly to counteract normal damage
                    ItemStack[] armorContents = player.getInventory().getArmorContents();
                    boolean armorRepaired = false;

                    for (ItemStack armor : armorContents) {
                        if (armor != null && armor.getType() != Material.AIR) {
                            if (armor.getItemMeta() instanceof Damageable) {
                                Damageable damageable = (Damageable) armor.getItemMeta();
                                int currentDamage = damageable.getDamage();

                                if (currentDamage > 0) {
                                    damageable.setDamage(Math.max(0, currentDamage - 1));
                                    armor.setItemMeta((ItemMeta) damageable);
                                    armorRepaired = true;
                                }
                            }
                        }
                    }

                    if (armorRepaired) {
                        player.getInventory().setArmorContents(armorContents);
                    }
                }
            }
        };

        task.runTaskTimer(plugin, 100, 100); // Every 5 seconds
        diamondArmorTasks.put(player.getUniqueId(), task);
    }

    // FIXED: Made public so other classes can access
    public void stopDiamondArmorProtectionTimer(Player player) {
        BukkitRunnable task = diamondArmorTasks.remove(player.getUniqueId());
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

        // FIXED: Lapis can use anvils without using XP levels
        if (oreType == OreType.LAPIS && event.getInventory().getType().name().contains("ANVIL")) {
            // Allow anvil use but restore XP after
            new BukkitRunnable() {
                int originalLevel = player.getLevel();
                float originalExp = player.getExp();

                @Override
                public void run() {
                    player.setLevel(originalLevel);
                    player.setExp(originalExp);
                    player.sendMessage("§9Lapis blessing! No XP consumed for anvil use!");
                }
            }.runTaskLater(plugin, 1);
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

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        // FIXED: Lapis prevents trading with villagers
        if (oreType == OreType.LAPIS && event.getRightClicked() instanceof Villager) {
            event.setCancelled(true);
            player.sendMessage("§cLapis prevents you from trading with villagers!");
        }
    }

    @EventHandler
    public void onPlayerItemBreak(PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        // FIXED: Dirt ore makes leather armor unbreakable
        if (oreType == OreType.DIRT) {
            ItemStack brokenItem = event.getBrokenItem();
            if (brokenItem.getType().name().contains("LEATHER")) {
                // Give them a new leather armor piece
                ItemStack replacement = new ItemStack(brokenItem.getType());
                if (brokenItem.hasItemMeta()) {
                    replacement.setItemMeta(brokenItem.getItemMeta());
                }

                // Add to inventory or drop if full
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(replacement);
                } else {
                    player.getWorld().dropItemNaturally(player.getLocation(), replacement);
                }

                player.sendMessage("§6Dirt blessing! Your leather armor was instantly replaced!");
            }
        }
    }

    @EventHandler
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        // Check for dirt ore effects when armor changes
        Player player = event.getPlayer();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == OreType.DIRT) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    checkAndApplyDirtArmor(player);
                    checkAndApplyDirtMiningFatigue(player);
                }
            }.runTaskLater(plugin, 1);
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
        stopCopperArmorDurabilityTimer(player);
        stopDiamondArmorProtectionTimer(player);
    }
}