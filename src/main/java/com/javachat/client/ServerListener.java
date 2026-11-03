package com.javachat.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.javachat.controller.ChatController;

import java.awt.EventQueue;

public class ServerListener {
    private final ChatController controller;

    public ServerListener(ChatController controller) {
        this.controller = controller;
    }

    public void onEvent(JsonNode event) {
        EventQueue.invokeLater(() -> controller.handleServerEvent(event));
    }
}
