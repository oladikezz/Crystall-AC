package com.crystallac.check.packet;

import com.crystallac.CrystallAC;
import com.crystallac.check.AbstractCheck;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;

/**
 * Heuristic check for Timer and Packet Rate Manipulation:
 * Evaluates client packet submission intervals against the server's 50ms tick cadence.
 */
public class TimerCheck extends AbstractCheck {

    public TimerCheck(CrystallAC plugin) {
        super(plugin, CheckType.TIMER);
    }

    public void handleFlyingPacket(PlayerData data) {
        if (!isEnabled() || data.getExemptionManager().isExemptFromMovement(versionAdapter)) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastTime = data.getLastFlyingPacketTime();
        data.setLastFlyingPacketTime(now);

        long interval = now - lastTime;
        if (interval <= 0 || interval > 1000) {
            // Ignore reconnection pauses or time jumps
            return;
        }

        // Expected interval is 50.0ms per tick
        double balance = data.getPacketBalanceMs();
        balance += (50.0 - interval);

        // Cap negative balance to prevent buffering timer exploit
        if (balance < -100.0) {
            balance = -100.0;
        }

        data.setPacketBalanceMs(balance);

        double maxDrift = getConfiguredThreshold("max_balance_drift_ms", 50.0);

        if (balance > maxDrift) {
            flag(data, 2.5, String.format("Packet balance drift (balance=%.1f ms, max=%.1f ms)", balance, maxDrift));
            // Reset balance slightly after flagging to prevent continuous instant flagging
            data.setPacketBalanceMs(maxDrift / 2.0);
        }
    }
}
