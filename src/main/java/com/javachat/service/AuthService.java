package com.javachat.service;

import com.javachat.dao.UserDAO;
import com.javachat.model.User;

import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Pattern;

public class AuthService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");
    private final UserDAO userDAO = new UserDAO();

    public User register(String username, String password) throws SQLException {
        String normalized = normalizeUsername(username);
        validateUsername(normalized);
        validatePassword(password);
        return userDAO.register(normalized, password);
    }

    public Optional<User> login(String username, String password) throws SQLException {
        String normalized = normalizeUsername(username);
        if (normalized.isBlank() || password == null || password.isEmpty()) return Optional.empty();
        return userDAO.authenticate(normalized, password);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private void validateUsername(String username) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3-30 characters and contain only letters, numbers, or underscores.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        }
        if (password.length() > 72) {
            throw new IllegalArgumentException("Password must be 72 characters or fewer.");
        }
    }
}
