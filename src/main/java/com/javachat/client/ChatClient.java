package com.javachat.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.javachat.model.User;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ChatClient implements Closeable {
    private final ObjectMapper mapper = new ObjectMapper();
    private final BlockingQueue<String> outbound = new LinkedBlockingQueue<>();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final Consumer<JsonNode> eventConsumer;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Thread readerThread;
    private Thread writerThread;
    private volatile User currentUser;

    public ChatClient(Consumer<JsonNode> eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        connected.set(true);

        readerThread = new Thread(this::readLoop, "javachat-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        writerThread = new Thread(this::writeLoop, "javachat-writer");
        writerThread.setDaemon(true);
        writerThread.start();
    }

    public void send(ObjectNode node) {
        if (!connected.get()) {
            eventConsumer.accept(errorNode("Not connected to the server."));
            return;
        }
        try {
            outbound.put(mapper.writeValueAsString(node));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            eventConsumer.accept(errorNode("Sending was interrupted."));
        } catch (Exception e) {
            eventConsumer.accept(errorNode("Could not encode request."));
        }
    }

    public void requestLogin(String username, String password) {
        ObjectNode n = mapper.createObjectNode();
        n.put("type", "LOGIN");
        n.put("username", username);
        n.put("password", password);
        send(n);
    }

    public void requestRegister(String username, String password) {
        ObjectNode n = mapper.createObjectNode();
        n.put("type", "REGISTER");
        n.put("username", username);
        n.put("password", password);
        send(n);
    }

    public void requestUsers() {
        ObjectNode n = mapper.createObjectNode();
        n.put("type", "USERS");
        send(n);
    }

    public void requestHistory(int otherUserId) {
        ObjectNode n = mapper.createObjectNode();
        n.put("type", "HISTORY");
        n.put("otherUserId", otherUserId);
        send(n);
    }

    public void sendPrivateMessage(int recipientId, String text) {
        ObjectNode n = mapper.createObjectNode();
        n.put("type", "MESSAGE");
        n.put("recipientId", recipientId);
        n.put("text", text);
        send(n);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        currentUser = user;
    }

    public boolean isConnected() {
        return connected.get();
    }

    private void readLoop() {
        try {
            String line;
            while (connected.get() && (line = reader.readLine()) != null) {
                JsonNode node = mapper.readTree(line);
                eventConsumer.accept(node);
            }
        } catch (IOException e) {
            if (connected.get()) eventConsumer.accept(errorNode("Connection to server was lost."));
        } finally {
            connected.set(false);
        }
    }

    private void writeLoop() {
        try {
            while (connected.get()) {
                String line = outbound.take();
                writer.write(line);
                writer.newLine();
                writer.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            if (connected.get()) eventConsumer.accept(errorNode("Could not send data to server."));
        }
    }

    private ObjectNode errorNode(String message) {
        ObjectNode n = mapper.createObjectNode();
        n.put("type", "ERROR");
        n.put("message", message);
        return n;
    }

    @Override
    public void close() throws IOException {
        connected.set(false);
        if (readerThread != null) readerThread.interrupt();
        if (writerThread != null) writerThread.interrupt();
        if (socket != null) socket.close();
    }
}
