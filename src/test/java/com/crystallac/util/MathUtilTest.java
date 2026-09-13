package com.crystallac.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MathUtilTest {

    public static float wrapDegrees(float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    public static double angleDistance(float angle1, float angle2) {
        float diff = Math.abs(wrapDegrees(angle1 - angle2));
        return diff;
    }

    @Test
    public void testWrapDegreesNormalized() {
        assertEquals(0.0F, wrapDegrees(0.0F), 1e-4);
        assertEquals(90.0F, wrapDegrees(90.0F), 1e-4);
        assertEquals(-90.0F, wrapDegrees(-90.0F), 1e-4);
        assertEquals(-170.0F, wrapDegrees(190.0F), 1e-4);
        assertEquals(170.0F, wrapDegrees(-190.0F), 1e-4);
        assertEquals(0.0F, wrapDegrees(720.0F), 1e-4);
    }

    @Test
    public void testAngleDistance() {
        assertEquals(10.0, angleDistance(355.0F, 5.0F), 1e-4);
        assertEquals(180.0, angleDistance(0.0F, 180.0F), 1e-4);
        assertEquals(90.0, angleDistance(45.0F, 135.0F), 1e-4);
    }
}
