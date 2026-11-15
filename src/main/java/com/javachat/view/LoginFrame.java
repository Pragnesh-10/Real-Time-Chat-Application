package com.javachat.view;

import com.javachat.controller.ChatController;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Authentication window with a welcome screen and separate login and
 * registration forms.
 */
public class LoginFrame extends Frame {
    private static final String WELCOME = "welcome";
    private static final String LOGIN = "login";
    private static final String REGISTER = "register";

    private final ChatController controller;
    private final CardLayout cardLayout = new CardLayout();
    private final Panel cards = new Panel(cardLayout);
    private final Label statusLabel = new Label(" ", Label.CENTER);

    private final TextField loginUsernameField = new TextField(24);
    private final TextField loginPasswordField = new TextField(24);

    private final TextField registerUsernameField = new TextField(24);
    private final TextField emailField = new TextField(24);
    private final TextField registerPasswordField = new TextField(24);
    private final TextField confirmPasswordField = new TextField(24);

    public LoginFrame(ChatController controller) {
        this.controller = controller;

        setTitle("JavaChat");
        setSize(520, 430);
        setResizable(false);
        setLayout(new BorderLayout(16, 16));
        setBackground(new Color(245, 247, 250));

        loginPasswordField.setEchoChar('*');
        registerPasswordField.setEchoChar('*');
        confirmPasswordField.setEchoChar('*');

        cards.add(createWelcomePanel(), WELCOME);
        cards.add(createLoginPanel(), LOGIN);
        cards.add(createRegisterPanel(), REGISTER);

        Label appTitle = new Label("JavaChat", Label.CENTER);
        appTitle.setFont(new Font("SansSerif", Font.BOLD, 30));
        appTitle.setForeground(new Color(31, 54, 92));

        Label tagline = new Label("Simple. Fast. Real-time messaging.", Label.CENTER);
        tagline.setFont(new Font("SansSerif", Font.PLAIN, 14));

        Panel header = new Panel(new GridLayout(2, 1, 0, 4));
        header.setBackground(new Color(245, 247, 250));
        header.add(appTitle);
        header.add(tagline);

        statusLabel.setForeground(new Color(180, 35, 35));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        Panel content = new Panel(new BorderLayout(12, 12));
        content.setBackground(new Color(245, 247, 250));
        content.add(header, BorderLayout.NORTH);
        content.add(cards, BorderLayout.CENTER);
        content.add(statusLabel, BorderLayout.SOUTH);

        Panel padding = new Panel(new BorderLayout());
        padding.setBackground(new Color(245, 247, 250));
        padding.add(content, BorderLayout.CENTER);
        add(padding, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });

