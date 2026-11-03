package com.javachat.view;

import com.javachat.controller.ChatController;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LoginFrame extends Frame {
    private final ChatController controller;
    private final TextField usernameField = new TextField(24);
    private final TextField emailField = new TextField(24);
    private final TextField passwordField = new TextField(24);
    private final TextField confirmPasswordField = new TextField(24);
    private final Label errorLabel = new Label(" ", Label.CENTER);

    public LoginFrame(ChatController controller) {
        this.controller = controller;
        setTitle("JavaChat - Login / Register");
        setSize(480, 400);
        setResizable(false);
        setLayout(new BorderLayout(12, 12));

        passwordField.setEchoChar('*');
        confirmPasswordField.setEchoChar('*');

        Label title = new Label("JavaChat", Label.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        Label subtitle = new Label("Log in or create your account", Label.CENTER);

        Panel form = new Panel(new GridLayout(4, 2, 10, 10));
        form.add(new Label("Username"));
        form.add(usernameField);
        form.add(new Label("Email (registration only)"));
        form.add(emailField);
        form.add(new Label("Password"));
        form.add(passwordField);
        form.add(new Label("Confirm password (registration only)"));
        form.add(confirmPasswordField);

        Button loginButton = new Button("Log in");
        Button registerButton = new Button("Create account");
        loginButton.addActionListener(e -> submit(false));
        registerButton.addActionListener(e -> submit(true));
        passwordField.addActionListener(e -> submit(false));

        Panel buttons = new Panel(new GridLayout(1, 2, 10, 0));
        buttons.add(loginButton);
        buttons.add(registerButton);

        errorLabel.setForeground(new Color(180, 35, 35));

        Panel center = new Panel(new BorderLayout(8, 8));
        center.add(subtitle, BorderLayout.NORTH);
        center.add(form, BorderLayout.CENTER);
        center.add(buttons, BorderLayout.SOUTH);

        Panel content = new Panel(new BorderLayout(12, 12));
        content.add(title, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);
        content.add(errorLabel, BorderLayout.SOUTH);

        Panel padding = new Panel(new BorderLayout());
        padding.add(content, BorderLayout.CENTER);
        padding.setBackground(new Color(245, 245, 245));
        add(padding, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });

        setLocationRelativeTo(null);
    }

    private void submit(boolean register) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isBlank() || password.isBlank()) {
            showError("Enter a username and password.");
            return;
        }

        if (register) {
            if (email.isBlank()) {
                showError("Enter your email address.");
                return;
            }
            if (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                showError("Enter a valid email address.");
                return;
            }
            if (confirmPassword.isBlank() || !password.equals(confirmPassword)) {
                showError("Passwords do not match.");
                return;
            }
        }

        errorLabel.setText("Connecting...");
        controller.connectAndRegisterOrLogin(username, email, password, register);
    }

    public void showError(String message) {
        errorLabel.setText(message.length() > 65 ? message.substring(0, 62) + "..." : message);
        errorLabel.setToolTipText(message);
    }
}
