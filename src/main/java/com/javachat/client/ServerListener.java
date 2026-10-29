package com.javachat.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.javachat.controller.ChatController;

import javax.swing.SwingUtilities;

public class ServerListener {
    private final ChatController controller;

    public ServerListener(ChatController controller) {
        this.controller = controller;
    }

    public void onEvent(JsonNode event) {
        SwingUtilities.invokeLater(() -> controller.handleServerEvent(event));
    }
}
