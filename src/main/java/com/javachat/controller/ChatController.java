package com.javachat.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.javachat.client.ChatClient;
import com.javachat.client.ServerListener;
import com.javachat.model.User;
import com.javachat.view.ChatFrame;
import com.javachat.view.LoginFrame;

import javax.swing.*;
import java.io.IOException;

public class ChatController {
    private final ChatClient client;
    private LoginFrame loginFrame;
    private ChatFrame chatFrame;
    private User currentUser;
    private int selectedUserId = -1;
    private String selectedUsername = "";

    public ChatController() {
        client = new ChatClient(event -> new ServerListener(this).onEvent(event));
    }

    public void showLogin() {
        loginFrame = new LoginFrame(this);
        loginFrame.setVisible(true);
    }

    public void connectAndLogin(String username, String password, boolean register) {
        if (!client.isConnected()) {
            try {
                String host = System.getenv().getOrDefault("CHAT_HOST", "127.0.0.1");
                int port = Integer.parseInt(System.getenv().getOrDefault("CHAT_PORT", "5050"));
                client.connect(host, port);
            } catch (IOException e) {
                showLoginError("Could not connect to the chat server. Start ChatServer first.\n" + e.getMessage());
                return;
            }
        }
        if (register) client.requestRegister(username, password);
        else client.requestLogin(username, password);
    }

    public void handleServerEvent(JsonNode event) {
        String type = event.path("type").asText("");
        switch (type) {
            case "LOGIN_SUCCESS" -> {
                currentUser = new User(event.path("userId").asInt(), event.path("username").asText());
                client.setCurrentUser(currentUser);
                if (loginFrame != null) loginFrame.dispose();
                chatFrame = new ChatFrame(this, currentUser);
                chatFrame.setVisible(true);
                client.requestUsers();
            }
            case "ERROR" -> showError(event.path("message").asText("Unknown error"));
            case "USERS" -> {
                if (chatFrame != null) chatFrame.updateUsers(event.path("users"));
            }
            case "MESSAGE" -> {
                if (chatFrame != null) chatFrame.receiveMessage(event);
            }
            case "HISTORY" -> {
                if (chatFrame != null) chatFrame.showHistory(event);
            }
            default -> { }
        }
    }

    public void requestUsers() {
        client.requestUsers();
    }

    public void selectUser(int userId, String username) {
        selectedUserId = userId;
        selectedUsername = username;
        if (chatFrame != null) chatFrame.setConversationTitle(username);
        client.requestHistory(userId);
    }

    public void sendMessage(String text) {
        if (selectedUserId < 1) {
            showError("Select a user first.");
            return;
        }
        client.sendPrivateMessage(selectedUserId, text);
    }

    public int getCurrentUserId() {
        return currentUser == null ? -1 : currentUser.id();
    }

    public String getCurrentUsername() {
        return currentUser == null ? "" : currentUser.username();
    }

    private void showLoginError(String message) {
        SwingUtilities.invokeLater(() -> {
            if (loginFrame != null) loginFrame.showError(message);
            else JOptionPane.showMessageDialog(null, message, "JavaChat", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void showError(String message) {
        if (loginFrame != null && loginFrame.isVisible()) {
            loginFrame.showError(message);
        } else {
            JOptionPane.showMessageDialog(chatFrame, message, "JavaChat", JOptionPane.ERROR_MESSAGE);
        }
    }
}
