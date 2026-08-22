package com.crystallac.adapter;

import com.crystallac.adapter.impl.VersionAdapter_1_21_11;
import com.crystallac.adapter.impl.VersionAdapter_1_21_4;
import com.crystallac.adapter.impl.VersionAdapter_26_1_2;
import com.crystallac.adapter.impl.VersionAdapter_26_2;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

/**
 * Factory responsible for detecting server version and providing the appropriate VersionAdapter.
 */
public class VersionAdapterFactory {

    private static final Logger LOGGER = Bukkit.getLogger();

    public static VersionAdapter createAdapter() {
        String serverVersion = Bukkit.getBukkitVersion().toLowerCase();
        String fullVersion = Bukkit.getVersion().toLowerCase();

        LOGGER.info("[CrystallAC] Detecting server environment: " + fullVersion + " (Bukkit: " + serverVersion + ")");

        if (serverVersion.contains("26.2") || fullVersion.contains("26.2")) {
            LOGGER.info("[CrystallAC] Bound to VersionAdapter_26_2");
            return new VersionAdapter_26_2();
        } else if (serverVersion.contains("26.1.2") || fullVersion.contains("26.1")) {
            LOGGER.info("[CrystallAC] Bound to VersionAdapter_26_1_2");
            return new VersionAdapter_26_1_2();
        } else if (serverVersion.contains("1.21.11") || fullVersion.contains("1.21.11")) {
            LOGGER.info("[CrystallAC] Bound to VersionAdapter_1_21_11");
            return new VersionAdapter_1_21_11();
        } else if (serverVersion.contains("1.21.4") || fullVersion.contains("1.21.4")) {
            LOGGER.info("[CrystallAC] Bound to VersionAdapter_1_21_4");
            return new VersionAdapter_1_21_4();
        }

        // Fallback for general 1.21+ Paper servers
        LOGGER.warning("[CrystallAC] Exact version not explicitly matched, falling back to universal 1.21.4 adapter.");
        return new VersionAdapter_1_21_4();
    }
}
