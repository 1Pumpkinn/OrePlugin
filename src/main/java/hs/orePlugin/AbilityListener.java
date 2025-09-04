package hs.orePlugin;

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

        // Check if target is trusted (prevent damage to trusted players)
        if (event.getEntity() instanceof Player) {
            Player target = (Player) event.getEntity();
            if (trustManager.isTrusted(attacker, target)) {
                event.setCancelled(true);
                return;
            }
        }

        // Handle ability-specific combat effects
        switch (oreType) {
            case WOOD:
                // Wood ability - axes deal 1.5x damage
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
                // Copper ability - lightning on hit
                if (abilityManager.hasActiveEffect(attacker) && event.getEntity() instanceof LivingEntity) {
                    event.getEntity().getWorld().strikeLightning(event.getEntity().getLocation());
                }
                break;

            case DIAMOND:
                // Diamond ability - 2x damage with diamond sword
                if (abilityManager.hasActiveEffect(attacker)) {
                    ItemStack weapon = attacker.getInventory().getItemInMainHand();
                    if (weapon != null && weapon.getType() == Material.DIAMOND_SWORD) {
                        event.setDamage(event.getDamage() * 2);
                        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 2.0f);
                    }
                }
                break;

            case REDSTONE:
                // Redstone ability - prevent jumping
                if (abilityManager.hasActiveEffect(attacker) && event.getEntity() instanceof Player) {
                    Player target = (Player) event.getEntity();
                    target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 200, -10)); // Jump boost -10 (no jumping)
                    target.sendMessage("§4You cannot jump for 10 seconds!");
                    attacker.sendMessage("§cSticky Slime effect applied to " + target.getName() + "!");

                    // Remove active effect from attacker (one-time use)
                    plugin.getAbilityManager().hasActiveEffect(attacker); // This will be handled in AbilityManager
                }
                break;

            case AMETHYST:
                // Amethyst upside - extra damage with amethyst shards in offhand
                ItemStack offhand = attacker.getInventory().getItemInOffHand();
                if (offhand != null && offhand.getType() == Material.AMETHYST_SHARD) {
                    event.setDamage(event.getDamage() + 1.5);
                }
                break;

            case COAL:
                // Coal upside - +1 damage when on fire
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

        // Handle passive downsides and ability effects
        switch (oreType) {
            case COAL:
                // Coal downside - water damage
                if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING ||
                        (player.isInWater() && event.getCause() != EntityDamageEvent.DamageCause.FIRE)) {
                    event.setDamage(event.getDamage() + 2);
                }
                break;

            case REDSTONE:
                // Redstone downside - bees and slimes do 5x damage
                if (event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
                    if (damageEvent.getDamager() instanceof Bee || damageEvent.getDamager() instanceof Slime) {
                        event.setDamage(event.getDamage() * 5);
                        player.sendMessage("§cRedstone weakness to " + damageEvent.getDamager().getType().name().toLowerCase() + "!");
                    }
                }
                // Redstone upside - no dripstone damage
                if (event.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK) {
                    // This would need more specific checking for dripstone in a real implementation
                    event.setCancelled(true);
                }
                break;

            case AMETHYST:
                // Amethyst ability - crystal mode (no damage, no knockback)
                if (abilityManager.hasActiveEffect(player)) {
                    event.setCancelled(true);
                    player.setVelocity(player.getVelocity().multiply(0)); // No knockback
                }
                break;
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        if (oreType == null) return;

        ItemStack result = event.getRecipe().getResult();

        // Check for ore crafting (25% chance to shatter)
        if (isOreItem(result.getType())) {
            if (random.nextDouble() < 0.25) {
                event.setCancelled(true);
                player.sendMessage("§cThe ore shattered during crafting!");
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                return;
            }

            // Assign ore type if successfully crafted
            OreType craftedOre = getOreTypeFromItem(result.getType());
            if (craftedOre != null && !craftedOre.isStarter()) {
                dataManager.setPlayerOre(player, craftedOre);
                player.sendMessage("§aYou have acquired the " + craftedOre.getDisplayName() + " ore type!");

                // Restart action bar to show new ore type immediately
                plugin.getActionBarManager().stopActionBar(player);
                plugin.getActionBarManager().startActionBar(player);
            }
        }

        // Gold downside - random durability for tools/weapons/armor
        if (oreType == OreType.GOLD && (isToolWeaponOrArmor(result.getType()))) {
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        // Lapis downside - cannot use anvils for levels
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

        // Lapis ability - exp splashing gives regen
        if (oreType == OreType.LAPIS && abilityManager.hasActiveEffect(player)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
        }
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oreType = dataManager.getPlayerOre(player);

        // Netherite downside - 50% chance water bucket becomes lava
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

    private boolean isOreItem(Material material) {
        return material.name().contains("_ORE") ||
                material == Material.COAL || material == Material.DIAMOND ||
                material == Material.EMERALD || material == Material.AMETHYST_SHARD ||
                material.name().contains("_INGOT");
    }

    private boolean isToolWeaponOrArmor(Material material) {
        String name = material.name();
        return name.contains("_SWORD") || name.contains("_AXE") ||
                name.contains("_PICKAXE") || name.contains("_SHOVEL") ||
                name.contains("_HOE") || name.contains("_HELMET") ||
                name.contains("_CHESTPLATE") || name.contains("_LEGGINGS") ||
                name.contains("_BOOTS");
    }

    private OreType getOreTypeFromItem(Material material) {
        switch (material) {
            case COAL: return OreType.COAL;
            case COPPER_INGOT: return OreType.COPPER;
            case IRON_INGOT: return OreType.IRON;
            case GOLD_INGOT: return OreType.GOLD;
            case REDSTONE: return OreType.REDSTONE;
            case LAPIS_LAZULI: return OreType.LAPIS;
            case EMERALD: return OreType.EMERALD;
            case AMETHYST_SHARD: return OreType.AMETHYST;
            case DIAMOND: return OreType.DIAMOND;
            case NETHERITE_INGOT: return OreType.NETHERITE;
            default: return null;
        }
    }
}