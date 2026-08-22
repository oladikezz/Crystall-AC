package com.crystallac.check;

import com.crystallac.CrystallAC;
import com.crystallac.check.combat.AimbotCheck;
import com.crystallac.check.combat.AutoclickerCheck;
import com.crystallac.check.combat.KillauraCheck;
import com.crystallac.check.combat.ReachCheck;
import com.crystallac.check.movement.FlyCheck;
import com.crystallac.check.movement.SpeedCheck;
import com.crystallac.check.packet.TimerCheck;
import com.crystallac.data.PlayerData;
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

    public CheckManager(CrystallAC plugin) {
        this.killauraCheck = new KillauraCheck(plugin);
        this.reachCheck = new ReachCheck(plugin);
        this.autoclickerCheck = new AutoclickerCheck(plugin);
        this.aimbotCheck = new AimbotCheck(plugin);
        this.speedCheck = new SpeedCheck(plugin);
        this.flyCheck = new FlyCheck(plugin);
        this.timerCheck = new TimerCheck(plugin);

        register(killauraCheck);
        register(reachCheck);
        register(autoclickerCheck);
        register(aimbotCheck);
        register(speedCheck);
        register(flyCheck);
        register(timerCheck);
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
    public void dispatchAttack(PlayerData data, Entity target) {
        killauraCheck.handleAttack(data, target);
        reachCheck.handleAttack(data, target);
    }

    public void dispatchMovement(PlayerData data) {
        speedCheck.handleMovement(data);
        flyCheck.handleMovement(data);
    }

    public void dispatchRotation(PlayerData data) {
        aimbotCheck.handleRotation(data);
    }

    public void dispatchClick(PlayerData data) {
        autoclickerCheck.handleClick(data);
    }

    public void dispatchFlyingPacket(PlayerData data) {
        timerCheck.handleFlyingPacket(data);
    }
}
