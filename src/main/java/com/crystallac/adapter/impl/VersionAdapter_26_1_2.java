package com.crystallac.adapter.impl;

import com.crystallac.adapter.VersionAdapter;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

/**
 * Version adapter implementation for Minecraft 26.1.2.
 * Future-proof protocol and modern Paper entity attributes adaptation.
 */
public class VersionAdapter_26_1_2 implements VersionAdapter {

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
        } catch (Exception e) {
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
        return "26.1.2";
    }

    @Override
    public int getProtocolVersion() {
        return 801; // 26.1.2 protocol
    }
}
