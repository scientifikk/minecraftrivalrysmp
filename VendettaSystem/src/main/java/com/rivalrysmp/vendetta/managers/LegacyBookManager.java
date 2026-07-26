package com.rivalrysmp.vendetta.managers;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent log of every notable Vendetta event: rivalry kills, bounty
 * claims, and Reckoning results. Stored via SQLite so it survives restarts.
 */
public class LegacyBookManager {

    private final JavaPlugin plugin;
    private Connection connection;

    public LegacyBookManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupDatabase();
    }

    private void setupDatabase() {
        try {
            String fileName = plugin.getConfig().getString("legacy-book.storage-file", "legacy.db");
            File dbFile = new File(plugin.getDataFolder(), fileName);
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement st = connection.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS legacy_log (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "timestamp TEXT," +
                        "type TEXT," +
                        "actor TEXT," +
                        "target TEXT," +
                        "message TEXT)");
            }
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Failed to set up Legacy Book database: " + e.getMessage());
        }
    }

    public void logEntry(String type, String actor, String target, String message) {
        if (connection == null) return;
        String sql = "INSERT INTO legacy_log (timestamp, type, actor, target, message) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, java.time.LocalDateTime.now().toString());
            ps.setString(2, type);
            ps.setString(3, actor);
            ps.setString(4, target);
            ps.setString(5, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to write Legacy Book entry: " + e.getMessage());
        }
    }

    public List<String> getEntriesFor(String playerName, int limit) {
        List<String> results = new ArrayList<>();
        if (connection == null) return results;
        String sql = "SELECT timestamp, message FROM legacy_log WHERE actor = ? OR target = ? ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ps.setString(2, playerName);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add("[" + rs.getString("timestamp") + "] " + rs.getString("message"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to read Legacy Book: " + e.getMessage());
        }
        return results;
    }

    public List<String> getRecentEntries(int limit) {
        List<String> results = new ArrayList<>();
        if (connection == null) return results;
        String sql = "SELECT timestamp, message FROM legacy_log ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add("[" + rs.getString("timestamp") + "] " + rs.getString("message"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to read Legacy Book: " + e.getMessage());
        }
        return results;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
