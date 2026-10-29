package com.javachat.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/java_chat";

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        String url = env("DB_URL", DEFAULT_URL);
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (user == null || user.isBlank()) {
            throw new SQLException("DB_USER is missing. Configure it in your run configuration.");
        }
        if (password == null) {
            throw new SQLException("DB_PASSWORD is missing. Configure it in your run configuration.");
        }

        return DriverManager.getConnection(url, user, password);
    }

    public static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
