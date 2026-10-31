package com.javachat.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.javachat.dao.MessageDAO;
import com.javachat.dao.UserDAO;
import com.javachat.model.ChatMessage;
import com.javachat.model.User;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private final ObjectMapper mapper = new ObjectMapper();
    private final UserDAO userDAO = new UserDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private BufferedReader in;
    private BufferedWriter out;
    private final Object writeLock = new Object();
    private volatile User user;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (socket) {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.length() > 20_000) {
                    sendError("Request is too large.");
                    continue;
                }
                try {
                    handle(mapper.readTree(line));
                } catch (Exception e) {
                    sendError("Invalid request.");
                }
            }
        } catch (IOException ignored) {
            // Client disconnected.
        } finally {
            user = null;
            server.remove(this);
        }
    }

    private void handle(JsonNode request) throws SQLException {
        String type = text(request, "type");
        switch (type) {
            case "REGISTER" -> register(request);
            case "LOGIN" -> login(request);
            case "USERS" -> {
                requireLogin();
                server.broadcastUsers();
            }
            case "HISTORY" -> history(request);
            case "MESSAGE" -> message(request);
            default -> sendError("Unknown request type.");
        }
    }

    private void register(JsonNode request) throws SQLException {
        if (user != null) {
            sendError("You are already logged in.");
            return;
        }

        String username = text(request, "username").trim();
        String email = text(request, "email").trim();
        String password = text(request, "password");

        if (!username.matches("^[A-Za-z0-9_]{3,30}$")) {
            sendError("Username must be 3-30 characters: letters, numbers, underscores.");
            return;
        }
        if (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            sendError("Enter a valid email address.");
            return;
        }
        if (password.length() < 8 || password.length() > 72) {
            sendError("Password must be between 8 and 72 characters.");
            return;
        }

        try {
            User created = userDAO.register(username, email, password);
            user = created;
            sendLoginSuccess(created);
            server.broadcastUsers();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                sendError("That username or email address is already registered.");
            } else {
                throw e;
            }
        }
    }

    private void login(JsonNode request) throws SQLException {
        if (user != null) {
            sendError("You are already logged in.");
            return;
        }
        String username = text(request, "username").trim();
        String password = text(request, "password");
        Optional<User> result = userDAO.authenticate(username, password);
        if (result.isEmpty()) {
            sendError("Incorrect username or password.");
            return;
        }

        User found = result.get();
        // Prevent simultaneous sessions for the same user in this simple version.
        boolean alreadyOnline = serverUserExists(found.id());
        if (alreadyOnline) {
            sendError("This user is already connected.");
            return;
        }

        user = found;
        sendLoginSuccess(found);
        server.broadcastUsers();
    }

    private boolean serverUserExists(int id) {
        return server.isUserOnline(id);
    }

    private void sendLoginSuccess(User found) {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "LOGIN_SUCCESS");
        response.put("userId", found.id());
        response.put("username", found.username());
        send(response);
    }

    private void history(JsonNode request) throws SQLException {
        if (!requireLogin()) return;
        int otherId = request.path("otherUserId").asInt(-1);
        if (otherId < 1 || otherId == user.id()) {
            sendError("Select a valid user to open a conversation.");
            return;
        }
        Optional<User> other = userDAO.findById(otherId);
        if (other.isEmpty()) {
            sendError("User does not exist.");
            return;
        }
        List<ChatMessage> messages = messageDAO.history(user.id(), otherId, 200);
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "HISTORY");
        response.put("otherUserId", otherId);
        ArrayNode array = response.putArray("messages");
        for (ChatMessage m : messages) {
            ObjectNode item = array.addObject();
            item.put("id", m.id());
            item.put("senderId", m.senderId());
            item.put("senderUsername", m.senderUsername());
            item.put("recipientId", m.recipientId());
            item.put("recipientUsername", m.recipientUsername());
            item.put("text", m.text());
            item.put("sentAt", m.sentAt().toString());
        }
        send(response);
    }

    private void message(JsonNode request) throws SQLException {
        if (!requireLogin()) return;
        int recipientId = request.path("recipientId").asInt(-1);
        String text = text(request, "text").trim();
        if (recipientId < 1 || recipientId == user.id()) {
            sendError("Select a valid recipient.");
            return;
        }
        if (text.isBlank() || text.length() > 4000) {
            sendError("Message must contain 1-4000 characters.");
            return;
        }

        Optional<User> recipient = userDAO.findById(recipientId);
        if (recipient.isEmpty()) {
            sendError("Recipient does not exist.");
            return;
        }

        ChatMessage saved = messageDAO.save(
                user.id(), user.username(), recipientId,
                recipient.get().username(), text
        );

        ObjectNode event = mapper.createObjectNode();
        event.put("type", "MESSAGE");
        event.put("id", saved.id());
        event.put("senderId", saved.senderId());
        event.put("senderUsername", saved.senderUsername());
        event.put("recipientId", saved.recipientId());
        event.put("recipientUsername", saved.recipientUsername());
        event.put("text", saved.text());
        event.put("sentAt", saved.sentAt().toString());

        send(event); // Echo to sender.
        server.sendToUser(recipientId, event);
    }

    public boolean isAuthenticated() {
        return user != null;
    }

    public int getUserId() {
        return user == null ? -1 : user.id();
    }

    public ObjectNode publicUser() {
        ObjectNode node = mapper.createObjectNode();
        node.put("userId", user.id());
        node.put("username", user.username());
        return node;
    }

    public void sendUsers(List<ObjectNode> users) {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "USERS");
        ArrayNode array = response.putArray("users");
        users.forEach(u -> array.add(u));
        send(response);
    }

    public void send(ObjectNode node) {
        try {
            String line = mapper.writeValueAsString(node);
            synchronized (writeLock) {
                if (out != null) {
                    out.write(line);
                    out.newLine();
                    out.flush();
                }
            }
        } catch (IOException ignored) {
            try { socket.close(); } catch (IOException ignoredAgain) {}
        }
    }

    private boolean requireLogin() {
        if (user == null) {
            sendError("Please log in first.");
            return false;
        }
        return true;
    }

    private void sendError(String message) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "ERROR");
        node.put("message", message);
        send(node);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }
}
