package hs.orePlugin;

import org.bukkit.entity.Player;
import java.util.*;

public class PlayerData {
    private UUID playerId;
    private OreType currentOre;
    private Map<OreType, Long> cooldowns;
    private Set<UUID> trustedPlayers;
    private long lastIronDrop;
    private boolean crystalModeActive;
    private long crystalModeEnd;
    private Map<String, Object> temporaryData;

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.cooldowns = new HashMap<>();
        this.trustedPlayers = new HashSet<>();
        this.lastIronDrop = System.currentTimeMillis();
        this.crystalModeActive = false;
        this.crystalModeEnd = 0;
        this.temporaryData = new HashMap<>();
    }

    // Getters and Setters
    public UUID getPlayerId() {
        return playerId;
    }

    public OreType getCurrentOre() {
        return currentOre;
    }

    public void setCurrentOre(OreType oreType) {
        this.currentOre = oreType;
    }

    public boolean isOnCooldown(OreType oreType) {
        Long cooldownEnd = cooldowns.get(oreType);
        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }

    public long getRemainingCooldown(OreType oreType) {
        Long cooldownEnd = cooldowns.get(oreType);
        if (cooldownEnd == null) return 0;
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }

    public void setCooldown(OreType oreType) {
        cooldowns.put(oreType, System.currentTimeMillis() + (oreType.getCooldown() * 1000L));
    }

    public Set<UUID> getTrustedPlayers() {
        return trustedPlayers;
    }

    public void addTrustedPlayer(UUID playerId) {
        trustedPlayers.add(playerId);
    }

    public void removeTrustedPlayer(UUID playerId) {
        trustedPlayers.remove(playerId);
    }

    public boolean isTrusted(UUID playerId) {
        return trustedPlayers.contains(playerId);
    }

    public long getLastIronDrop() {
        return lastIronDrop;
    }

    public void updateIronDropTime() {
        this.lastIronDrop = System.currentTimeMillis();
    }

    public boolean isCrystalModeActive() {
        return crystalModeActive && System.currentTimeMillis() < crystalModeEnd;
    }

    public void setCrystalMode(boolean active, long durationMs) {
        this.crystalModeActive = active;
        if (active) {
            this.crystalModeEnd = System.currentTimeMillis() + durationMs;
        } else {
            this.crystalModeEnd = 0;
        }
    }

    public Map<String, Object> getTemporaryData() {
        return temporaryData;
    }

    public void setTemporaryData(String key, Object value) {
        temporaryData.put(key, value);
    }

    public Object getTemporaryData(String key) {
        return temporaryData.get(key);
    }

    public void removeTemporaryData(String key) {
        temporaryData.remove(key);
    }

    // Helper methods for specific ore effects
    public boolean hasGoldRushActive() {
        Long endTime = (Long) getTemporaryData("goldRushEnd");
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public boolean hasAxeDamageBoostActive() {
        Long endTime = (Long) getTemporaryData("axeDamageEnd");
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public boolean hasChannelingActive() {
        Long endTime = (Long) getTemporaryData("channelingEnd");
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public boolean hasDiamondDamageActive() {
        Long endTime = (Long) getTemporaryData("diamondDamageEnd");
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public boolean hasResistanceActive() {
        Long endTime = (Long) getTemporaryData("resistanceEnd");
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public boolean hasHeartBoostActive() {
        Long endTime = (Long) getTemporaryData("heartBoostEnd");
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public boolean hasBeaconEffectsActive() {
        Long endTime = (Long) getTemporaryData("beaconEffectsEnd");
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public boolean hasExpRegenerationActive() {
        Long endTime = (Long) getTemporaryData("expRegenEnd");
        return endTime != null && System.currentTimeMillis() < endTime;
    }
}