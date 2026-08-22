package com.crystallac.data;

import com.crystallac.check.CheckType;
import com.crystallac.ml.FeatureBuffer;
import com.crystallac.ml.FeatureSnapshot;
import com.crystallac.statistical.PlayerBaselineTracker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Encapsulates all real-time movement, combat, rotation, packet balance, and statistical baseline state for a player.
 */
public class PlayerData {

    private final UUID uuid;
    private final ExemptionManager exemptionManager;
    private final PlayerBaselineTracker baselineTracker;
    private final FeatureBuffer featureBuffer;

    // Movement & Positions
    private Location from;
    private Location to;
    private double deltaX, deltaY, deltaZ, deltaXZ;
    private double lastDeltaX, lastDeltaY, lastDeltaZ, lastDeltaXZ;
    private boolean clientOnGround;
    private boolean serverOnGround;

    // Rotations
    private float yaw, pitch;
    private float lastYaw, lastPitch;
    private float deltaYaw, deltaPitch;
    private float lastDeltaYaw, lastDeltaPitch;

    // Packet & Timings
    private long lastFlyingPacketTime = System.currentTimeMillis();
    private double packetBalanceMs = 0.0;
    private int flyingPacketsInWindow = 0;
    private long lastTimerWindowReset = System.currentTimeMillis();

    // Combat & Clicks
    private Entity lastAttackedEntity;
    private long lastAttackTime = 0;
    private long lastSwingTime = 0;
    private int attacksThisTick = 0;
    private final Deque<Long> clickTimestamps = new ArrayDeque<>();
    private final List<Long> clickIntervals = new ArrayList<>();

    // Violation Levels
    private final Map<CheckType, Double> violationLevels = new EnumMap<>(CheckType.class);

