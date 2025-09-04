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
import java.util.Random;

public class AbilityListener implements Listener {

    private final OreAbilitiesPlugin plugin;
    private final Random random = new Random();

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
                    event.getEntity().getWorld().strikeLightning(event.getEntity().getLocation());
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
                    target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 200, -10));
                    target.sendMessage("§4You cannot jump for 10 seconds!");
                    attacker.sendMessage("§cSticky Slime effect applied to " + target.getName() + "!");
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
                if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING ||
                        (player.isInWater() && event.getCause() != EntityDamageEvent.DamageCause.FIRE)) {
                    event.setDamage(event.getDamage() + 2);
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
                if (event.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK) {
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
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
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

            // 25% chance to shatter
            if (random.nextDouble() < 0.25) {
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
            player.sendMessage("§7Cooldown: §b" + newOreType.getCooldown() + " seconds");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

            // Update action bar to show new ore type
            plugin.getActionBarManager().stopActionBar(player);
            plugin.getActionBarManager().startActionBar(player);

            return;
        }

        // Handle gold ore curse for tools/weapons/armor crafting
        if (currentOreType == OreType.GOLD && isToolWeaponOrArmor(result.getType())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item.getType() == result.getType()) {
                        int randomDurability = random.nextInt(100) + 1;
                        item.setDurability((short) (item.getType().getMaxDurability() - randomDurability));
                        player.sendMessage("§6Gold curse! Item durability randomized!");
                    }
                }
            }.runTaskLater(plugin, 1);
        }
    }

    private void applyOreTypeEffects(Player player, OreType oreType) {
        switch (oreType) {
            case IRON:
                // Apply +2 armor bonus
                if (player.getAttribute(org.bukkit.attribute.Attribute.ARMOR) != null) {
                    double currentBase = player.getAttribute(org.bukkit.attribute.Attribute.ARMOR).getBaseValue();
                    player.getAttribute(org.bukkit.attribute.Attribute.ARMOR).setBaseValue(currentBase + 2);
                }
                plugin.getPlayerDataManager().setIronDropTimer(player);
                break;
            case AMETHYST:
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
                break;
            case EMERALD:
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 9));
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
                // Reset armor attribute
                if (player.getAttribute(org.bukkit.attribute.Attribute.ARMOR) != null) {
                    player.getAttribute(org.bukkit.attribute.Attribute.ARMOR).setBaseValue(0);
                }
                break;
            case AMETHYST:
                player.removePotionEffect(PotionEffectType.GLOWING);
                break;
            case EMERALD:
                player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
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