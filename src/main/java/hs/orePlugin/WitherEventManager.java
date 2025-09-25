package hs.orePlugin;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;

import java.util.*;

public class WitherEventManager implements Listener {

    private final OreAbilitiesPlugin plugin;
    private boolean eventActive = false;
    private Wither eventWither = null;
    private Player witherOreHolder = null;
    private int timeRemaining = 600; // 10 minutes in seconds
    private Location arenaCenter = null;
    private Location spawnPortalLocation = null;
    private List<Player> eventParticipants = new ArrayList<>();
    private Map<Player, Location> originalLocations = new HashMap<>();
    private Set<UUID> piglinOreRecipients = new HashSet<>();

    // Timers
    private BukkitTask eventTimer = null;
    private BukkitTask piglinSpawnTimer = null;
    private BukkitTask scoreboardUpdateTimer = null;

    public WitherEventManager(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean startWitherEvent(Player initiator, Location arenaLocation) {
        if (eventActive) {
            initiator.sendMessage("§cWither event is already active!");
            return false;
        }

        // Get all online players
        eventParticipants.clear();
        originalLocations.clear();
        piglinOreRecipients.clear();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            eventParticipants.add(player);
            originalLocations.put(player, player.getLocation().clone());
        }

        if (eventParticipants.isEmpty()) {
            initiator.sendMessage("§cNo players online for the event!");
            return false;
        }

        eventActive = true;
        OreConfigs configs = plugin.getOreConfigs();
        timeRemaining = configs != null ? configs.getWitherEventDuration() : 600;
        arenaCenter = arenaLocation;

        // Teleport all players
        teleportPlayersToArena();

        // Spawn wither with configured health
        spawnEventWither();

        // Start timers
        startEventTimers();

        // Setup scoreboard
        setupEventScoreboard();

        // Announce event
        announceEventStart();

        plugin.getLogger().info("Wither Event started by " + initiator.getName());
        return true;
    }

    private void announceEventStart() {
        for (Player player : eventParticipants) {
            player.sendTitle("§4⚔ WITHER EVENT ⚔", "§cDefeat the 500 HP Wither!", 10, 70, 20);
            player.sendMessage("");
            player.sendMessage("§4§l════════════════════════════════");
            player.sendMessage("§c§l         WITHER EVENT STARTED!");
            player.sendMessage("");
            player.sendMessage("§e⚔ Objective: §fDefeat the 500 HP Wither");
            player.sendMessage("§e⏰ Time Limit: §f10 minutes");
            player.sendMessage("§e🎁 Rewards:");
            player.sendMessage("  §8➤ §4Wither Ore §7- Kill the Wither");
            player.sendMessage("  §8➤ §6Piglin Ore §7- Automatic on Wither death");
            player.sendMessage("");
            player.sendMessage("§c⚠ Warning: §fPiglins spawn every 20 seconds!");
            player.sendMessage("§c⚠ Warning: §fWither Ore transfers on death!");
            player.sendMessage("§4§l════════════════════════════════");
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }
    }

    private void teleportPlayersToArena() {
        int playerCount = eventParticipants.size();
        double angleStep = 2 * Math.PI / playerCount;
        int radius = 20;

        for (int i = 0; i < eventParticipants.size(); i++) {
            Player player = eventParticipants.get(i);
            double angle = i * angleStep;

            double x = arenaCenter.getX() + radius * Math.cos(angle);
            double z = arenaCenter.getZ() + radius * Math.sin(angle);

            Location teleportLoc = new Location(arenaCenter.getWorld(), x, arenaCenter.getY(), z);
            teleportLoc.setYaw((float) Math.toDegrees(angle + Math.PI));

            player.teleport(teleportLoc);
        }
    }

