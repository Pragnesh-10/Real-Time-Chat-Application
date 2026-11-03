package com.javachat.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.javachat.controller.ChatController;
import com.javachat.model.User;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ChatFrame extends Frame {
    private final ChatController controller;
    private final User currentUser;
    private final List userList = new List();
    private final Map<String, Integer> usernameToId = new HashMap<>();
    private final Map<String, String> displayNameToUsername = new HashMap<>();
    private final TextArea transcript = new TextArea("", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);
    private final TextField messageField = new TextField();
    private final Label conversationLabel = new Label("Choose a user to start chatting");
    private int selectedUserId = -1;
    private String selectedUsername = "";
    private boolean updatingUsers = false;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public ChatFrame(ChatController controller, User currentUser) {
        this.controller = controller;
        this.currentUser = currentUser;

        setTitle("JavaChat - " + currentUser.username());
        setSize(850, 560);
        setLayout(new BorderLayout());

        transcript.setEditable(false);
        transcript.setFont(new Font("SansSerif", Font.PLAIN, 14));

        userList.addItemListener(e -> {
            if (updatingUsers) return;
            String selected = userList.getSelectedItem();
            if (selected == null) return;

            Integer id = usernameToId.get(selected);
            String username = displayNameToUsername.get(selected);
            if (id != null && username != null && id != currentUser.id()) {
                selectedUserId = id;
                selectedUsername = username;
                conversationLabel.setText("Chat with " + selectedUsername);
                transcript.setText("");
                controller.selectUser(selectedUserId, selectedUsername);
            }
        });

        Button sendButton = new Button("Send");
        sendButton.addActionListener(e -> send());
        messageField.addActionListener(e -> send());

        Panel bottom = new Panel(new BorderLayout(8, 0));
        bottom.add(messageField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        Panel chatPanel = new Panel(new BorderLayout(8, 8));
        conversationLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        chatPanel.add(conversationLabel, BorderLayout.NORTH);
        chatPanel.add(transcript, BorderLayout.CENTER);
        chatPanel.add(bottom, BorderLayout.SOUTH);

        Panel sidebar = new Panel(new BorderLayout(6, 6));
        sidebar.add(new Label("All users (online/offline)"), BorderLayout.NORTH);
        sidebar.add(userList, BorderLayout.CENTER);
        Button refresh = new Button("Refresh");
        refresh.addActionListener(e -> controller.requestUsers());
        sidebar.add(refresh, BorderLayout.SOUTH);
        sidebar.setPreferredSize(new Dimension(210, 0));

        add(sidebar, BorderLayout.WEST);
        add(chatPanel, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });

        setLocationRelativeTo(null);
    }

    public void updateUsers(JsonNode users) {
        String previouslySelected = selectedUsername;
        updatingUsers = true;
        userList.removeAll();
        usernameToId.clear();
        displayNameToUsername.clear();

        for (JsonNode node : users) {
            int id = node.path("userId").asInt();
            String username = node.path("username").asText();
            boolean online = node.path("online").asBoolean(false);
            String displayName = id == currentUser.id()
                    ? username + " (you)"
                    : username + (online ? " (online)" : " (offline)");

            usernameToId.put(displayName, id);
            displayNameToUsername.put(displayName, username);
            userList.add(displayName);
        }

        // Restore the previous conversation without triggering a duplicate selection.
        if (!previouslySelected.isBlank()) {
            for (int i = 0; i < userList.getItemCount(); i++) {
                String displayName = userList.getItem(i);
                if (previouslySelected.equals(displayNameToUsername.get(displayName))) {
                    userList.select(i);
                    break;
                }
            }
        }
        updatingUsers = false;
    }

    public void setConversationTitle(String username) {
        conversationLabel.setText("Chat with " + username);
    }

    public void showHistory(JsonNode event) {
        if (event.path("otherUserId").asInt() != selectedUserId) return;
        transcript.setText("");
        JsonNode messages = event.path("messages");
        for (JsonNode message : messages) appendMessage(message);
        transcript.setCaretPosition(transcript.getText().length());
    }

    public void receiveMessage(JsonNode message) {
        int senderId = message.path("senderId").asInt();
        int recipientId = message.path("recipientId").asInt();
        int otherId = senderId == currentUser.id() ? recipientId : senderId;

        if (otherId == selectedUserId) {
            appendMessage(message);
            transcript.setCaretPosition(transcript.getText().length());
        } else if (recipientId == currentUser.id()) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void appendMessage(JsonNode message) {
        String sender = message.path("senderUsername").asText("unknown");
        String text = message.path("text").asText("");
        String timestamp = message.path("sentAt").asText("");
        String time = timestamp.length() >= 16
                ? timestamp.substring(11, 16)
                : LocalDateTime.now().format(TIME_FORMAT);
        String label = message.path("senderId").asInt() == currentUser.id() ? "You" : sender;
        transcript.append("[" + time + "] " + label + ": " + text + System.lineSeparator());
    }

    private void send() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;
        if (selectedUserId < 1) {
            showMessage("Choose a user first.");
            return;
        }
        controller.sendMessage(text);
        messageField.setText("");
    }

    public void showMessage(String message) {
        Dialog dialog = new Dialog(this, "JavaChat", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.add(new Label(message, Label.CENTER), BorderLayout.CENTER);
        Button ok = new Button("OK");
        ok.addActionListener(e -> dialog.dispose());
        Panel buttons = new Panel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(ok);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setSize(320, 130);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
