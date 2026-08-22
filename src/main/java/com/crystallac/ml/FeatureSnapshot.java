package com.crystallac.ml;

/**
 * Encapsulates a multi-dimensional numerical feature snapshot of player behavior at a given instant.
 */
public class FeatureSnapshot {

    private final long timestamp;
    private final double cps;
    private final double cpsStdDev;
    private final double reachDistance;
    private final double deltaYaw;
    private final double deltaPitch;
    private final double deltaXZ;
    private final double deltaY;
    private final double packetBalanceDrift;

    public FeatureSnapshot(long timestamp,
                           double cps,
                           double cpsStdDev,
                           double reachDistance,
                           double deltaYaw,
                           double deltaPitch,
                           double deltaXZ,
                           double deltaY,
                           double packetBalanceDrift) {
        this.timestamp = timestamp;
        this.cps = cps;
        this.cpsStdDev = cpsStdDev;
        this.reachDistance = reachDistance;
        this.deltaYaw = deltaYaw;
        this.deltaPitch = deltaPitch;
        this.deltaXZ = deltaXZ;
        this.deltaY = deltaY;
        this.packetBalanceDrift = packetBalanceDrift;
    }

    public double[] toArray() {
        return new double[] {
            cps,
            cpsStdDev,
            reachDistance,
            deltaYaw,
            deltaPitch,
            deltaXZ,
            deltaY,
            packetBalanceDrift
        };
    }

    public String toJson() {
        return String.format(
            "{\"time\":%d,\"cps\":%.2f,\"cpsStd\":%.2f,\"reach\":%.2f,\"dYaw\":%.2f,\"dPitch\":%.2f,\"dXZ\":%.3f,\"dY\":%.3f,\"pDrift\":%.2f}",
            timestamp, cps, cpsStdDev, reachDistance, deltaYaw, deltaPitch, deltaXZ, deltaY, packetBalanceDrift
        );
    }

    public long getTimestamp() { return timestamp; }
    public double getCps() { return cps; }
    public double getCpsStdDev() { return cpsStdDev; }
    public double getReachDistance() { return reachDistance; }
    public double getDeltaYaw() { return deltaYaw; }
    public double getDeltaPitch() { return deltaPitch; }
    public double getDeltaXZ() { return deltaXZ; }
    public double getDeltaY() { return deltaY; }
    public double getPacketBalanceDrift() { return packetBalanceDrift; }
}
