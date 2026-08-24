package com.crystallac.check;

import com.crystallac.CrystallAC;
import com.crystallac.check.combat.AimbotCheck;
import com.crystallac.check.combat.AutoclickerCheck;
import com.crystallac.check.combat.KillauraCheck;
import com.crystallac.check.combat.ReachCheck;
import com.crystallac.check.inventory.InventoryMoveCheck;
import com.crystallac.check.movement.FlyCheck;
import com.crystallac.check.movement.SpeedCheck;
import com.crystallac.check.packet.BadPacketsCheck;
import com.crystallac.check.packet.TimerCheck;
import com.crystallac.check.world.FastBreakCheck;
import com.crystallac.check.world.FastPlaceCheck;
import com.crystallac.check.world.ScaffoldCheck;
import com.crystallac.data.PlayerData;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Registry and event dispatcher for all active checks.
 */
public class CheckManager {

    private final Map<CheckType, AbstractCheck> checks = new EnumMap<>(CheckType.class);

    private final KillauraCheck killauraCheck;
    private final ReachCheck reachCheck;
    private final AutoclickerCheck autoclickerCheck;
    private final AimbotCheck aimbotCheck;
    private final SpeedCheck speedCheck;
    private final FlyCheck flyCheck;
    private final TimerCheck timerCheck;
    private final ScaffoldCheck scaffoldCheck;
    private final FastPlaceCheck fastPlaceCheck;
    private final BadPacketsCheck badPacketsCheck;
    private final FastBreakCheck fastBreakCheck;
    private final InventoryMoveCheck inventoryMoveCheck;

    public CheckManager(CrystallAC plugin) {
        this.killauraCheck = new KillauraCheck(plugin);
        this.reachCheck = new ReachCheck(plugin);
        this.autoclickerCheck = new AutoclickerCheck(plugin);
        this.aimbotCheck = new AimbotCheck(plugin);
        this.speedCheck = new SpeedCheck(plugin);
        this.flyCheck = new FlyCheck(plugin);
        this.timerCheck = new TimerCheck(plugin);
        this.scaffoldCheck = new ScaffoldCheck(plugin);
        this.fastPlaceCheck = new FastPlaceCheck(plugin);
        this.badPacketsCheck = new BadPacketsCheck(plugin);
        this.fastBreakCheck = new FastBreakCheck(plugin);
        this.inventoryMoveCheck = new InventoryMoveCheck(plugin);

        register(killauraCheck);
        register(reachCheck);
        register(autoclickerCheck);
        register(aimbotCheck);
        register(speedCheck);
        register(flyCheck);
        register(timerCheck);
        register(scaffoldCheck);
        register(fastPlaceCheck);
        register(badPacketsCheck);
        register(fastBreakCheck);
        register(inventoryMoveCheck);
    }

    private void register(AbstractCheck check) {
        checks.put(check.getCheckType(), check);
    }

    public AbstractCheck getCheck(CheckType type) {
        return checks.get(type);
    }

    public Collection<AbstractCheck> getAllChecks() {
        return Collections.unmodifiableCollection(checks.values());
    }

    // Event Dispatches
    public boolean dispatchAttack(PlayerData data, Entity target) {
        boolean killauraCancel = killauraCheck.handleAttack(data, target);
        boolean reachCancel = reachCheck.handleAttack(data, target);
        return killauraCancel || reachCancel;
    }

    public void dispatchMovement(PlayerData data) {
        speedCheck.handleMovement(data);
        flyCheck.handleMovement(data);
        inventoryMoveCheck.handleMovement(data);
    }

    public void dispatchRotation(PlayerData data) {
        aimbotCheck.handleRotation(data);
        badPacketsCheck.handlePacket(data);
    }

    public void dispatchClick(PlayerData data) {
        autoclickerCheck.handleClick(data);
    }

    public void dispatchFlyingPacket(PlayerData data) {
        timerCheck.handleFlyingPacket(data);
        badPacketsCheck.handlePacket(data);
    }

    public void dispatchBlockPlace(PlayerData data, Block placedBlock, Block placedAgainst, BlockFace face) {
        fastPlaceCheck.handleBlockPlace(data);
        scaffoldCheck.handleBlockPlace(data, placedBlock, placedAgainst, face);
    }

    public void dispatchBlockBreak(PlayerData data, Block block) {
        fastBreakCheck.handleBlockBreak(data, block);
    }
}
