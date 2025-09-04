package hs.orePlugin.Listeners;

import hs.orePlugin.Managers.PlayerDataManager;
import hs.orePlugin.Managers.TrustManager;
import hs.orePlugin.OreType;
import hs.orePlugin.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EntityDamageByEntityListener implements Listener {
    private final PlayerDataManager playerDataManager;
    private final TrustManager trustManager;

    public EntityDamageByEntityListener(PlayerDataManager playerDataManager, TrustManager trustManager) {
        this.playerDataManager = playerDataManager;
        this.trustManager = trustManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        PlayerData attackerData = playerDataManager.getPlayerData(attacker);

        // Trust system - prevent damage between trusted players
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (trustManager.areMutuallyTrusted(attacker, victim)) {
                event.setCancelled(true);
                return;
            }
        }

        // Crystal mode - no knockback for amethyst users
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            PlayerData victimData = playerDataManager.getPlayerData(victim);
            if (victimData.isCrystalModeActive()) {
                // No knockback in crystal mode
                event.setCancelled(true);
                return;
            }
        }

        OreType ore = attackerData.getCurrentOre();
        if (ore == null) return;

        // Apply ore-specific combat effects
        applyCombatEffects(attacker, event, ore, attackerData);

        // Handle special mob damage multipliers
        handleMobDamageMultipliers(event, ore);
    }

    private void applyCombatEffects(Player attacker, EntityDamageByEntityEvent event, OreType ore, PlayerData data) {
        switch (ore) {
            case WOOD:
                // Axes deal 1.5x damage when boost is active
                ItemStack weapon = attacker.getInventory().getItemInMainHand();
                if (data.hasAxeDamageBoostActive() && isAxe(weapon.getType())) {
                    event.setDamage(event.getDamage() * 1.5);
                }
                break;

            case COAL:
                // +1 damage when on fire
                if (attacker.getFireTicks() > 0) {
                    event.setDamage(event.getDamage() + 1);
                }
                break;

            case COPPER:
                // Strike target with lightning when channeling is active
                if (data.hasChannelingActive() && event.getEntity() instanceof LivingEntity) {
                    event.getEntity().getWorld().strikeLightning(event.getEntity().getLocation());
                }
                break;

            case DIAMOND:
                // 2x damage with diamond sword when boost is active
                ItemStack diamondWeapon = attacker.getInventory().getItemInMainHand();
                if (data.hasDiamondDamageActive() && diamondWeapon.getType() == Material.DIAMOND_SWORD) {
                    event.setDamage(event.getDamage() * 2);
                }
                break;

            case AMETHYST:
                // +1.5 attack damage with amethyst shards in offhand
                ItemStack offhand = attacker.getInventory().getItemInOffHand();
                if (offhand.getType() == Material.AMETHYST_SHARD) {
                    event.setDamage(event.getDamage() + 1.5);
                }
                break;

            case REDSTONE:
                // Apply sticky slime effect
                if (data.getTemporaryData("stickySlimeActive") != null &&
                        event.getEntity() instanceof Player) {
                    Player victim = (Player) event.getEntity();
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 200, -10)); // Negative jump boost prevents jumping
                    attacker.sendMessage("§cTarget cannot jump for 10 seconds!");
                    data.removeTemporaryData("stickySlimeActive");
                }
                break;
        }
    }

    private void handleMobDamageMultipliers(EntityDamageByEntityEvent event, OreType ore) {
        if (!(event.getEntity() instanceof Player)) return;

        Entity damager = event.getDamager();

        // Redstone ore - bees and slimes do 5x damage
        if (ore == OreType.REDSTONE) {
            if (damager instanceof Bee || damager instanceof Slime) {
                event.setDamage(event.getDamage() * 5);
            }
        }
    }

    private boolean isAxe(Material material) {
        String name = material.name().toLowerCase();
        return name.contains("axe");
    }
}