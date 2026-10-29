package com.javachat.dao;

import com.javachat.model.ChatMessage;
import com.javachat.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    public ChatMessage save(int senderId, String senderName, int recipientId,
                            String recipientName, String text) throws SQLException {
        String sql = "INSERT INTO messages (sender_id, recipient_id, message_text) VALUES (?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, senderId);
            ps.setInt(2, recipientId);
            ps.setString(3, text);
            ps.executeUpdate();

            long id = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) id = keys.getLong(1);
            }
            return new ChatMessage(id, senderId, senderName, recipientId, recipientName,
                    text, LocalDateTime.now());
        }
    }

    public List<ChatMessage> history(int userA, int userB, int limit) throws SQLException {
        String sql = """
            SELECT m.message_id, m.sender_id, su.username AS sender_username,
                   m.recipient_id, ru.username AS recipient_username,
                   m.message_text, m.sent_at
            FROM messages m
            JOIN users su ON su.user_id = m.sender_id
            JOIN users ru ON ru.user_id = m.recipient_id
            WHERE (m.sender_id = ? AND m.recipient_id = ?)
               OR (m.sender_id = ? AND m.recipient_id = ?)
            ORDER BY m.sent_at DESC, m.message_id DESC
            LIMIT ?
            """;

        List<ChatMessage> result = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userA);
            ps.setInt(2, userB);
            ps.setInt(3, userB);
            ps.setInt(4, userA);
            ps.setInt(5, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("sent_at");
                    LocalDateTime sentAt = ts == null ? LocalDateTime.now() : ts.toLocalDateTime();
                    result.add(new ChatMessage(
                            rs.getLong("message_id"),
                            rs.getInt("sender_id"),
                            rs.getString("sender_username"),
                            rs.getInt("recipient_id"),
                            rs.getString("recipient_username"),
                            rs.getString("message_text"),
                            sentAt
                    ));
                }
            }
        }
        java.util.Collections.reverse(result);
        return result;
    }
}
