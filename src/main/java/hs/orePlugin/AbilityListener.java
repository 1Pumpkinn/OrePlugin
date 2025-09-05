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
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class AbilityListener implements Listener {

    private final OreAbilitiesPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> lastWaterDamageTime = new HashMap<>();

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
                    // Strike lightning but prevent damage to the copper user
                    Location strikeLocation = event.getEntity().getLocation();
                    event.getEntity().getWorld().strikeLightning(strikeLocation);

                    // If the attacker is near the lightning, protect them
                    if (attacker.getLocation().distance(strikeLocation) < 5) {
                        // Give temporary fire resistance to prevent lightning damage
                        attacker.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0));
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
                    // Fixed: Use JUMP_BOOST with negative amplifier to prevent jumping
                    target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 200, -10, false, false));
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
                // Fixed: Check if player is in water and apply damage
                if (player.isInWater() && event.getCause() != EntityDamageEvent.DamageCause.FIRE) {
                    UUID uuid = player.getUniqueId();
                    long currentTime = System.currentTimeMillis();

                    // Prevent spam damage - only damage every 2 seconds while in water
                    if (!lastWaterDamageTime.containsKey(uuid) ||
                            currentTime - lastWaterDamageTime.get(uuid) > 2000) {

                        player.damage(2.0);
                        player.sendMessage("§8Coal curse! Water burns you!");
                        lastWaterDamageTime.put(uuid, currentTime);
                    }
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
                // Fixed: Dripstone damage prevention
                if (event.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK ||
                        event.getCause() == EntityDamageEvent.DamageCause.STAGMITE) {
                    event.setCancelled(true);
                }
                break;

            case AMETHYST:
                if (abilityManager.hasActiveEffect(player)) {
                    event.setCancelled(true);
                    player.setVelocity(player.getVelocity().multiply(0));
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
            // Check if player moved out of water to reset damage timer
            if (!player.isInWater()) {
                lastWaterDamageTime.remove(player.getUniqueId());
            }
        }
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

        // Handle gold ore curse for tools/weapons/armor crafting - FIXED
        if (currentOreType == OreType.GOLD && isToolWeaponOrArmor(result.getType())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Check all inventory slots for the newly crafted item
                    ItemStack[] contents = player.getInventory().getContents();
                    for (int i = 0; i < contents.length; i++) {
                        ItemStack item = contents[i];
                        if (item != null && item.getType() == result.getType()) {
                            // Randomize durability from 1 to max durability
                            short maxDurability = item.getType().getMaxDurability();
                            if (maxDurability > 0) {
                                int randomDurability = random.nextInt(100) + 1;
                                short newDurability = (short) Math.min(randomDurability, maxDurability);
                                item.setDurability((short) (maxDurability - newDurability));
                                player.sendMessage("§6Gold curse! Item durability randomized to " + newDurability + "/" + maxDurability + "!");
                                break;
                            }
                        }
                    }
                }
            }.runTaskLater(plugin, 2);
        }
    }

    private void applyOreTypeEffects(Player player, OreType oreType) {
        switch (oreType) {
            case IRON:
                // Apply +2 armor bonus - FIXED to work properly
                AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);
                if (armorAttribute != null) {
                    armorAttribute.setBaseValue(armorAttribute.getBaseValue() + 2);
                }
                plugin.getPlayerDataManager().setIronDropTimer(player);
                break;
            case AMETHYST:
                // Apply permanent glowing - FIXED
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
                break;
            case EMERALD:
                // Infinite hero of the village
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 9, false, false));
                break;
            case COPPER:
                // Apply armor durability curse - FIXED
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
                // Reset armor attribute - FIXED
                AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);
                if (armorAttribute != null) {
                    armorAttribute.setBaseValue(Math.max(0, armorAttribute.getBaseValue() - 2));
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

    // FIXED: Copper armor durability curse
    private void startArmorDurabilityTimer(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerDataManager dataManager = plugin.getPlayerDataManager();
                if (dataManager.getPlayerOre(player) != OreType.COPPER || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Damage armor pieces
                ItemStack[] armorContents = player.getInventory().getArmorContents();
                for (ItemStack armor : armorContents) {
                    if (armor != null && armor.getType() != Material.AIR) {
                        short maxDurability = armor.getType().getMaxDurability();
                        if (maxDurability > 0) {
                            short currentDurability = armor.getDurability();
                            armor.setDurability((short) Math.min(maxDurability, currentDurability + 2)); // 2x faster wear
                        }
                    }
                }
                player.getInventory().setArmorContents(armorContents);
            }
        }.runTaskTimer(plugin, 20, 100); // Check every 5 seconds
    }

    private void stopArmorDurabilityTimer(Player player) {
        // Timer will stop automatically when ore type changes
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
}