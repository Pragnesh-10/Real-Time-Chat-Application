package com.javachat.server;

import com.javachat.util.DatabaseConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private final int port;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        try (Connection ignored = DatabaseConnection.getConnection()) {
            System.out.println("MySQL connection verified.");
        } catch (SQLException e) {
            throw new IOException("Cannot connect to MySQL: " + e.getMessage(), e);
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("JavaChat server listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);
                pool.submit(handler);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    public void remove(ClientHandler handler) {
        clients.remove(handler);
        broadcastUsers();
    }

    public void broadcastUsers() {
        var users = clients.stream()
                .filter(ClientHandler::isAuthenticated)
                .map(ClientHandler::publicUser)
                .toList();

        for (ClientHandler client : clients) {
            if (client.isAuthenticated()) client.sendUsers(users);
        }
    }

    public boolean isUserOnline(int userId) {
        return clients.stream().anyMatch(c -> c.isAuthenticated() && c.getUserId() == userId);
    }

    public void sendToUser(int userId, com.fasterxml.jackson.databind.node.ObjectNode message) {
        clients.stream()
                .filter(ClientHandler::isAuthenticated)
                .filter(c -> c.getUserId() == userId)
                .forEach(c -> c.send(message));
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(
                System.getenv().getOrDefault("CHAT_PORT", "5050")
        );
        try {
            new ChatServer(port).start();
        } catch (IOException e) {
            System.err.println("Server stopped: " + e.getMessage());
        }
    }
}
