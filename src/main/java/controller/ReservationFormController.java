package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.Reservation;
import model.Room;
import model.User;
import util.DataStore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReservationFormController {

    @FXML private Label roomNameLabel;
    @FXML private Label roomStatusLabel;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private TextField customerField;
    @FXML private TextArea notesField;
    @FXML private Label statusLabel;
    @FXML private Label bookingPreviewLabel;
    @FXML private Button reserveButton;

    private Room selectedRoom;
    private User currentUser;
    private UserController parentController;

    @FXML
    public void initialize() {
        // Text Event: Live preview of booking details
        setupBookingPreview();
    }

    // -------------------- SETTERS --------------------
    public void setRoom(Room room) {
        this.selectedRoom = room;
        if (roomNameLabel != null) {
            roomNameLabel.setText(room.getName());
            roomNameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");
        }
        if (roomStatusLabel != null) {
            roomStatusLabel.setText("Status: " + room.getStatus());
            roomStatusLabel.setStyle(getStatusStyle(room.getStatus()));
        }
        updateBookingPreview();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (customerField != null && user != null) {
            customerField.setText(user.getUsername());
        }
    }

    public void setParentController(UserController controller) {
        this.parentController = controller;
    }

    // -------------------- TEXT EVENT: LIVE BOOKING PREVIEW --------------------
    private void setupBookingPreview() {
        if (datePicker != null) {
            datePicker.valueProperty().addListener((obs, oldV, newV) -> updateBookingPreview());
        }
        if (timeField != null) {
            timeField.textProperty().addListener((obs, oldV, newV) -> updateBookingPreview());
        }
        if (customerField != null) {
            customerField.textProperty().addListener((obs, oldV, newV) -> updateBookingPreview());
        }
    }

    private void updateBookingPreview() {
        if (bookingPreviewLabel != null && selectedRoom != null) {
            String room = selectedRoom.getName();
            String date = datePicker != null && datePicker.getValue() != null ?
                    datePicker.getValue().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "Not selected";
            String time = timeField != null && !timeField.getText().isEmpty() ?
                    timeField.getText() : "Not specified";
            String customer = customerField != null && !customerField.getText().isEmpty() ?
                    customerField.getText() : "Not specified";

            bookingPreviewLabel.setText(
                    String.format("Booking: %s | Date: %s | Time: %s | Customer: %s",
                            room, date, time, customer)
            );
            bookingPreviewLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666; -fx-font-size: 12px;");
        }
    }

    // -------------------- ACTION EVENT: RESERVE ROOM --------------------
    // -------------------- ACTION EVENT: RESERVE ROOM --------------------
    @FXML
    private void handleReservation() {
        // Validation
        if (selectedRoom == null) {
            showStatus("Error: No room selected!", Color.RED);
            return;
        }

        // Only allow requests if currently Available (can't request for Reserved/Pending)
        if (!"Available".equalsIgnoreCase(selectedRoom.getStatus())) {
            showStatus("Reservation Denied! Room is not available.", Color.RED);
            showAlert(Alert.AlertType.ERROR, "Reservation Denied",
                    "The selected room '" + selectedRoom.getName() + "' is not available.");
            return;
        }

        if (datePicker.getValue() == null) {
            showStatus("Please select a date!", Color.RED);
            datePicker.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            return;
        } else {
            datePicker.setStyle("");
        }

        if (timeField.getText().isEmpty()) {
            showStatus("Please enter a time!", Color.RED);
            timeField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            return;
        } else {
            timeField.setStyle("");
        }

        if (customerField.getText().isEmpty()) {
            showStatus("Please enter customer name!", Color.RED);
            customerField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            return;
        } else {
            customerField.setStyle("");
        }

        // Create reservation data
        String username = currentUser != null ? currentUser.getUsername() : customerField.getText();
        String roomName = selectedRoom.getName();
        String dateTime = datePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + " " + timeField.getText();

        // Mark room as PENDING (awaiting admin approval)
        selectedRoom.setStatus("Pending");
        DataStore.updateRoom(selectedRoom);

        // Save reservation (no status field on Reservation model, we rely on Room status)
        Reservation reservation = new Reservation(username, roomName, dateTime);
        DataStore.addReservation(reservation);

        // Feedback
        showStatus("Request submitted. Status: Pending admin approval.", Color.ORANGE);
        showAlert(Alert.AlertType.INFORMATION, "Request Submitted",
                "Your reservation request for '" + roomName + "' on " + dateTime + " is now PENDING.\n"
                        + "You will see it become RESERVED once the admin approves.");

        // Optional: “email” copy says pending
        simulateConfirmationEmail(username, roomName, dateTime + " (PENDING)");

        // Refresh and close
        if (parentController != null) parentController.refreshCards();
        closeForm();
    }

    // -------------------- CANCEL ACTION --------------------
    @FXML
    private void handleCancel() {
        closeForm();
    }

    private void closeForm() {
        Stage stage = (Stage) reserveButton.getScene().getWindow();
        stage.close();
    }

    // -------------------- NETWORK EVENT: CONFIRMATION EMAIL --------------------
    private void simulateConfirmationEmail(String username, String roomName, String dateTime) {
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate network delay
                javafx.application.Platform.runLater(() -> {
                    System.out.println("[EMAIL API] Sending confirmation email...");
                    System.out.println("[EMAIL API] To: " + username + "@example.com");
                    System.out.println("[EMAIL API] Subject: Room Reservation Confirmation");
                    System.out.println("[EMAIL API] Body: Your reservation for '" + roomName +
                            "' on " + dateTime + " has been confirmed.");
                    System.out.println("[EMAIL API] Status: Sent successfully (200 OK)");
                    System.out.println("[EMAIL API] Timestamp: " +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // -------------------- UTILITY METHODS --------------------
    private void showStatus(String message, Color color) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setTextFill(color);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String getStatusStyle(String status) {
        String baseStyle = "-fx-font-size: 14px; -fx-font-weight: bold;";

        if ("Available".equalsIgnoreCase(status)) {
            return baseStyle + "-fx-text-fill: #4CAF50;";
        } else if ("Reserved".equalsIgnoreCase(status)) {
            return baseStyle + "-fx-text-fill: #f44336;";
        } else if ("Pending".equalsIgnoreCase(status)) {
            return baseStyle + "-fx-text-fill: #FFC107;";
        }
        return baseStyle + "-fx-text-fill: #999;";
    }
}