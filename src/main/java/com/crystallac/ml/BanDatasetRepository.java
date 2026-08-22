package com.crystallac.ml;

import com.crystallac.storage.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Repository for managing labeled ban examples in SQLite.
 */
public class BanDatasetRepository {

    private final DatabaseManager dbManager;
    private final Logger logger;

    public BanDatasetRepository(DatabaseManager dbManager, Logger logger) {
        this.dbManager = dbManager;
        this.logger = logger;
    }

    public void saveLabeledBan(UUID playerUuid, String playerName, String cheatType, String bannedBy, List<FeatureSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }

        StringBuilder jsonArray = new StringBuilder("[");
        for (int i = 0; i < snapshots.size(); i++) {
            jsonArray.append(snapshots.get(i).toJson());
            if (i < snapshots.size() - 1) jsonArray.append(",");
        }
        jsonArray.append("]");

        long now = System.currentTimeMillis();
        String sql = "INSERT INTO labeled_bans (timestamp, player_uuid, player_name, cheat_type, banned_by, feature_vector_json) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            Connection conn = dbManager.getConnection();
            if (conn != null) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setLong(1, now);
                    pstmt.setString(2, playerUuid.toString());
                    pstmt.setString(3, playerName);
                    pstmt.setString(4, cheatType.toUpperCase());
                    pstmt.setString(5, bannedBy);
                    pstmt.setString(6, jsonArray.toString());
                    pstmt.executeUpdate();
                }
                logger.info(String.format("[CrystallAC] Saved %d feature snapshots for confirmed ban of %s (%s).",
                        snapshots.size(), playerName, cheatType));
            }
        } catch (SQLException e) {
            logger.severe("[CrystallAC] Failed to save labeled ban to database: " + e.getMessage());
        }
    }

    public int getSampleCount(String cheatType) {
        String sql = "SELECT COUNT(*) FROM labeled_bans WHERE cheat_type = ?";
        try {
            Connection conn = dbManager.getConnection();
            if (conn != null) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, cheatType.toUpperCase());
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.warning("[CrystallAC] Error reading sample count from database: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalSampleCount() {
        String sql = "SELECT COUNT(*) FROM labeled_bans";
        try {
            Connection conn = dbManager.getConnection();
            if (conn != null) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            logger.warning("[CrystallAC] Error reading total sample count: " + e.getMessage());
        }
        return 0;
    }
}
