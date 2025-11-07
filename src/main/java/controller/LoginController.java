package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import model.User;
import util.DataStore;
import util.Validator;

import java.io.IOException;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class LoginController {
    // -------------------- FXML ELEMENTS --------------------
    @FXML private VBox loginPane;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;

    @FXML private VBox registerPane;
    @FXML private TextField usernameField;
    @FXML private TextField signupEmailField;
    @FXML private PasswordField signupPasswordField;
    @FXML private Label registerStatusLabel;
    @FXML private Button registerButton;
    @FXML private TextField passwordVisibleField;
    @FXML private CheckBox showPasswordCheck;

    private int idleTime = 0;
    private Timer idleTimer = new Timer(true);
    private TimerTask currentIdleTask;

    // -------------------- INITIALIZATION --------------------
    @FXML
    public void initialize() {
        // Initialize database connection
        DataStore.initialize();

        // Start idle timer
        startIdleTimer();

        // Text Event: Real-time email validation for login
        emailField.textProperty().addListener((obs, oldV, newV) -> {
            resetIdleTimer();
            validateEmailField(emailField, newV);
        });

        // Text Event: Real-time password validation for login
        passwordField.textProperty().addListener((obs, oldV, newV) -> {
            resetIdleTimer();
            validatePasswordField(passwordField, newV);
        });

        // Keep both fields in sync
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        // Toggle visibility/managed so layout doesn’t shift
        showPasswordCheck.selectedProperty().addListener((obs, wasChecked, isChecked) -> {
            passwordVisibleField.setVisible(isChecked);
            passwordVisibleField.setManaged(isChecked);

            passwordField.setVisible(!isChecked);
            passwordField.setManaged(!isChecked);
        });

        // Text Event: Real-time email validation for registration
        signupEmailField.textProperty().addListener((obs, oldV, newV) -> {
            resetIdleTimer();
            validateEmailField(signupEmailField, newV);
        });

        // Text Event: Real-time password validation for registration
        signupPasswordField.textProperty().addListener((obs, oldV, newV) -> {
            resetIdleTimer();
            validatePasswordField(signupPasswordField, newV);
        });

        // Text Event: Username validation
        usernameField.textProperty().addListener((obs, oldV, newV) -> {
            resetIdleTimer();
            if (newV.length() < 3) {
                usernameField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            } else {
                usernameField.setStyle("-fx-border-color: green; -fx-border-width: 2;");
            }
        });

        // Key Event: Enter key submits login form
        emailField.setOnKeyPressed(this::handleKeyPress);
        passwordField.setOnKeyPressed(this::handleKeyPress);

        // Key Event: Enter key submits registration form
        usernameField.setOnKeyPressed(this::handleRegisterKeyPress);
        signupEmailField.setOnKeyPressed(this::handleRegisterKeyPress);
        signupPasswordField.setOnKeyPressed(this::handleRegisterKeyPress);

        // Mouse Event: Hover over Login button
        setupButtonHoverEffect(loginButton);
        setupButtonHoverEffect(registerButton);

        // Window Event: Confirm exit on close
        Platform.runLater(() -> {
            Stage stage = (Stage) loginPane.getScene().getWindow();
            stage.setOnCloseRequest(this::handleWindowClose);
        });
    }

    // -------------------- TEXT EVENT: EMAIL VALIDATION --------------------
    private void validateEmailField(TextField field, String email) {
        if (email.isEmpty()) {
            field.setStyle("-fx-border-color: gray; -fx-border-width: 1;");
            return;
        }

        if (!Validator.isValidEmail(email)) {
            field.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        } else {
            field.setStyle("-fx-border-color: green; -fx-border-width: 2;");
            simulateEmailCheckAPI(email);
        }
    }

    // -------------------- TEXT EVENT: PASSWORD VALIDATION --------------------
    private void validatePasswordField(PasswordField field, String password) {
        if (password.isEmpty()) {
            field.setStyle("-fx-border-color: gray; -fx-border-width: 1;");
            return;
        }

        if (password.length() < 6) {
            field.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        } else {
            field.setStyle("-fx-border-color: green; -fx-border-width: 2;");
        }
    }

    // -------------------- KEY EVENT: ENTER KEY HANDLER --------------------
    @FXML
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin();
        }
    }

    @FXML
    private void handleRegisterKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleRegister();
        }
    }

    // -------------------- ACTION EVENT: LOGIN --------------------
    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String pass = passwordField.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            setStatus(statusLabel, "Please enter both email and password.", "red");
            return;
        }

        if (!Validator.isValidEmail(email)) {
            setStatus(statusLabel, "Invalid email format!", "red");
            return;
        }

        boolean isValid = DataStore.validateUser(email, pass);
        if (!isValid) {
            setStatus(statusLabel, "Invalid login credentials!", "red");
            emailField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            passwordField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        } else {
            setStatus(statusLabel, "Login Successful!", "green");
            emailField.setStyle("-fx-border-color: green; -fx-border-width: 2;");
            passwordField.setStyle("-fx-border-color: green; -fx-border-width: 2;");

            stopIdleTimer();

            if (DataStore.isAdmin(email)) {
                System.out.println("Opening Admin Dashboard...");
                switchToAdminDashboard();
            } else {
                System.out.println("Opening User Dashboard...");
                switchToUserDashboard(email);
            }
        }
    }

    // -------------------- ACTION EVENT: REGISTRATION --------------------
    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String email = signupEmailField.getText();
        String pass = signupPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            setStatus(registerStatusLabel, "Please fill out all fields.", "red");
            return;
        }

        if (username.length() < 3) {
            setStatus(registerStatusLabel, "Username must be at least 3 characters!", "red");
            return;
        }

        if (!Validator.isValidEmail(email)) {
            setStatus(registerStatusLabel, "Invalid email format!", "red");
            return;
        }

        if (pass.length() < 6) {
            setStatus(registerStatusLabel, "Password must be at least 6 characters!", "red");
            return;
        }

        if (DataStore.userExists(email)) {
            setStatus(registerStatusLabel, "User already exists!", "red");
            return;
        }

        DataStore.addUser(username, email, pass, "user");
        setStatus(registerStatusLabel, "Account created! You can now log in.", "green");

        // Send welcome email after successful signup
        User newUser = new User(username, email, pass, "user");
        util.EmailService.getInstance().sendWelcomeEmail(newUser);
        System.out.println("[EMAIL] Welcome email triggered for: " + email);

        usernameField.clear();
        signupEmailField.clear();
        signupPasswordField.clear();

        usernameField.setStyle("");
        signupEmailField.setStyle("");
        signupPasswordField.setStyle("");
    }

    // -------------------- SWITCHING VIEWS --------------------
    @FXML
    private void switchToRegister() {
        resetIdleTimer();
        loginPane.setVisible(false);
        registerPane.setVisible(true);
        clearLoginFields();
    }

    @FXML
    private void switchToLogin() {
        resetIdleTimer();
        registerPane.setVisible(false);
        loginPane.setVisible(true);
        clearRegisterFields();
    }

    private void clearLoginFields() {
        emailField.clear();
        passwordField.clear();
        emailField.setStyle("");
        passwordField.setStyle("");
        statusLabel.setText("");
    }

    private void clearRegisterFields() {
        usernameField.clear();
        signupEmailField.clear();
        signupPasswordField.clear();
        usernameField.setStyle("");
        signupEmailField.setStyle("");
        signupPasswordField.setStyle("");
        registerStatusLabel.setText("");
    }

    // -------------------- DASHBOARD SWITCHING --------------------
    private void switchToUserDashboard(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/user_dashboard.fxml"));
            Parent root = loader.load();

            UserController controller = loader.getController();
            User currentUser = DataStore.getUserByEmail(email);

            if (currentUser != null) {
                controller.setCurrentUser(currentUser);
            }

            Stage stage = (Stage) loginPane.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("User Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
            setStatus(statusLabel, "Error loading User Dashboard!", "red");
        }
    }

    private void switchToAdminDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin_dashboard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginPane.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 500));
            stage.setTitle("Admin Dashboard");

            simulateAdminSyncAPI();
        } catch (IOException e) {
            e.printStackTrace();
            setStatus(statusLabel, "Error loading Admin Dashboard!", "red");
        }
    }

    // -------------------- MOUSE EVENT: HOVER EFFECTS --------------------
    private void setupButtonHoverEffect(Button button) {
        String originalStyle = button.getStyle();

        button.setOnMouseEntered(e -> {
            resetIdleTimer();
            button.setStyle(originalStyle + "-fx-background-color: #4CAF50; " +
                    "-fx-text-fill: white; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);");
        });

        button.setOnMouseExited(e -> {
            button.setStyle(originalStyle);
        });
    }

    // -------------------- TIMER EVENT: IDLE SESSION --------------------
    private void startIdleTimer() {
        currentIdleTask = new TimerTask() {
            public void run() {
                idleTime += 5;
                if (idleTime >= 30) {
                    Platform.runLater(() -> {
                        setStatus(statusLabel, "Session expired. Please log in again.", "red");
                        clearLoginFields();
                        clearRegisterFields();

                        if (registerPane.isVisible()) {
                            registerPane.setVisible(false);
                            loginPane.setVisible(true);
                        }
                    });
                    idleTime = 0;
                }
            }
        };
        idleTimer.schedule(currentIdleTask, 0, 5000);
    }

    private void resetIdleTimer() {
        idleTime = 0;
    }

    private void stopIdleTimer() {
        if (currentIdleTask != null) {
            currentIdleTask.cancel();
        }
        idleTimer.cancel();
    }

    // -------------------- WINDOW EVENT: CLOSE CONFIRMATION --------------------
    private void handleWindowClose(WindowEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Confirmation");
        alert.setHeaderText("Are you sure you want to exit?");
        alert.setContentText("Any unsaved changes will be lost.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() != ButtonType.OK) {
            event.consume();
        } else {
            stopIdleTimer();
            DataStore.closeConnection();
        }
    }

    // -------------------- NETWORK EVENT: SIMULATE API CALLS --------------------
    private void simulateEmailCheckAPI(String email) {
        new Thread(() -> {
            try {
                Thread.sleep(500);
                Platform.runLater(() -> {
                    System.out.println("[API] Checking email: " + email + " - Email format valid");
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void simulateAdminSyncAPI() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                Platform.runLater(() -> {
                    System.out.println("[API] Admin user list synced successfully");
                    System.out.println("[API] Total users: " + DataStore.getUsers().size());
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // -------------------- UTILITY --------------------
    private void setStatus(Label label, String text, String color) {
        if (label != null) {
            label.setText(text);
            label.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        }
    }
}