package com.crystallac.storage;

import com.crystallac.check.CheckType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles persistent asynchronous logging of check violations to log files and the SQLite database.
 */
public class AuditLogger {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private final File logFile;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public AuditLogger(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        File logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        this.logFile = new File(logsDir, "violations.log");
    }

    public void logViolation(Player player, CheckType check, double vl, String details) {
        long now = System.currentTimeMillis();
        String playerName = player.getName();
        String playerUuid = player.getUniqueId().toString();
        String formattedDate = dateFormat.format(new Date(now));

        executor.submit(() -> {
            // 1. Log to text file
            if (plugin.getConfig().getBoolean("settings.log_to_file", true)) {
                try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                    writer.printf("[%s] [VIOLATION] Player: %s (%s) | Check: %s | VL: %.2f | Details: %s%n",
                            formattedDate, playerName, playerUuid, check.getDisplayName(), vl, details);
                } catch (IOException e) {
                    plugin.getLogger().warning("[CrystallAC] Failed to write violation to log file: " + e.getMessage());
                }
            }

            // 2. Log to SQLite
            if (plugin.getConfig().getBoolean("settings.log_to_database", true)) {
                try {
                    Connection conn = dbManager.getConnection();
                    if (conn != null) {
                        String sql = "INSERT INTO violations (timestamp, player_uuid, player_name, check_type, vl, details) VALUES (?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setLong(1, now);
                            pstmt.setString(2, playerUuid);
                            pstmt.setString(3, playerName);
                            pstmt.setString(4, check.name());
                            pstmt.setDouble(5, vl);
                            pstmt.setString(6, details);
                            pstmt.executeUpdate();
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[CrystallAC] Failed to write violation to SQLite: " + e.getMessage());
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
