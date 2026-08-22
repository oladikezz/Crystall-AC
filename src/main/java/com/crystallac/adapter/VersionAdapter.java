package com.crystallac.adapter;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

/**
 * Common version-independent adapter interface for Minecraft Paper/Spigot platforms.
 * Provides abstraction for NMS / protocol discrepancies across 1.21.4, 1.21.11, 26.1.2, 26.2.
 */
public interface VersionAdapter {

    /**
     * Retrieves the bounding box of an entity.
     * @param entity Target entity
     * @return BoundingBox of the entity
     */
    BoundingBox getBoundingBox(Entity entity);

    /**
     * Gets the network latency (ping) of the player in milliseconds.
     * @param player Target player
     * @return Ping in ms
     */
    int getPing(Player player);

    /**
     * Checks if the player is gliding with an Elytra.
     * @param player Target player
     * @return true if currently gliding
     */
    boolean isGliding(Player player);

    /**
     * Checks if the player is executing a riptide trident boost.
     * @param player Target player
     * @return true if riptiding
     */
    boolean isRiptiding(Player player);

    /**
     * Checks if the player is swimming in water or submerged.
     * @param player Target player
     * @return true if swimming
     */
    boolean isSwimming(Player player);

    /**
     * Checks if a block is solid and collidable.
     * @param block Target block
     * @return true if solid/collidable
     */
    boolean isBlockPassable(Block block);

    /**
     * Returns the human-readable version identifier.
     * @return Version string (e.g., "1.21.4", "26.2")
     */
    String getVersionName();

    /**
     * Returns the Protocol version integer.
     * @return Protocol integer
     */
    int getProtocolVersion();
}