        showWelcome();
        setLocationRelativeTo(null);
    }

    private Panel createWelcomePanel() {
        Panel panel = new Panel(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        Panel content = new Panel(new GridLayout(4, 1, 0, 14));
        content.setBackground(Color.WHITE);

        Label heading = new Label("Welcome!", Label.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 23));

        Label message = new Label("Choose how you want to continue.", Label.CENTER);
        message.setFont(new Font("SansSerif", Font.PLAIN, 14));

        ColorButton loginButton = createButton("Login", new Color(37, 99, 235), Color.WHITE);
        ColorButton createAccountButton = createButton("Create Account", new Color(22, 163, 74), Color.WHITE);

        loginButton.addActionListener(e -> showLoginForm());
        createAccountButton.addActionListener(e -> showRegisterForm());

        content.add(heading);
        content.add(message);
        content.add(loginButton);
        content.add(createAccountButton);

        panel.add(content);
        return panel;
    }

    private Panel createLoginPanel() {
        Panel panel = new Panel(new BorderLayout(12, 16));
        panel.setBackground(Color.WHITE);

        Label heading = new Label("Login to your account", Label.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));
        panel.add(heading, BorderLayout.NORTH);

        Panel fields = new Panel(new GridLayout(2, 2, 10, 14));
        fields.setBackground(Color.WHITE);
        fields.add(new Label("Username"));
        fields.add(loginUsernameField);
        fields.add(new Label("Password"));
        fields.add(loginPasswordField);
        panel.add(fields, BorderLayout.CENTER);

        ColorButton submitButton = createButton("Login", new Color(37, 99, 235), Color.WHITE);
        ColorButton createAccountButton = createButton("Create Account", new Color(22, 163, 74), Color.WHITE);
        ColorButton backButton = createButton("Back", new Color(225, 229, 235), new Color(35, 42, 52));

        submitButton.addActionListener(e -> submitLogin());
        loginPasswordField.addActionListener(e -> submitLogin());
        createAccountButton.addActionListener(e -> showRegisterForm());
        backButton.addActionListener(e -> showWelcome());

        Panel actions = new Panel(new GridLayout(1, 3, 8, 0));
        actions.setBackground(Color.WHITE);
        actions.add(submitButton);
        actions.add(createAccountButton);
        actions.add(backButton);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private Panel createRegisterPanel() {
        Panel panel = new Panel(new BorderLayout(12, 16));
        panel.setBackground(Color.WHITE);

        Label heading = new Label("Create your account", Label.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));
        panel.add(heading, BorderLayout.NORTH);

        Panel fields = new Panel(new GridLayout(4, 2, 10, 10));
        fields.setBackground(Color.WHITE);
        fields.add(new Label("Username"));
        fields.add(registerUsernameField);
        fields.add(new Label("Email"));
        fields.add(emailField);
        fields.add(new Label("Password"));
        fields.add(registerPasswordField);
        fields.add(new Label("Confirm Password"));
        fields.add(confirmPasswordField);
        panel.add(fields, BorderLayout.CENTER);

        ColorButton submitButton = createButton("Create Account", new Color(22, 163, 74), Color.WHITE);
        ColorButton loginButton = createButton("I already have an account", new Color(37, 99, 235), Color.WHITE);
        ColorButton backButton = createButton("Back", new Color(225, 229, 235), new Color(35, 42, 52));

        submitButton.addActionListener(e -> submitRegistration());
        confirmPasswordField.addActionListener(e -> submitRegistration());
        loginButton.addActionListener(e -> showLoginForm());
        backButton.addActionListener(e -> showWelcome());

        Panel actions = new Panel(new GridLayout(1, 3, 8, 0));
        actions.setBackground(Color.WHITE);
        actions.add(submitButton);
        actions.add(loginButton);
        actions.add(backButton);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private ColorButton createButton(String text, Color background, Color foreground) {
        return new ColorButton(text, background, foreground);
    }

    /**
     * A custom-drawn button built with AWT Canvas.
     * This avoids the native macOS AWT Button appearance overriding its colors.
     */
    private static class ColorButton extends Canvas {
        private final String text;
        private final Color buttonColor;
        private final Color textColor;
        private final List<ActionListener> listeners = new ArrayList<>();
        private boolean hovered;
        private boolean pressed;

        ColorButton(String text, Color buttonColor, Color textColor) {
            this.text = text;
            this.buttonColor = buttonColor;
            this.textColor = textColor;
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setPreferredSize(new Dimension(150, 42));
            setMinimumSize(new Dimension(100, 38));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFocusable(true);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    pressed = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        pressed = true;
                        requestFocus();
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    boolean activate = pressed && contains(e.getPoint());
                    pressed = false;
                    repaint();
                    if (activate) {
                        ActionEvent event = new ActionEvent(
                                ColorButton.this,
                                ActionEvent.ACTION_PERFORMED,
                                text
                        );
                        for (ActionListener listener : listeners) {
                            listener.actionPerformed(event);
                        }
                    }
                }
            });
        }

        public void addActionListener(ActionListener listener) {
            listeners.add(listener);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(180, 42);
        }

        @Override
        public void paint(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            Color fill = buttonColor;
            if (pressed) {
                fill = buttonColor.darker();
            } else if (hovered) {
                fill = buttonColor.brighter();
            }

            int arc = 12;
            g.setColor(fill);
            g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);

            g.setFont(getFont());
            g.setColor(textColor);
            FontMetrics metrics = g.getFontMetrics();
            int x = (getWidth() - metrics.stringWidth(text)) / 2;
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(text, x, y);
            g.dispose();
        }
    }

    private void showWelcome() {
        statusLabel.setText(" ");
        cardLayout.show(cards, WELCOME);
    }

    private void showLoginForm() {
        statusLabel.setText(" ");
        cardLayout.show(cards, LOGIN);
        loginUsernameField.requestFocus();
    }

    private void showRegisterForm() {
        statusLabel.setText(" ");
        cardLayout.show(cards, REGISTER);
        registerUsernameField.requestFocus();
    }

    private void submitLogin() {
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();

        if (username.isBlank() || password.isBlank()) {
            showError("Enter your username and password.");
            return;
        }

        statusLabel.setForeground(new Color(35, 110, 65));
        statusLabel.setText("Connecting to server...");
        controller.connectAndRegisterOrLogin(username, "", password, false);
    }

    private void submitRegistration() {
        String username = registerUsernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = registerPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            showError("Complete all registration fields.");
            return;
        }
        if (!username.matches("^[A-Za-z0-9_]{3,30}$")) {
            showError("Username must be 3-30 characters: letters, numbers, underscores.");
            return;
        }
        if (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            showError("Enter a valid email address.");
            return;
        }
        if (password.length() < 8 || password.length() > 72) {
            showError("Password must be between 8 and 72 characters.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        statusLabel.setForeground(new Color(35, 110, 65));
        statusLabel.setText("Creating your account...");
        controller.connectAndRegisterOrLogin(username, email, password, true);
    }

    public void showError(String message) {
        statusLabel.setForeground(new Color(180, 35, 35));
        statusLabel.setText(message.length() > 75 ? message.substring(0, 72) + "..." : message);
    }
}
