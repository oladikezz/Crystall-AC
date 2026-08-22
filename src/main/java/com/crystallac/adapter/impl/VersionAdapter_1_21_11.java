package com.crystallac.adapter.impl;

import com.crystallac.adapter.VersionAdapter;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

/**
 * Version adapter implementation for Minecraft 1.21.11.
 * Supports extended 1.21 sub-series physics and protocol quirks.
 */
public class VersionAdapter_1_21_11 implements VersionAdapter {

    @Override
    public BoundingBox getBoundingBox(Entity entity) {
        if (entity == null) {
            return new BoundingBox(0, 0, 0, 0, 0, 0);
        }
        return entity.getBoundingBox();
    }

    @Override
    public int getPing(Player player) {
        if (player == null) return 0;
        try {
            return player.getPing();
        } catch (NoSuchMethodError | Exception e) {
            return 20;
        }
    }

    @Override
    public boolean isGliding(Player player) {
        return player != null && player.isGliding();
    }

    @Override
    public boolean isRiptiding(Player player) {
        return player != null && player.isRiptiding();
    }

    @Override
    public boolean isSwimming(Player player) {
        return player != null && (player.isSwimming() || player.isInWater());
    }

    @Override
    public boolean isBlockPassable(Block block) {
        if (block == null) return true;
        return block.isPassable();
    }

    @Override
    public String getVersionName() {
        return "1.21.11";
    }

    @Override
    public int getProtocolVersion() {
        return 776; // 1.21.11 designated protocol
    }
}