    public PlayerData(UUID uuid, double ewmaAlpha, int featureBufferSeconds) {
        this.uuid = uuid;
        this.exemptionManager = new ExemptionManager(this);
        this.baselineTracker = new PlayerBaselineTracker(ewmaAlpha);
        this.featureBuffer = new FeatureBuffer(featureBufferSeconds);

        for (CheckType type : CheckType.values()) {
            violationLevels.put(type, 0.0);
        }
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public UUID getUuid() {
        return uuid;
    }

    public ExemptionManager getExemptionManager() {
        return exemptionManager;
    }

    public PlayerBaselineTracker getBaselineTracker() {
        return baselineTracker;
    }

    public FeatureBuffer getFeatureBuffer() {
        return featureBuffer;
    }

    public void updateMovement(Location newTo, boolean onGround) {
        this.from = (this.to != null) ? this.to.clone() : newTo.clone();
        this.to = newTo.clone();

        this.lastDeltaX = this.deltaX;
        this.lastDeltaY = this.deltaY;
        this.lastDeltaZ = this.deltaZ;
        this.lastDeltaXZ = this.deltaXZ;

        this.deltaX = to.getX() - from.getX();
        this.deltaY = to.getY() - from.getY();
        this.deltaZ = to.getZ() - from.getZ();
        this.deltaXZ = Math.hypot(deltaX, deltaZ);

        this.clientOnGround = onGround;
        this.serverOnGround = to.clone().subtract(0, 0.1, 0).getBlock().getType().isSolid();

        // Rotations
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
        this.yaw = to.getYaw();
        this.pitch = to.getPitch();

        this.lastDeltaYaw = this.deltaYaw;
        this.lastDeltaPitch = this.deltaPitch;
        this.deltaYaw = Math.abs(yaw - lastYaw);
        this.deltaPitch = Math.abs(pitch - lastPitch);

        // Update baseline tracker
        if (deltaXZ > 0.05) {
            baselineTracker.recordMovementSpeed(deltaXZ);
        }
        if (deltaYaw > 0.1 || deltaPitch > 0.1) {
            baselineTracker.recordRotationSample(Math.hypot(deltaYaw, deltaPitch));
        }

        // Record feature snapshot periodically
        captureSnapshot(0.0);
    }

    public synchronized void recordClick() {
        long now = System.currentTimeMillis();
        if (!clickTimestamps.isEmpty()) {
            long delay = now - clickTimestamps.peekLast();
            if (delay > 0 && delay < 1000) {
                clickIntervals.add(delay);
                if (clickIntervals.size() > 50) {
                    clickIntervals.remove(0);
                }
            }
        }
        clickTimestamps.addLast(now);

        // Purge clicks older than 1 second
        while (!clickTimestamps.isEmpty() && (now - clickTimestamps.peekFirst()) > 1000) {
            clickTimestamps.pollFirst();
        }

        double currentCps = getCps();
        double currentStdDev = getClickIntervalStdDev();
        baselineTracker.recordCpsSample(currentCps, currentStdDev);
    }

    public synchronized double getCps() {
        long now = System.currentTimeMillis();
        while (!clickTimestamps.isEmpty() && (now - clickTimestamps.peekFirst()) > 1000) {
            clickTimestamps.pollFirst();
        }
        return clickTimestamps.size();
    }

    public synchronized double getClickIntervalStdDev() {
        if (clickIntervals.size() < 4) return 15.0; // Default human variance

        double sum = 0.0;
        for (long val : clickIntervals) {
            sum += val;
        }
        double mean = sum / clickIntervals.size();

        double sumSq = 0.0;
        for (long val : clickIntervals) {
            sumSq += Math.pow(val - mean, 2);
        }
        return Math.sqrt(sumSq / (clickIntervals.size() - 1));
    }

    public void captureSnapshot(double reach) {
        FeatureSnapshot snapshot = new FeatureSnapshot(
            System.currentTimeMillis(),
            getCps(),
            getClickIntervalStdDev(),
            reach,
            deltaYaw,
            deltaPitch,
            deltaXZ,
            deltaY,
            packetBalanceMs
        );
        featureBuffer.addSnapshot(snapshot);
    }

    // Violation level management
    public double getVL(CheckType type) {
        return violationLevels.getOrDefault(type, 0.0);
    }

    public void setVL(CheckType type, double vl) {
        violationLevels.put(type, Math.max(0.0, vl));
    }

    public void addVL(CheckType type, double amount) {
        setVL(type, getVL(type) + amount);
    }

    public void decayVL(CheckType type, double amount) {
        setVL(type, Math.max(0.0, getVL(type) - amount));
    }

    public double getTotalVL() {
        return violationLevels.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    // Getters and Setters for combat/movement state
    public Location getFrom() { return from; }
    public Location getTo() { return to; }
    public double getDeltaX() { return deltaX; }
    public double getDeltaY() { return deltaY; }
    public double getDeltaZ() { return deltaZ; }
    public double getDeltaXZ() { return deltaXZ; }
    public double getLastDeltaXZ() { return lastDeltaXZ; }
    public double getLastDeltaY() { return lastDeltaY; }
    public boolean isClientOnGround() { return clientOnGround; }
    public boolean isServerOnGround() { return serverOnGround; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getLastYaw() { return lastYaw; }
    public float getLastPitch() { return lastPitch; }
    public float getDeltaYaw() { return deltaYaw; }
    public float getDeltaPitch() { return deltaPitch; }
    public float getLastDeltaYaw() { return lastDeltaYaw; }
    public float getLastDeltaPitch() { return lastDeltaPitch; }

    public long getLastFlyingPacketTime() { return lastFlyingPacketTime; }
    public void setLastFlyingPacketTime(long time) { this.lastFlyingPacketTime = time; }
    public double getPacketBalanceMs() { return packetBalanceMs; }
    public void setPacketBalanceMs(double balance) { this.packetBalanceMs = balance; }
    public int getFlyingPacketsInWindow() { return flyingPacketsInWindow; }
    public void setFlyingPacketsInWindow(int count) { this.flyingPacketsInWindow = count; }
    public long getLastTimerWindowReset() { return lastTimerWindowReset; }
    public void setLastTimerWindowReset(long time) { this.lastTimerWindowReset = time; }

    public Entity getLastAttackedEntity() { return lastAttackedEntity; }
    public void setLastAttackedEntity(Entity lastAttackedEntity) { this.lastAttackedEntity = lastAttackedEntity; }
    public long getLastAttackTime() { return lastAttackTime; }
    public void setLastAttackTime(long lastAttackTime) { this.lastAttackTime = lastAttackTime; }
    public long getLastSwingTime() { return lastSwingTime; }
    public void setLastSwingTime(long lastSwingTime) { this.lastSwingTime = lastSwingTime; }
    public int getAttacksThisTick() { return attacksThisTick; }
    public void setAttacksThisTick(int attacksThisTick) { this.attacksThisTick = attacksThisTick; }
}
