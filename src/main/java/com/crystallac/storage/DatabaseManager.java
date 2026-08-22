package com.crystallac.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Manages embedded SQLite database lifecycle for violation auditing and ML ban datasets.
 */
public class DatabaseManager {

    private final File dbFile;
    private final Logger logger;
    private Connection connection;

    public DatabaseManager(File dataFolder, Logger logger) {
        this.logger = logger;
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.dbFile = new File(dataFolder, "data.db");
    }

    public synchronized void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
            logger.info("[CrystallAC] SQLite database initialized successfully at: " + dbFile.getName());
        } catch (Exception e) {
            logger.severe("[CrystallAC] Failed to initialize SQLite database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Violations audit log table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS violations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp BIGINT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    check_type TEXT NOT NULL,
                    vl REAL NOT NULL,
                    details TEXT
                );
            """);

            // Labeled dataset collected from moderator /ac ban actions
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS labeled_bans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp BIGINT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    cheat_type TEXT NOT NULL,
                    banned_by TEXT NOT NULL,
                    feature_vector_json TEXT NOT NULL
                );
            """);
        }
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            }
        } catch (SQLException e) {
            logger.severe("[CrystallAC] Error reconnecting to SQLite: " + e.getMessage());
        }
        return connection;
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.warning("[CrystallAC] Error closing SQLite connection: " + e.getMessage());
        }
    }
}
