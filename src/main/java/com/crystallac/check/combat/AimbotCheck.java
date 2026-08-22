package com.crystallac.check.combat;

import com.crystallac.CrystallAC;
import com.crystallac.check.AbstractCheck;
import com.crystallac.check.CheckType;
import com.crystallac.data.PlayerData;

/**
 * Heuristic check for Aimbot:
 * - Rotational acceleration anomalies
 * - GCD (Greatest Common Divisor) mouse sensitivity quantization analysis
 * - Unnatural linear smoothing and pitch lock
 */
public class AimbotCheck extends AbstractCheck {

    public AimbotCheck(CrystallAC plugin) {
        super(plugin, CheckType.AIMBOT);
    }

    public void handleRotation(PlayerData data) {
        if (!isEnabled() || data.getExemptionManager().isExemptFromCombat(versionAdapter)) {
            return;
        }

        float deltaYaw = data.getDeltaYaw();
        float deltaPitch = data.getDeltaPitch();
        float lastDeltaYaw = data.getLastDeltaYaw();
        float lastDeltaPitch = data.getLastDeltaPitch();

        // 1. Extreme yaw acceleration snap onto target
        double maxYawAccel = getConfiguredThreshold("max_yaw_acceleration", 45.0);
        float yawAcceleration = Math.abs(deltaYaw - lastDeltaYaw);

        if (yawAcceleration > maxYawAccel && data.getLastAttackedEntity() != null) {
            long timeSinceAttack = System.currentTimeMillis() - data.getLastAttackTime();
            if (timeSinceAttack < 300) {
                flag(data, 2.5, String.format("Yaw snap acceleration (accel=%.1f deg/tick, max=%.1f)", yawAcceleration, maxYawAccel));
            }
        }

        // 2. Pitch Lock check (Yaw moves aggressively > 25 degrees while pitch remains perfectly identical 0.0000)
        if (deltaYaw > 25.0f && deltaPitch == 0.0f && lastDeltaPitch == 0.0f) {
            long timeSinceAttack = System.currentTimeMillis() - data.getLastAttackTime();
            if (timeSinceAttack < 400) {
                flag(data, 1.5, String.format("Pitch lock during combat (dYaw=%.1f, dPitch=0.0)", deltaYaw));
            }
        }

        // 3. GCD Sensitivity Divisor Check
        boolean checkGcd = plugin.getConfig().getBoolean("checks.aimbot.check_gcd_sensitivity", true);
        if (checkGcd && deltaPitch > 0.0f && deltaPitch < 20.0f) {
            double gcd = getGcd((long) (deltaPitch * 1000000), (long) (lastDeltaPitch * 1000000));
            // Aimbots generating floating point angles from vector trig often produce tiny GCD remainder (< 1000)
            if (gcd > 0 && gcd < 131072 && deltaPitch > 1.0f && lastDeltaPitch > 1.0f) {
                double modulo = (deltaPitch * 1000000) % 131072;
                if (modulo < 0.0001) {
                    flag(data, 1.0, "Invalid rotation sensitivity quantization");
                }
            }
        }
    }

    private long getGcd(long a, long b) {
        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
}
