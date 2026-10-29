package com.javachat.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.javachat.controller.ChatController;
import com.javachat.model.User;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ChatFrame extends JFrame {
    private final ChatController controller;
    private final User currentUser;
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(userListModel);
    private final Map<String, Integer> usernameToId = new HashMap<>();
    private final Map<String, String> displayNameToUsername = new HashMap<>();
    private final JTextArea transcript = new JTextArea();
    private final JTextField messageField = new JTextField();
    private final JLabel conversationLabel = new JLabel("Choose a user to start chatting");
    private int selectedUserId = -1;
    private String selectedUsername = "";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public ChatFrame(ChatController controller, User currentUser) {
        this.controller = controller;
        this.currentUser = currentUser;

        setTitle("JavaChat - " + currentUser.username());
        setSize(850, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        transcript.setEditable(false);
        transcript.setLineWrap(true);
        transcript.setWrapStyleWord(true);
        transcript.setFont(new Font("SansSerif", Font.PLAIN, 14));

        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = userList.getSelectedValue();
                if (selected != null) {
                    Integer id = usernameToId.get(selected);
                    String username = displayNameToUsername.get(selected);
                    if (id != null && username != null && id != currentUser.id()) {
                        selectedUserId = id;
                        selectedUsername = username;
                        conversationLabel.setText("Chat with " + selectedUsername);
                        transcript.setText("");
                        controller.selectUser(selectedUserId, selectedUsername);
                    }
                }
            }
        });

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> send());
        messageField.addActionListener(e -> send());

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        bottom.add(messageField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        JPanel chatPanel = new JPanel(new BorderLayout(8, 8));
        chatPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        conversationLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        chatPanel.add(conversationLabel, BorderLayout.NORTH);
        chatPanel.add(new JScrollPane(transcript), BorderLayout.CENTER);
        chatPanel.add(bottom, BorderLayout.SOUTH);

        JPanel sidebar = new JPanel(new BorderLayout(6, 6));
        sidebar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 4));
        sidebar.add(new JLabel("All users (online/offline)"), BorderLayout.NORTH);
        sidebar.add(new JScrollPane(userList), BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> controller.requestUsers());
        sidebar.add(refresh, BorderLayout.SOUTH);
        sidebar.setPreferredSize(new Dimension(210, 0));

        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(chatPanel, BorderLayout.CENTER);
    }

    public void updateUsers(JsonNode users) {
        String previouslySelected = selectedUsername;
        userListModel.clear();
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
            userListModel.addElement(displayName);
        }

        // Keep the selected conversation when the directory refreshes.
        if (!previouslySelected.isBlank()) {
            for (int i = 0; i < userListModel.size(); i++) {
                String displayName = userListModel.get(i);
                if (previouslySelected.equals(displayNameToUsername.get(displayName))) {
                    userList.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    public void setConversationTitle(String username) {
        conversationLabel.setText("Chat with " + username);
    }

    public void showHistory(JsonNode event) {
        if (event.path("otherUserId").asInt() != selectedUserId) return;
        transcript.setText("");
        JsonNode messages = event.path("messages");
        for (JsonNode message : messages) appendMessage(message);
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }

    public void receiveMessage(JsonNode message) {
        int senderId = message.path("senderId").asInt();
        int recipientId = message.path("recipientId").asInt();
        int otherId = senderId == currentUser.id() ? recipientId : senderId;

        if (otherId == selectedUserId) {
            appendMessage(message);
            transcript.setCaretPosition(transcript.getDocument().getLength());
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
        transcript.append("[" + time + "] " + label + ": " + text + "\n");
    }

    private void send() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;
        if (selectedUserId < 1) {
            JOptionPane.showMessageDialog(this, "Choose a user first.");
            return;
        }
        controller.sendMessage(text);
        messageField.setText("");
    }
}
