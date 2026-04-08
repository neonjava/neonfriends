package me.neonjavadev.neonfriends.manager;

import me.neonjavadev.neonfriends.NeonFriends;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendManager {

    private final NeonFriends plugin;
    private Connection connection;

    public FriendManager(NeonFriends plugin) throws SQLException, ClassNotFoundException, IOException {
        this.plugin = plugin;
        setupDatabase();
    }

    private void setupDatabase() throws SQLException, ClassNotFoundException, IOException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, "friends.db");
        if (!dbFile.exists()) {
            dbFile.createNewFile();
        }

        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        try (Statement statement = connection.createStatement()) {
            // Table for storing friendship status
            // Status: 0 = Pending, 1 = Accepted, 2 = Rejected
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS friends (" +
                    "player1 UUID NOT NULL, " +
                    "player2 UUID NOT NULL, " +
                    "status INTEGER NOT NULL, " +
                    "PRIMARY KEY (player1, player2))");

            // Table for player names mapping (optional but helpful for offline looks)
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid UUID PRIMARY KEY, " +
                    "name TEXT NOT NULL)");
        }
    }

    public void updatePlayerName(UUID uuid, String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO players (uuid, name) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerName(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    public void sendFriendRequest(UUID requester, UUID receiver) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO friends (player1, player2, status) VALUES (?, ?, 0)")) {
            ps.setString(1, requester.toString());
            ps.setString(2, receiver.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void acceptFriendRequest(UUID requester, UUID receiver) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE friends SET status = 1 WHERE player1 = ? AND player2 = ?")) {
            ps.setString(1, requester.toString());
            ps.setString(2, receiver.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void rejectFriendRequest(UUID requester, UUID receiver) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM friends WHERE player1 = ? AND player2 = ? AND status = 0")) {
            ps.setString(1, requester.toString());
            ps.setString(2, receiver.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeFriend(UUID player1, UUID player2) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM friends WHERE (player1 = ? AND player2 = ?) OR (player1 = ? AND player2 = ?)")) {
            ps.setString(1, player1.toString());
            ps.setString(2, player2.toString());
            ps.setString(3, player2.toString());
            ps.setString(4, player1.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<UUID> getFriends(UUID playerUuid) {
        List<UUID> friends = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player1, player2 FROM friends WHERE (player1 = ? OR player2 = ?) AND status = 1")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String p1 = rs.getString("player1");
                String p2 = rs.getString("player2");
                friends.add(UUID.fromString(p1.equals(playerUuid.toString()) ? p2 : p1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return friends;
    }

    public List<UUID> getPendingRequests(UUID playerUuid) {
        List<UUID> pending = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player1 FROM friends WHERE player2 = ? AND status = 0")) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                pending.add(UUID.fromString(rs.getString("player1")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pending;
    }

    public boolean areFriends(UUID p1, UUID p2) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT status FROM friends WHERE ((player1 = ? AND player2 = ?) OR (player1 = ? AND player2 = ?)) AND status = 1")) {
            ps.setString(1, p1.toString());
            ps.setString(2, p2.toString());
            ps.setString(3, p2.toString());
            ps.setString(4, p1.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean hasPendingRequest(UUID requester, UUID receiver) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT status FROM friends WHERE player1 = ? AND player2 = ? AND status = 0")) {
            ps.setString(1, requester.toString());
            ps.setString(2, receiver.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
