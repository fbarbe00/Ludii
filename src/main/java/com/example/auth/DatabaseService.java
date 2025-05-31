package com.example.auth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseService {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/user_auth_db";
    private static final String DB_USER = "app_user";
    private static final String DB_PASSWORD = "app_password";

    public DatabaseService() {
        try {
            Class.forName("org.postgresql.Driver");
            createUsersTableIfNotExists();
            createGameSessionsTableIfNotExists(); // Add this call
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("PostgreSQL JDBC driver not found.");
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public void createUsersTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                     "id SERIAL PRIMARY KEY," +
                     "username VARCHAR(255) UNIQUE NOT NULL," +
                     "password_hash VARCHAR(255) NOT NULL" +
                     ");";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("Users table created or already exists.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error creating users table: " + e.getMessage());
        }
    }

    public void createGameSessionsTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS game_sessions (" +
                "id SERIAL PRIMARY KEY," +
                "game_name VARCHAR(255) NOT NULL," +
                "ludii_game_string TEXT NOT NULL," +
                "current_state_json TEXT," +
                "player1_id INTEGER REFERENCES users(id) NOT NULL," +
                "player2_id INTEGER REFERENCES users(id) NULL," +
                "status VARCHAR(50) NOT NULL," + // WAITING_FOR_PLAYER, IN_PROGRESS, FINISHED_P1_WIN, FINISHED_P2_WIN, DRAW
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("Game sessions table created or already exists.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error creating game_sessions table: " + e.getMessage());
        }
    }


    public User findUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error finding user by username: " + e.getMessage());
        }
        return null;
    }

    public User createUser(String username, String passwordHash) {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?) RETURNING id;";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                return new User(id, username, passwordHash);
            }
        } catch (SQLException e) {
            // Handle specific error for unique constraint violation (username already exists)
            if (e.getSQLState().equals("23505")) { // Unique violation
                System.err.println("Username '" + username + "' already exists.");
                return null; // Or throw a custom exception
            }
            e.printStackTrace();
            System.err.println("Error creating user: " + e.getMessage());
        }
        return null;
    }

    // --- GameSession Methods ---

    public GameSession createGameSession(String gameName, String ludiiGameString, String initialStateJson, int player1Id) {
        String sql = "INSERT INTO game_sessions (game_name, ludii_game_string, current_state_json, player1_id, status) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING id, created_at, updated_at;";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, gameName);
            pstmt.setString(2, ludiiGameString);
            pstmt.setString(3, initialStateJson);
            pstmt.setInt(4, player1Id);
            pstmt.setString(5, "WAITING_FOR_PLAYER");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new GameSession(rs.getInt("id"), gameName, ludiiGameString, initialStateJson, player1Id, null, "WAITING_FOR_PLAYER", rs.getTimestamp("created_at"), rs.getTimestamp("updated_at"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error creating game session: " + e.getMessage());
        }
        return null;
    }

    public GameSession findGameSessionById(int sessionId) {
        String sql = "SELECT * FROM game_sessions WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sessionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Integer player2Id = rs.getObject("player2_id", Integer.class);
                return new GameSession(
                        rs.getInt("id"),
                        rs.getString("game_name"),
                        rs.getString("ludii_game_string"),
                        rs.getString("current_state_json"),
                        rs.getInt("player1_id"),
                        player2Id,
                        rs.getString("status"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error finding game session by ID: " + e.getMessage());
        }
        return null;
    }

    public boolean joinGameSession(int sessionId, int player2Id) {
        String sql = "UPDATE game_sessions SET player2_id = ?, status = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ? AND player1_id != ? AND player2_id IS NULL AND status = 'WAITING_FOR_PLAYER';";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, player2Id);
            pstmt.setString(2, "IN_PROGRESS");
            pstmt.setInt(3, sessionId);
            pstmt.setInt(4, player2Id); // Ensure player2 is not the same as player1

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error joining game session: " + e.getMessage());
        }
        return false;
    }

    public boolean updateGameSessionState(int sessionId, String newStateJson, String status) {
        String sql = "UPDATE game_sessions SET current_state_json = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?;";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStateJson);
            pstmt.setString(2, status);
            pstmt.setInt(3, sessionId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error updating game session state: " + e.getMessage());
        }
        return false;
    }

    public java.util.List<GameSession> getOpenGameSessions() {
        java.util.List<GameSession> openSessions = new java.util.ArrayList<>();
        String sql = "SELECT * FROM game_sessions WHERE status = 'WAITING_FOR_PLAYER' ORDER BY created_at DESC;";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                 Integer player2Id = rs.getObject("player2_id", Integer.class);
                 openSessions.add(new GameSession(
                        rs.getInt("id"),
                        rs.getString("game_name"),
                        rs.getString("ludii_game_string"),
                        rs.getString("current_state_json"),
                        rs.getInt("player1_id"),
                        player2Id,
                        rs.getString("status"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error retrieving open game sessions: " + e.getMessage());
        }
        return openSessions;
    }

}
