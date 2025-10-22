package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import util.DataStore;
import util.Validator;

import java.util.Timer;
import java.util.TimerTask;

public class LoginController {
    // -------------------- FXML ELEMENTS --------------------
    @FXML private VBox loginPane;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML private VBox registerPane;
    @FXML private TextField usernameField;
    @FXML private TextField signupEmailField;
    @FXML private PasswordField signupPasswordField;
    @FXML private Label registerStatusLabel;

    private int idleTime = 0;
    private Timer idleTimer = new Timer();

    // -------------------- INITIALIZATION --------------------
    @FXML
    public void initialize() {
        // Load users from JSON file instead of static data
        DataStore.loadUsers("src/main/resources/data/users.json");
        startIdleTimer();

        // Real-time email validation
        emailField.textProperty().addListener((obs, oldV, newV) -> {
            if (!Validator.isValidEmail(newV)) {
                emailField.setStyle("-fx-border-color: red;");
            } else {
                emailField.setStyle("-fx-border-color: green;");
            }
        });
    }

    // -------------------- LOGIN --------------------
    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String pass = passwordField.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            setStatus(statusLabel, "Please enter both email and password.", "red");
            return;
        }

        boolean isValid = DataStore.validateUser(email, pass);
        if (!isValid) {
            setStatus(statusLabel, "Invalid login credentials!", "red");
        } else {
            setStatus(statusLabel, "Login Successful!", "green");

            // Role differentiation
            if (DataStore.isAdmin(email)) {
                System.out.println("Opening Admin Dashboard...");
            } else {
                System.out.println("Opening User Dashboard...");
            }
        }
    }

    // -------------------- REGISTRATION --------------------
    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String email = signupEmailField.getText();
        String pass = signupPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            setStatus(registerStatusLabel, "Please fill out all fields.", "red");
            return;
        }

        if (!Validator.isValidEmail(email)) {
            setStatus(registerStatusLabel, "Invalid email format!", "red");
            return;
        }

        if (DataStore.userExists(email)) {
            setStatus(registerStatusLabel, "User already exists!", "red");
            return;
        }

        DataStore.addUser(username, email, pass, "user");
        setStatus(registerStatusLabel, "Account created! You can now log in.", "green");

        usernameField.clear();
        signupEmailField.clear();
        signupPasswordField.clear();
    }

    // -------------------- SWITCHING --------------------
    @FXML
    private void switchToRegister() {
        loginPane.setVisible(false);
        registerPane.setVisible(true);
    }

    @FXML
    private void switchToLogin() {
        registerPane.setVisible(false);
        loginPane.setVisible(true);
    }

    // -------------------- TIMER --------------------
    private void startIdleTimer() {
        idleTimer.schedule(new TimerTask() {
            public void run() {
                idleTime += 5;
                if (idleTime >= 30) {
                    setStatus(statusLabel, "Session expired. Please log in again.", "red");
                    emailField.clear();
                    passwordField.clear();
                    idleTime = 0;
                }
            }
        }, 0, 5000);
    }

    private void setStatus(Label label, String text, String color) {
        if (label != null) {
            label.setText(text);
            label.setStyle("-fx-text-fill: " + color + ";");
        }
    }
}
