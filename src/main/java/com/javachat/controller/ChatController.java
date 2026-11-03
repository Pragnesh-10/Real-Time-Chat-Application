package com.javachat.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.javachat.client.ChatClient;
import com.javachat.client.ServerListener;
import com.javachat.model.User;
import com.javachat.view.ChatFrame;
import com.javachat.view.LoginFrame;

import java.awt.EventQueue;
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

    public void connectAndRegisterOrLogin(String username, String email, String password, boolean register) {
        if (!client.isConnected()) {
            try {
                String host = System.getenv().getOrDefault("CHAT_HOST", "127.0.0.1");
                int port = Integer.parseInt(System.getenv().getOrDefault("CHAT_PORT", "5050"));
                client.connect(host, port);
            } catch (IOException | NumberFormatException e) {
                showLoginError("Could not connect to the chat server. Check the server and CHAT_PORT.");
                return;
            }
        }

        if (register) client.requestRegister(username, email, password);
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
        EventQueue.invokeLater(() -> {
            if (loginFrame != null) loginFrame.showError(message);
            else showDialog(null, message);
        });
    }

    private void showError(String message) {
        EventQueue.invokeLater(() -> {
            if (loginFrame != null && loginFrame.isVisible()) {
                loginFrame.showError(message);
            } else if (chatFrame != null) {
                chatFrame.showMessage(message);
            }
        });
    }

    private void showDialog(java.awt.Frame owner, String message) {
        java.awt.Dialog dialog = new java.awt.Dialog(owner, "JavaChat", true);
        dialog.setLayout(new java.awt.BorderLayout(10, 10));
        dialog.add(new java.awt.Label(message, java.awt.Label.CENTER), java.awt.BorderLayout.CENTER);
        java.awt.Button ok = new java.awt.Button("OK");
        ok.addActionListener(e -> dialog.dispose());
        java.awt.Panel buttons = new java.awt.Panel(new java.awt.FlowLayout());
        buttons.add(ok);
        dialog.add(buttons, java.awt.BorderLayout.SOUTH);
        dialog.setSize(360, 140);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
