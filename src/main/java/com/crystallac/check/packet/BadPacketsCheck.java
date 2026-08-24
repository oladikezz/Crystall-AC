package com.crystallac.check.packet;

import com.crystallac.CrystallAC;
import com.crystallac.check.AbstractCheck;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;

/**
 * Checks for invalid packet structures, exploits, crash payloads, and illegal angles:
 * - Pitch out of vanilla bounds (|pitch| > 90.0 degrees)
 * - NaN / Infinity coordinates
 */
public class BadPacketsCheck extends AbstractCheck {

    public BadPacketsCheck(CrystallAC plugin) {
        super(plugin, CheckType.BADPACKETS);
    }

    public void handlePacket(PlayerData data) {
        if (!isEnabled()) return;

        float pitch = data.getPitch();

        // 1. Impossible Pitch (Derp / Crash / Headless cheats)
        if (Math.abs(pitch) > 90.001f) {
            flag(data, 10.0, String.format("Illegal pitch angle (pitch=%.2f deg)", pitch));
        }

        // 2. NaN / Infinity verification
        double x = data.getDeltaX();
        double y = data.getDeltaY();
        double z = data.getDeltaZ();

        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) ||
            Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
            flag(data, 20.0, "NaN/Infinity position coordinate payload");
        }
    }
}