    private void spawnEventWither() {
        OreConfigs configs = plugin.getOreConfigs();
        int witherHealth = configs != null ? configs.getWitherEventHealth() : 500;

        Location witherSpawn = arenaCenter.clone().add(0, 10, 0);
        eventWither = (Wither) arenaCenter.getWorld().spawnEntity(witherSpawn, EntityType.WITHER);

        // Set health to configured amount
        eventWither.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(witherHealth);
        eventWither.setHealth(witherHealth);

        eventWither.setCustomName("§4Event Wither §c[" + witherHealth + "❤]");
        eventWither.setCustomNameVisible(true);

        // Make wither more aggressive
        if (!eventParticipants.isEmpty()) {
            eventWither.setTarget(eventParticipants.get(0));
        }
    }

    private void startEventTimers() {
        // Main event countdown timer
        eventTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventActive) {
                    cancel();
                    return;
                }

                timeRemaining--;

                if (timeRemaining <= 0) {
                    endEventTimeout();
                    return;
                }

                // Announce time milestones
                if (timeRemaining == 300) { // 5 minutes left
                    announceToParticipants("§e⚠ 5 minutes remaining in Wither Event!");
                } else if (timeRemaining == 60) { // 1 minute left
                    announceToParticipants("§c⚠ 1 minute remaining in Wither Event!");
                } else if (timeRemaining <= 10 && timeRemaining > 0) { // Countdown
                    announceToParticipants("§4⚠ " + timeRemaining + " seconds left!");
                }

                updateWitherHealth();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Piglin spawn timer
        OreConfigs configs = plugin.getOreConfigs();
        int spawnInterval = configs != null ? configs.getPiglinSpawnInterval() : 20;

        piglinSpawnTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventActive || eventWither == null || eventWither.isDead()) {
                    cancel();
                    return;
                }

                spawnEventPiglins();
            }
        }.runTaskTimer(plugin, spawnInterval * 20L, spawnInterval * 20L);

        // Scoreboard update timer
        scoreboardUpdateTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventActive) {
                    cancel();
                    return;
                }
                updateScoreboards();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void spawnEventPiglins() {
        if (eventWither == null || eventWither.isDead()) return;

        Location witherLoc = eventWither.getLocation();

        // Spawn 2 regular piglins
        for (int i = 0; i < 2; i++) {
            Location spawnLoc = getRandomSpawnLocation(witherLoc);
            Piglin piglin = (Piglin) witherLoc.getWorld().spawnEntity(spawnLoc, EntityType.PIGLIN);
            piglin.setAdult();
            // Set aggressive behavior without using setAngry
            piglin.setImmuneToZombification(true);
            if (!eventParticipants.isEmpty()) {
                piglin.setTarget(eventParticipants.get(new Random().nextInt(eventParticipants.size())));
            }
            piglin.getEquipment().setItemInMainHand(new ItemStack(Material.GOLDEN_SWORD));
        }

        // Spawn 1 piglin brute
        Location bruteLoc = getRandomSpawnLocation(witherLoc);
        PiglinBrute brute = (PiglinBrute) witherLoc.getWorld().spawnEntity(bruteLoc, EntityType.PIGLIN_BRUTE);
        brute.setAdult();
        brute.setImmuneToZombification(true);
        if (!eventParticipants.isEmpty()) {
            brute.setTarget(eventParticipants.get(new Random().nextInt(eventParticipants.size())));
        }
        brute.getEquipment().setItemInMainHand(new ItemStack(Material.GOLDEN_AXE));

        announceToParticipants("§6⚔ Piglins have spawned near the Wither!");
    }

    private Location getRandomSpawnLocation(Location center) {
        double angle = Math.random() * 2 * Math.PI;
        double distance = 8 + Math.random() * 12; // 8-20 blocks away

        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);

        return new Location(center.getWorld(), x, center.getY(), z);
    }

    @EventHandler
    public void onWitherDeath(EntityDeathEvent event) {
        if (!eventActive || event.getEntity() != eventWither) return;

        Player killer = eventWither.getKiller();
        if (killer != null) {
            // Give Wither ore to killer
            giveWitherOre(killer);

            // Give Piglin ore to the killer only (one-time reward)
            if (!piglinOreRecipients.contains(killer.getUniqueId())) {
                givePiglinOre(killer);
                piglinOreRecipients.add(killer.getUniqueId());
                // Only announce to the player who got it
                killer.sendMessage("§6§l✓ You received Piglin Ore as a bonus reward!");
            }

            announceToParticipants("§a✓ " + killer.getName() + " §ahas defeated the Event Wither!");
        }

        // Stop piglin spawning
        if (piglinSpawnTimer != null) {
            piglinSpawnTimer.cancel();
            piglinSpawnTimer = null;
        }

        announceToParticipants("§7Piglin spawning has stopped.");

        // Create portal after 5 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                createSpawnPortal();
            }
        }.runTaskLater(plugin, 100L);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!eventActive || !eventParticipants.contains(event.getEntity())) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Transfer Wither ore if the victim had it
        if (witherOreHolder != null && witherOreHolder.equals(victim) && killer != null) {
            transferWitherOre(victim, killer);
        }
    }

    @EventHandler
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!eventActive || !(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        // Check if victim has wither ore and would die from this damage
        if (witherOreHolder != null && witherOreHolder.equals(victim)) {
            // Schedule check for death after damage is applied
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (victim.isDead() || victim.getHealth() <= 0) {
                        transferWitherOre(victim, damager);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    private void giveWitherOre(Player player) {
        witherOreHolder = player;

        // Clean up old ore effects and set new ore
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oldOre = dataManager.getPlayerOre(player);
        if (oldOre != null) {
            plugin.getPlayerListener().cleanupPlayerEffects(player, oldOre);
        }

        dataManager.setPlayerOre(player, OreType.WITHER);

        // Apply wither ore effects
        plugin.getAbilityListener().applyAllOreTypeEffectsFixed(player, OreType.WITHER);

        announceToParticipants("§4⚔ " + player.getName() + " §4now holds the Wither Ore!");
    }

    private void givePiglinOre(Player player) {
        // Clean up old ore effects and set new ore
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oldOre = dataManager.getPlayerOre(player);

        // Only give piglin ore if they don't have wither ore
        if (oldOre != OreType.WITHER) {
            if (oldOre != null) {
                plugin.getPlayerListener().cleanupPlayerEffects(player, oldOre);
            }

            dataManager.setPlayerOre(player, OreType.PIGLIN);
            plugin.getAbilityListener().applyAllOreTypeEffectsFixed(player, OreType.PIGLIN);
        }
    }

    private void transferWitherOre(Player from, Player to) {
        witherOreHolder = to;

        // Transfer ore without resetting timer
        PlayerDataManager dataManager = plugin.getPlayerDataManager();
        OreType oldOre = dataManager.getPlayerOre(to);
        if (oldOre != null) {
            plugin.getPlayerListener().cleanupPlayerEffects(to, oldOre);
        }

        dataManager.setPlayerOre(to, OreType.WITHER);
        plugin.getAbilityListener().applyAllOreTypeEffectsFixed(to, OreType.WITHER);

        announceToParticipants("§4⚔ " + to.getName() + " §4killed the Wither Ore holder and claimed it!");
    }

    private void createSpawnPortal() {
        // Create portal 10 blocks away from center
        Location portalLoc = arenaCenter.clone().add(0, 0, 15);
        spawnPortalLocation = portalLoc;

        // Create portal frame
        World world = portalLoc.getWorld();

        // Create a 3x3 portal frame
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                Location frameLoc = portalLoc.clone().add(x, y, 0);
                if (x == -1 || x == 1 || y == 0 || y == 2) {
                    world.getBlockAt(frameLoc).setType(Material.OBSIDIAN);
                } else {
                    world.getBlockAt(frameLoc).setType(Material.NETHER_PORTAL);
                }
            }
        }

        announceToParticipants("§a✓ Spawn portal created! Step through to return home!");

        // Start portal teleport checker
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventActive) {
                    cancel();
                    return;
                }

                checkPortalTeleports();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void checkPortalTeleports() {
        if (spawnPortalLocation == null) return;

        Iterator<Player> iterator = eventParticipants.iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            if (player.getLocation().distance(spawnPortalLocation) < 2) {
                teleportToSpawn(player);
                iterator.remove();

                // Check if all players have left
                if (eventParticipants.isEmpty()) {
                    endEvent();
                }
            }
        }
    }

    private void teleportToSpawn(Player player) {
        Location originalLoc = originalLocations.get(player);
        if (originalLoc != null) {
            player.teleport(originalLoc);
        } else {
            player.teleport(player.getWorld().getSpawnLocation());
        }

        player.sendMessage("§a✓ Returned to original location!");
    }

    private void endEventTimeout() {
        announceToParticipants("§c⚠ Wither Event timed out! Portal opening...");

        // Kill the wither if still alive
        if (eventWither != null && !eventWither.isDead()) {
            eventWither.remove();
        }

        createSpawnPortal();

        // Force end after 30 more seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                endEvent();
            }
        }.runTaskLater(plugin, 600L);
    }

    private void endEvent() {
        eventActive = false;

        // Teleport remaining players
        for (Player player : eventParticipants) {
            teleportToSpawn(player);
        }

        // Cancel all timers
        if (eventTimer != null) {
            eventTimer.cancel();
            eventTimer = null;
        }
        if (piglinSpawnTimer != null) {
            piglinSpawnTimer.cancel();
            piglinSpawnTimer = null;
        }
        if (scoreboardUpdateTimer != null) {
            scoreboardUpdateTimer.cancel();
            scoreboardUpdateTimer = null;
        }

        // Clear scoreboard
        clearEventScoreboard();

        // Clean up portal
        if (spawnPortalLocation != null) {
            World world = spawnPortalLocation.getWorld();
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 2; y++) {
                    Location frameLoc = spawnPortalLocation.clone().add(x, y, 0);
                    world.getBlockAt(frameLoc).setType(Material.AIR);
                }
            }
        }

        // Reset variables
        eventParticipants.clear();
        originalLocations.clear();
        piglinOreRecipients.clear();
        witherOreHolder = null;
        eventWither = null;
        spawnPortalLocation = null;

        plugin.getLogger().info("Wither Event ended.");
    }

    private void setupEventScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();

        for (Player player : eventParticipants) {
            Scoreboard scoreboard = manager.getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective("witherEvent", "dummy", "§4⚔ WITHER EVENT ⚔");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            player.setScoreboard(scoreboard);
        }
    }

    private void updateScoreboards() {
        String timeFormatted = formatTime(timeRemaining);
        String witherHolderName = witherOreHolder != null ? witherOreHolder.getName() : "None";

        for (Player player : eventParticipants) {
            Scoreboard scoreboard = player.getScoreboard();
            Objective objective = scoreboard.getObjective("witherEvent");

            if (objective != null) {
                // Clear old scores
                for (String entry : scoreboard.getEntries()) {
                    scoreboard.resetScores(entry);
                }

                // Add new scores
                objective.getScore("§c").setScore(10);
                objective.getScore("§4Wither Ore Holder:").setScore(9);
                objective.getScore("§f" + witherHolderName).setScore(8);
                objective.getScore("§b").setScore(7);
                objective.getScore("§eTime Remaining:").setScore(6);
                objective.getScore("§a" + timeFormatted).setScore(5);

                if (eventWither != null && !eventWither.isDead()) {
                    objective.getScore("§d").setScore(4);
                    objective.getScore("§4Wither Health:").setScore(3);
                    int health = (int) Math.ceil(eventWither.getHealth());
                    int maxHealth = (int) eventWither.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    objective.getScore("§c" + health + "/" + maxHealth + "❤").setScore(2);
                }
            }
        }
    }

    private void updateWitherHealth() {
        if (eventWither != null && !eventWither.isDead()) {
            int health = (int) Math.ceil(eventWither.getHealth());
            int maxHealth = (int) eventWither.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            eventWither.setCustomName("§4Event Wither §c[" + health + "/" + maxHealth + "❤]");
        }
    }

    private void clearEventScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(manager.getMainScoreboard());
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    private void announceToParticipants(String message) {
        for (Player player : eventParticipants) {
            player.sendMessage(message);
        }
    }

    public boolean isEventActive() {
        return eventActive;
    }

    public void cleanup() {
        if (eventActive) {
            endEvent();
        }
    }
}