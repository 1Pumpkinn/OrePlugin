package hs.orePlugin;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.AnvilInventory;
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
    private final Map<UUID, Long> lastRainDamageTime = new HashMap<>();
    private final Map<UUID, BukkitRunnable> copperArmorTasks = new HashMap<>();
    private final Map<UUID, BukkitRunnable> diamondArmorTasks = new HashMap<>();
    private final Map<UUID, BukkitRunnable> coalRainTasks = new HashMap<>();

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
                        event.setDamage(event.getDamage() * 1.4);
                        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 2.0f);
                    }
                }
                break;

            case REDSTONE:
                if (abilityManager.hasActiveEffect(attacker) && event.getEntity() instanceof Player) {
                    Player target = (Player) event.getEntity();
                    plugin.getAbilityManager().addNoJumpEffect(target.getUniqueId(), 200);
                    target.sendMessage("§4Sticky Slime! You cannot jump for 10 seconds!");
                    attacker.sendMessage("§cSticky Slime effect applied to " + target.getName() + "!");
                    plugin.getAbilityManager().removeActiveEffect(attacker);
                }
                break;

            case AMETHYST:
                ItemStack offhand = attacker.getInventory().getItemInOffHand();
                if (offhand != null && offhand.getType() == Material.AMETHYST_SHARD) {
                    event.setDamage(event.getDamage() * 1.1);
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
                    player.sendMessage("§4Dripstone/Stalactite do not effect You!");
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
            handleCoalRainDamage(player);
        }

        if (oreType != null && plugin.getAbilityManager().hasNoJumpEffect(player.getUniqueId())) {
            if (event.getTo() != null && event.getFrom() != null) {
                double yDiff = event.getTo().getY() - event.getFrom().getY();
                if (yDiff > 0.1 && !player.isFlying() && player.getVelocity().getY() > 0.1) {
                    Location newLoc = event.getFrom().clone();
                    newLoc.setYaw(event.getTo().getYaw());
                    newLoc.setPitch(event.getTo().getPitch());
                    event.setTo(newLoc);
                    player.setVelocity(player.getVelocity().setY(0));
                }
            }
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

        // NEW: Prevent wood players from picking up efficiency > 3 axes
        if (oreType == OreType.WOOD) {
            ItemStack item = event.getItem().getItemStack();
            if (item.getType().name().contains("AXE")) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getEnchantLevel(Enchantment.EFFICIENCY) > 3) {
                    event.setCancelled(true);
                    player.sendMessage("§cWood ore limitation! Cannot pick up axes with Efficiency above 3!");
                }
            }
        }
    }

    // NEW: Prevent wood players from enchanting axes above efficiency 3
    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        if (!(event.getEnchanter() instanceof Player)) return;

        Player player = (Player) event.getEnchanter();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);
        AbilityManager abilityManager = plugin.getAbilityManager();

        if (oreType == OreType.WOOD && event.getItem().getType().name().contains("AXE")) {
            if (event.getEnchantsToAdd().containsKey(Enchantment.EFFICIENCY)) {
                int currentLevel = event.getItem().getEnchantmentLevel(Enchantment.EFFICIENCY);
                int newLevel = event.getEnchantsToAdd().get(Enchantment.EFFICIENCY);

                if (currentLevel + newLevel > 3) {
                    event.setCancelled(true);
                    player.sendMessage("§cWood ore limitation! Cannot enchant axes with Efficiency above 3!");
                    return;
                }
            }
        }

        if (oreType == OreType.LAPIS && abilityManager.hasActiveEffect(player)) {
            int originalLevel = player.getLevel();
            float originalExp = player.getExp();

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.setLevel(originalLevel);
                    player.setExp(originalExp);
                    player.sendMessage("§9No levels consumed!");
                }
            }.runTaskLater(plugin, 1);
        }
    }

    // NEW: Prevent wood players from using anvils to get efficiency > 3 on axes
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;

        OreType oreType = plugin.getPlayerDataManager().getPlayerOre(player);

        if (oreType == OreType.WOOD && event.getResult() != null && event.getResult().getType().name().contains("AXE")) {
            ItemMeta meta = event.getResult().getItemMeta();
            if (meta != null && meta.getEnchantLevel(Enchantment.EFFICIENCY) > 3) {
                event.setResult(null);
                player.sendMessage("§cWood ore limitation! Cannot create axes with Efficiency above 3!");
            }
        }

        if (oreType == OreType.LAPIS) {
            if (event.getResult() != null && !event.getResult().getType().isAir()) {
                event.getInventory().setRepairCost(0);
            }
        }
    }

    @EventHandler
    public void onAnvilClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory() instanceof AnvilInventory anvil)) return;

        OreType oreType = plugin.getPlayerDataManager().getPlayerOre(player);

        if (oreType == OreType.LAPIS && event.getSlotType() == InventoryType.SlotType.RESULT) {
            // Make sure they are taking the result
            if (event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
                // Override level cost
                anvil.setRepairCost(0);
                player.setLevel(player.getLevel());
            }
        }
    }

    // NEW: Handle armor durability modifications when player takes damage
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == null) return;

        ItemStack item = event.getItem();

        // Check if it's armor being damaged
        if (isArmor(item.getType())) {
            switch (oreType) {
                case COPPER:
                    // Copper: Armor breaks 1.5x faster (increase damage by 50%)
                    if (random.nextDouble() < 0.5) { // 50% chance to take additional damage
                        event.setDamage(event.getDamage() + 1);
                    }
                    break;
                case DIAMOND:
                    // Diamond: Armor takes 1.5x longer to break (reduce damage by 33%)
                    if (random.nextDouble() < 0.33) { // 33% chance to prevent damage
                        event.setCancelled(true);
                    }
                    break;
            }
        }

        // Keep existing dirt leather armor protection
        if (oreType == OreType.DIRT) {
            if (item != null && item.getType().name().contains("LEATHER")) {
                event.setCancelled(true);
            }
        }
    }

    // Helper method to check if an item is armor
    private boolean isArmor(Material material) {
        String name = material.name();
        return name.contains("_HELMET") || name.contains("_CHESTPLATE") ||
                name.contains("_LEGGINGS") || name.contains("_BOOTS");
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
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.5f);
                lastWaterDamageTime.put(uuid, currentTime);
            }
        } else {
            lastWaterDamageTime.remove(player.getUniqueId());
        }
    }

    private void handleCoalRainDamage(Player player) {
        if (player.getWorld().hasStorm() && !isUnderShelter(player)) {
            UUID uuid = player.getUniqueId();
            long currentTime = System.currentTimeMillis();

            if (!lastRainDamageTime.containsKey(uuid) ||
                    currentTime - lastRainDamageTime.get(uuid) > 2000) {

                double damage = 1.0;
                player.damage(damage);
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 2.0f);
                lastRainDamageTime.put(uuid, currentTime);
            }
        } else {
            lastRainDamageTime.remove(player.getUniqueId());
        }
    }

    private boolean isUnderShelter(Player player) {
        Location loc = player.getLocation();
        for (int y = 1; y <= 10; y++) {
            Block block = loc.clone().add(0, y, 0).getBlock();
            if (block.getType().isSolid()) {
                return true;
            }
        }
        return false;
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

        if (RecipeManager.isDirectOreItem(result)) {
            return;
        }

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

    public void applyAllOreTypeEffectsFixed(Player player, OreType oreType) {
        switch (oreType) {
            case DIRT:
                checkAndApplyDirtArmor(player);
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

    public void removeAllOreTypeEffectsFixed(Player player, OreType oreType) {
        switch (oreType) {
            case DIRT:
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                AttributeInstance dirtArmor = player.getAttribute(Attribute.ARMOR);
                if (dirtArmor != null) {
                    dirtArmor.setBaseValue(0);
                }
                break;
            case IRON:
                AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);
                if (armorAttribute != null) {
                    double currentBase = armorAttribute.getBaseValue();
                    armorAttribute.setBaseValue(Math.max(0, currentBase - 2));
                }
                plugin.getAbilityManager().cancelIronDropTimer(player);
                break;
            case AMETHYST:
                plugin.getAbilityManager().cancelAmethystGlowing(player);
                break;
            case EMERALD:
                player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
                player.removePotionEffect(PotionEffectType.WEAKNESS);
                break;
            case COPPER:
                stopCopperArmorDurabilityTimer(player);
                break;
            case DIAMOND:
                stopDiamondArmorProtectionTimer(player);
                break;
            case STONE:
                player.removePotionEffect(PotionEffectType.REGENERATION);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                break;
            case COAL:
                lastWaterDamageTime.remove(player.getUniqueId());
                lastRainDamageTime.remove(player.getUniqueId());
                stopCoalRainTimer(player);
                break;
            case WOOD:
                // No cleanup needed for wood ore
                break;
            case NETHERITE:
            case REDSTONE:
            case LAPIS:
            case GOLD:
                break;
        }
    }

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
                armorAttribute.setBaseValue(20);
            } else {
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
            player.sendMessage("§cDirt Downside! You need full leather armor!");
        } else if (hasFullLeatherArmor && player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) {
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            player.sendMessage("§aDirt Upside! Mining fatigue removed!");
        }
    }

    public void startCoalRainTimer(Player player) {
        stopCoalRainTimer(player);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                PlayerDataManager dataManager = plugin.getPlayerDataManager();
                if (dataManager.getPlayerOre(player) != OreType.COAL || !player.isOnline()) {
                    cancel();
                    coalRainTasks.remove(player.getUniqueId());
                    return;
                }

                handleCoalRainDamage(player);
            }
        };

        task.runTaskTimer(plugin, 40, 40);
        coalRainTasks.put(player.getUniqueId(), task);
    }

    public void stopCoalRainTimer(Player player) {
        BukkitRunnable task = coalRainTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

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

                ItemStack[] armorContents = player.getInventory().getArmorContents();
                boolean armorDamaged = false;

                for (ItemStack armor : armorContents) {
                    if (armor != null && armor.getType() != Material.AIR) {
                        if (armor.getItemMeta() instanceof Damageable) {
                            Damageable damageable = (Damageable) armor.getItemMeta();
                            int currentDamage = damageable.getDamage();
                            int maxDurability = armor.getType().getMaxDurability();

                            if (maxDurability > 0 && currentDamage < maxDurability) {
                                damageable.setDamage(currentDamage + 2);
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

        task.runTaskTimer(plugin, 100, 100);
        copperArmorTasks.put(player.getUniqueId(), task);
    }

    public void stopCopperArmorDurabilityTimer(Player player) {
        BukkitRunnable task = copperArmorTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

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

                skipCounter++;
                if (skipCounter % 2 == 0) {
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

        task.runTaskTimer(plugin, 100, 100);
        diamondArmorTasks.put(player.getUniqueId(), task);
    }

    public void stopDiamondArmorProtectionTimer(Player player) {
        BukkitRunnable task = diamondArmorTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
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

        if (oreType == OreType.LAPIS && event.getRightClicked() instanceof Villager) {
            event.setCancelled(true);
            player.sendMessage("§cLapis prevents you from trading with villagers!");
        }
    }

    // Add this method to AbilityListener.java after the existing onAnvilClick method

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        // Check for wood ore axe efficiency restriction
        if (oreType == OreType.WOOD) {
            ItemStack clickedItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();

            // Check item being moved into inventory
            if (clickedItem != null && isHighEfficiencyAxe(clickedItem) && isMovingToPlayerInventory(event)) {
                event.setCancelled(true);
                player.sendMessage("§cWood ore limitation! Cannot move axes with Efficiency above 3 to your inventory!");
                return;
            }

            // Check item on cursor being placed
            if (cursorItem != null && isHighEfficiencyAxe(cursorItem) && isMovingToPlayerInventory(event)) {
                event.setCancelled(true);
                player.sendMessage("§cWood ore limitation! Cannot place axes with Efficiency above 3 in your inventory!");
                return;
            }

            // Handle shift-click transfers
            if (event.isShiftClick() && clickedItem != null && isHighEfficiencyAxe(clickedItem)) {
                // Check if shift-clicking from a container to player inventory
                if (event.getInventory() != player.getInventory() &&
                        event.getSlot() < event.getInventory().getSize()) {
                    event.setCancelled(true);
                    player.sendMessage("§cWood ore limitation! Cannot shift-click axes with Efficiency above 3 to your inventory!");
                    return;
                }
            }
        }

        // Existing armor slot change detection for dirt ore (fixed for 1.21.8)
        if (oreType == OreType.DIRT && event.getSlotType() == InventoryType.SlotType.ARMOR) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    checkAndApplyDirtArmor(player);
                    checkAndApplyDirtMiningFatigue(player);
                }
            }.runTaskLater(plugin, 1);
        }
    }

    // Helper method to check if an axe has efficiency > 3
    private boolean isHighEfficiencyAxe(ItemStack item) {
        if (item == null || !item.getType().name().contains("AXE")) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int efficiencyLevel = meta.getEnchantLevel(Enchantment.EFFICIENCY);
            return efficiencyLevel > 3;
        }

        return false;
    }

    // Helper method to check if item is being moved to player inventory
    private boolean isMovingToPlayerInventory(InventoryClickEvent event) {
        // Check if clicking in player inventory area
        if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
            return true;
        }

        // Check if it's a shift-click that would move to player inventory
        if (event.isShiftClick() && event.getClickedInventory() != event.getWhoClicked().getInventory()) {
            return true;
        }

        return false;
    }

    // Also add this additional check for drag events (optional but recommended)
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == OreType.WOOD) {
            ItemStack draggedItem = event.getOldCursor();

            if (isHighEfficiencyAxe(draggedItem)) {
                // Check if any dragged slots belong to player inventory
                for (int slot : event.getRawSlots()) {
                    if (slot < player.getInventory().getSize()) {
                        event.setCancelled(true);
                        player.sendMessage("§cWood ore limitation! Cannot drag axes with Efficiency above 3 into your inventory!");
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
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

    public void cleanup(Player player) {
        lastWaterDamageTime.remove(player.getUniqueId());
        lastRainDamageTime.remove(player.getUniqueId());
        stopCopperArmorDurabilityTimer(player);
        stopDiamondArmorProtectionTimer(player);
        stopCoalRainTimer(player);
    }
}