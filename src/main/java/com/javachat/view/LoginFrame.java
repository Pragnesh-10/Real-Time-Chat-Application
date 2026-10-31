package com.javachat.view;

import com.javachat.controller.ChatController;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final ChatController controller;
    private final JTextField usernameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField confirmPasswordField = new JPasswordField();
    private final JLabel errorLabel = new JLabel(" ");

    public LoginFrame(ChatController controller) {
        this.controller = controller;
        setTitle("JavaChat - Login / Register");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 390);
        setLocationRelativeTo(null);
        setResizable(false);

        JLabel title = new JLabel("JavaChat", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        JLabel subtitle = new JLabel("Log in or create your account", SwingConstants.CENTER);

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
        form.add(new JLabel("Username"));
        form.add(usernameField);
        form.add(new JLabel("Email (registration only)"));
        form.add(emailField);
        form.add(new JLabel("Password"));
        form.add(passwordField);
        form.add(new JLabel("Confirm password (registration only)"));
        form.add(confirmPasswordField);

        JButton loginButton = new JButton("Log in");
        JButton registerButton = new JButton("Create account");
        loginButton.addActionListener(e -> submit(false));
        registerButton.addActionListener(e -> submit(true));
        passwordField.addActionListener(e -> submit(false));

        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.add(loginButton);
        buttons.add(registerButton);

        errorLabel.setForeground(new Color(180, 35, 35));
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(subtitle, BorderLayout.NORTH);
        center.add(form, BorderLayout.CENTER);
        center.add(buttons, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        panel.add(title, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        panel.add(errorLabel, BorderLayout.SOUTH);
        setContentPane(panel);
    }

    private void submit(boolean register) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

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
