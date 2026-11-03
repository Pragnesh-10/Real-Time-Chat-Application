package com.javachat;

import com.javachat.controller.ChatController;

import java.awt.EventQueue;

public class App {
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new ChatController().showLogin());
    }
}
