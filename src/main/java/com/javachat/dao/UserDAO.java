package com.javachat.dao;

import com.javachat.model.User;
import com.javachat.util.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {
    public User register(String username, String password) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return new User(keys.getInt(1), username);
            }
            throw new SQLException("Could not retrieve the new user ID.");
        }
    }

    public Optional<User> authenticate(String username, String password) throws SQLException {
        String sql = "SELECT user_id, username, password_hash FROM users WHERE username = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                String hash = rs.getString("password_hash");
                if (!BCrypt.checkpw(password, hash)) return Optional.empty();
                return Optional.of(new User(rs.getInt("user_id"), rs.getString("username")));
            }
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT user_id, username FROM users WHERE username = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(new User(rs.getInt("user_id"), rs.getString("username")));
                return Optional.empty();
            }
        }
    }

    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT user_id, username FROM users WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(new User(rs.getInt("user_id"), rs.getString("username")));
                return Optional.empty();
            }
        }
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT user_id, username FROM users ORDER BY username";
        List<User> users = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new User(rs.getInt("user_id"), rs.getString("username")));
            }
        }
        return users;
    }
}
