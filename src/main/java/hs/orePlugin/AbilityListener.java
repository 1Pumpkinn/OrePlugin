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
    private final Map<UUID, BukkitRunnable> coalRainTasks = new HashMap<>();

    public AbilityListener(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    // NEW EVENT HANDLER - Add this method to handle damage DEALT by players
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        AbilityManager abilityManager = plugin.getAbilityManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == null) return;

        switch (oreType) {
            case WOOD:
                if (abilityManager.hasActiveEffect(player)) {
                    ItemStack weapon = player.getInventory().getItemInMainHand();
                    if (weapon != null && isAxe(weapon.getType())) {
                        double originalDamage = event.getDamage();
                        double newDamage = originalDamage * 1.3;
                        event.setDamage(newDamage);

                        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, 1.0f, 1.2f);
                    }
                }
                break;

            case DIAMOND:
                if (abilityManager.hasActiveEffect(player)) {
                    ItemStack weapon = player.getInventory().getItemInMainHand();
                    if (weapon != null && weapon.getType() == Material.DIAMOND_SWORD) {
                        double originalDamage = event.getDamage();
                        double newDamage = originalDamage * 1.4;
                        event.setDamage(newDamage);
                        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_HIT, 1.0f, 1.5f);
                    }
                }
                break;

            case REDSTONE:
                // Apply no-jump effect when hitting with redstone ability active
                if (abilityManager.hasActiveEffect(player) && event.getEntity() instanceof Player) {
                    Player target = (Player) event.getEntity();
                    abilityManager.addNoJumpEffect(target.getUniqueId(), 200); // 10 seconds
                    player.sendMessage("§4Sticky Slime applied! Target cannot jump for 10 seconds!");
                    target.sendMessage("§cYou've been affected by Sticky Slime! Cannot jump for 10 seconds!");

                    abilityManager.removeActiveEffect(player);
                }
                break;

            case AMETHYST:
                // Check if player has amethyst shard in offhand for damage boost
                ItemStack offhandItem = player.getInventory().getItemInOffHand();
                if (offhandItem != null && offhandItem.getType() == Material.AMETHYST_SHARD) {
                    double originalDamage = event.getDamage();
                    double newDamage = originalDamage + 1.1;
                    event.setDamage(newDamage);
                }
                break;

            case COPPER:
                if (abilityManager.hasCopperLightningActive(player)) {
                    event.getEntity().getWorld().strikeLightning(event.getEntity().getLocation());
                    player.sendMessage("§3Channel The Clouds activated! Lightning struck your target!");
                    if (event.getEntity() instanceof Player) {
                        Player target = (Player) event.getEntity();
                        target.sendMessage("§c⚡ You were struck by lightning!");
                    }
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

                if (abilityManager.hasCopperLightningActive(player) && event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) event;
                    if (damageByEntity.getDamager() instanceof Player) {
                        Player attacker = (Player) damageByEntity.getDamager();
                        // Strike lightning at attacker's location
                        attacker.getWorld().strikeLightning(attacker.getLocation());
                        player.sendMessage("§3Channel The Clouds activated! Lightning struck your attacker!");
                        attacker.sendMessage("§c⚡ You were struck by lightning!");
                    }
                }
                break;

            case NETHERITE:
                if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                        event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                        event.getCause() == EntityDamageEvent.DamageCause.LAVA ||
                        event.getCause() == EntityDamageEvent.DamageCause.HOT_FLOOR) {
                    event.setCancelled(true);
                    // Also clear fire ticks to prevent visual fire
                    player.setFireTicks(0);
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

        if (oreType == OreType.WOOD) {
            ItemStack item = event.getItem().getItemStack();
            if (isAxe(item.getType())) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getEnchantLevel(Enchantment.EFFICIENCY) > 3) {
                    event.setCancelled(true);
                    player.sendMessage("§cWood ore limitation! Cannot pick up axes with Efficiency above 3!");
                }
            }
        }
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        if (!(event.getEnchanter() instanceof Player)) return;

        Player player = (Player) event.getEnchanter();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);
        AbilityManager abilityManager = plugin.getAbilityManager();

        // FIXED: Only restrict AXES for wood ore, allow other tools to have any efficiency level
        if (oreType == OreType.WOOD && isAxe(event.getItem().getType())) {
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

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;

        OreType oreType = plugin.getPlayerDataManager().getPlayerOre(player);

        // FIXED: Only restrict AXES for wood ore
        if (oreType == OreType.WOOD && event.getResult() != null && isAxe(event.getResult().getType())) {
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
            if (event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
                anvil.setRepairCost(0);
                player.setLevel(player.getLevel());
            }
        }
    }

    // FIXED: Armor durability is now handled when damage is taken, not on timers
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
                    int copperDamage = (int) Math.ceil(event.getDamage() * 2);
                    event.setDamage(copperDamage);
                    break;

                case DIAMOND:
                    if (event.getDamage() > 1) {
                        int reducedDurabilityDamage = (int) Math.ceil(event.getDamage() / 2);
                        event.setDamage(reducedDurabilityDamage);
                    } else {
                        event.setDamage(1);
                    }
                    break;
            }
        }

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

    // FIXED: More precise axe detection helper method
    private boolean isAxe(Material material) {
        return material == Material.WOODEN_AXE ||
                material == Material.STONE_AXE ||
                material == Material.IRON_AXE ||
                material == Material.GOLDEN_AXE ||
                material == Material.DIAMOND_AXE ||
                material == Material.NETHERITE_AXE;
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

                        player.sendMessage("§6Gold downside! Item durability set to " + randomDurability + "/" + maxDurability + "!");
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 0.8f);
                        break;
                    }
                }
            }
        }
    }

    public void applyAllOreTypeEffectsFixed(Player player, OreType oreType) {
        plugin.getLogger().info("Applying all ore effects for " + player.getName() + " with ore type: " + oreType.name());

        switch (oreType) {
            case DIRT:
                checkAndApplyDirtArmor(player);
                checkAndApplyDirtMiningFatigue(player);
                break;

            case IRON:
                AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);
                if (armorAttribute != null) {
                    double currentBase = armorAttribute.getBaseValue();
                    armorAttribute.setBaseValue(currentBase + 2.0);
                    player.sendMessage("§7Iron bonus: +1 armor bar applied!");
                }
                plugin.getAbilityManager().startIronDropTimer(player);
                break;

            case AMETHYST:
                plugin.getAbilityManager().startAmethystGlowing(player);
                break;

            case EMERALD:
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 9, false, false));
                player.sendMessage("§aEmerald Upside: Hero of the Village 10 applied!");
                plugin.getLogger().info("Applied Hero of the Village 10 to " + player.getName() + " via ore crafting");
                break;

            case NETHERITE:
                break;
            case COPPER:
                break;
            case DIAMOND:
                break;
            case COAL:
                break;
            case WOOD:
                break;
            case REDSTONE:
                break;
            case LAPIS:
                break;
            case GOLD:
                break;
            case STONE:
                break;
        }
    }

    // FIXED: Removed armor timer cleanup
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
                    armorAttribute.setBaseValue(Math.max(0.0, currentBase - 2.0));
                    player.sendMessage("§7Iron bonus removed: -1 armor bar");
                }
                plugin.getAbilityManager().cancelIronDropTimer(player);
                break;
            case AMETHYST:
                plugin.getAbilityManager().cancelAmethystGlowing(player);
                // Clean up team membership
                cleanupAmethystTeam(player);
                break;
            case EMERALD:
                player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
                player.removePotionEffect(PotionEffectType.WEAKNESS);
                break;
            case COPPER:
                // No timer to stop
                player.sendMessage("§7Copper armor effect removed");
                break;
            case DIAMOND:
                // No timer to stop
                player.sendMessage("§7Diamond armor effect removed");
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

    // Add method to clean up amethyst team membership
    private void cleanupAmethystTeam(Player player) {
        try {
            org.bukkit.scoreboard.Scoreboard scoreboard = player.getServer().getScoreboardManager().getMainScoreboard();
            org.bukkit.scoreboard.Team amethystTeam = scoreboard.getTeam("amethyst");
            if (amethystTeam != null && amethystTeam.hasEntry(player.getName())) {
                amethystTeam.removeEntry(player.getName());
                player.sendMessage("§7Amethyst team membership removed");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not remove player from amethyst team: " + e.getMessage());
        }
    }

    private void checkAndApplyDirtArmor(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean hasFullLeatherArmor = true;

        for (ItemStack piece : armor) {
            if (piece == null || piece.getType() == Material.AIR || !piece.getType().name().contains("LEATHER")) {
                hasFullLeatherArmor = false;
                break;
            }
        }

        AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);
        if (armorAttribute != null) {
            if (hasFullLeatherArmor) {
                armorAttribute.setBaseValue(20);

                // Make leather armor unbreakable
                for (ItemStack piece : armor) {
                    if (piece != null && piece.getType().name().contains("LEATHER")) {
                        ItemMeta meta = piece.getItemMeta();
                        if (meta != null) {
                            piece.setItemMeta(meta);
                        }
                    }
                }
            } else {
                armorAttribute.setBaseValue(0);
            }
        }
    }

    private void checkAndApplyDirtMiningFatigue(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean hasFullLeatherArmor = true;

        for (ItemStack piece : armor) {
            if (piece == null || piece.getType() == Material.AIR || !piece.getType().name().contains("LEATHER")) {
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
                        player.sendMessage("§4Netherite downside! Your water bucket turned to lava!");
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

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

    // Keep these helper methods for other event handlers that still need them
    private boolean isHighEfficiencyAxe(ItemStack item) {
        if (item == null || !isAxe(item.getType())) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int efficiencyLevel = meta.getEnchantLevel(Enchantment.EFFICIENCY);
            return efficiencyLevel > 3;
        }

        return false;
    }

    private boolean isMovingToPlayerInventory(InventoryClickEvent event) {
        if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
            return true;
        }

        if (event.isShiftClick() && event.getClickedInventory() != event.getWhoClicked().getInventory()) {
            return true;
        }

        return false;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);
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

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        // Existing copper trident logic
        if (oreType == OreType.COPPER) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    ItemStack item = player.getInventory().getItem(event.getNewSlot());
                    enchantTridentWithChanneling(player, item);
                }
            }.runTaskLater(plugin, 1);
        }

        // NEW: Wood ore axe efficiency reduction
        if (oreType == OreType.WOOD) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    ItemStack item = player.getInventory().getItem(event.getNewSlot());
                    reduceAxeEfficiencyIfNeeded(player, item);
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

    private void reduceAxeEfficiencyIfNeeded(Player player, ItemStack item) {
        if (item == null || !isAxe(item.getType())) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        int currentEfficiency = meta.getEnchantLevel(Enchantment.EFFICIENCY);
        if (currentEfficiency > 3) {

            meta.removeEnchant(Enchantment.EFFICIENCY);
            meta.addEnchant(Enchantment.EFFICIENCY, 3, true);
            item.setItemMeta(meta);

            player.sendMessage("§6Wood ore limitation! Axe efficiency reduced from " + currentEfficiency + " to 3!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.2f);
        }
    }

    public void cleanup(Player player) {
        lastWaterDamageTime.remove(player.getUniqueId());
        lastRainDamageTime.remove(player.getUniqueId());
        stopCoalRainTimer(player);
    }
}

