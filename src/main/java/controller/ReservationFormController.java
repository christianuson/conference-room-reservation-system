package controller;

import javafx.collections.FXCollections;
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
    @FXML private ComboBox<String> startHourCombo;
    @FXML private ComboBox<String> startMinuteCombo;
    @FXML private ComboBox<String> endHourCombo;
    @FXML private ComboBox<String> endMinuteCombo;
    @FXML private TextField customerField;
    @FXML private TextArea notesField;
    @FXML private Label statusLabel;
    @FXML private Label bookingPreviewLabel;
    @FXML private Label conflictWarningLabel;
    @FXML private Button reserveButton;

    private Room selectedRoom;
    private User currentUser;
    private UserController parentController;

    @FXML
    public void initialize() {
        // Initialize time ComboBoxes
        setupTimeComboBoxes();

        // Text Event: Live preview of booking details
        setupBookingPreview();

        // Check for conflicts when times change
        setupConflictDetection();
    }

    // -------------------- TIME COMBOBOX SETUP --------------------
    private void setupTimeComboBoxes() {
        // Hours: 00-23
        javafx.collections.ObservableList<String> hours = FXCollections.observableArrayList();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d", i));
        }
        startHourCombo.setItems(hours);
        endHourCombo.setItems(hours);

        // Minutes: 00, 15, 30, 45
        javafx.collections.ObservableList<String> minutes = FXCollections.observableArrayList(
                "00", "15", "30", "45"
        );
        startMinuteCombo.setItems(minutes);
        endMinuteCombo.setItems(minutes);

        // Set defaults
        startHourCombo.setValue("09");
        startMinuteCombo.setValue("00");
        endHourCombo.setValue("10");
        endMinuteCombo.setValue("00");
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

    // -------------------- CONFLICT DETECTION --------------------
    private void setupConflictDetection() {
        // Check for conflicts when date or times change
        datePicker.valueProperty().addListener((obs, oldV, newV) -> checkForConflicts());
        startHourCombo.valueProperty().addListener((obs, oldV, newV) -> checkForConflicts());
        startMinuteCombo.valueProperty().addListener((obs, oldV, newV) -> checkForConflicts());
        endHourCombo.valueProperty().addListener((obs, oldV, newV) -> checkForConflicts());
        endMinuteCombo.valueProperty().addListener((obs, oldV, newV) -> checkForConflicts());
    }

    private void checkForConflicts() {
        if (selectedRoom == null || datePicker.getValue() == null ||
                startHourCombo.getValue() == null || startMinuteCombo.getValue() == null ||
                endHourCombo.getValue() == null || endMinuteCombo.getValue() == null) {
            return;
        }

        String date = datePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String startTime = startHourCombo.getValue() + ":" + startMinuteCombo.getValue();
        String endTime = endHourCombo.getValue() + ":" + endMinuteCombo.getValue();

        boolean hasConflict = DataStore.hasConflict(selectedRoom.getName(), date, startTime, endTime);

        if (hasConflict) {
            conflictWarningLabel.setText("⚠️ WARNING: This time slot conflicts with an approved reservation. You can still submit, but it may be rejected.");
            conflictWarningLabel.setVisible(true);
            conflictWarningLabel.setManaged(true);
            reserveButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 5;");
        } else {
            conflictWarningLabel.setVisible(false);
            conflictWarningLabel.setManaged(false);
            reserveButton.setStyle("-fx-background-color: #a51618; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 5;");
        }
    }

    // -------------------- TEXT EVENT: LIVE BOOKING PREVIEW --------------------
    private void setupBookingPreview() {
        if (datePicker != null) {
            datePicker.valueProperty().addListener((obs, oldV, newV) -> updateBookingPreview());
        }
        if (startHourCombo != null) {
            startHourCombo.valueProperty().addListener((obs, oldV, newV) -> updateBookingPreview());
        }
        if (startMinuteCombo != null) {
            startMinuteCombo.valueProperty().addListener((obs, oldV, newV) -> updateBookingPreview());
        }
        if (endHourCombo != null) {
            endHourCombo.valueProperty().addListener((obs, oldV, newV) -> updateBookingPreview());
        }
        if (endMinuteCombo != null) {
            endMinuteCombo.valueProperty().addListener((obs, oldV, newV) -> updateBookingPreview());
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

            String startTime = "";
            if (startHourCombo != null && startMinuteCombo != null &&
                    startHourCombo.getValue() != null && startMinuteCombo.getValue() != null) {
                startTime = startHourCombo.getValue() + ":" + startMinuteCombo.getValue();
            } else {
                startTime = "Not specified";
            }

            String endTime = "";
            if (endHourCombo != null && endMinuteCombo != null &&
                    endHourCombo.getValue() != null && endMinuteCombo.getValue() != null) {
                endTime = endHourCombo.getValue() + ":" + endMinuteCombo.getValue();
            } else {
                endTime = "Not specified";
            }

            String customer = customerField != null && !customerField.getText().isEmpty() ?
                    customerField.getText() : "Not specified";

            bookingPreviewLabel.setText(
                    String.format("Booking: %s | Date: %s | Time: %s - %s | Customer: %s",
                            room, date, startTime, endTime, customer)
            );
            bookingPreviewLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666; -fx-font-size: 12px;");
        }
    }

    // -------------------- ACTION EVENT: RESERVE ROOM --------------------
    @FXML
    private void handleReservation() {
        // Validation
        if (selectedRoom == null) {
            showStatus("Error: No room selected!", Color.RED);
            return;
        }

        if (datePicker.getValue() == null) {
            showStatus("Please select a date!", Color.RED);
            datePicker.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            return;
        } else {
            datePicker.setStyle("");
        }

        if (startHourCombo.getValue() == null || startMinuteCombo.getValue() == null) {
            showStatus("Please select start time!", Color.RED);
            return;
        }

        if (endHourCombo.getValue() == null || endMinuteCombo.getValue() == null) {
            showStatus("Please select end time!", Color.RED);
            return;
        }

        if (customerField.getText().isEmpty()) {
            showStatus("Please enter customer name!", Color.RED);
            customerField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            return;
        } else {
            customerField.setStyle("");
        }

        // Get time range
        String date = datePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String startTime = startHourCombo.getValue() + ":" + startMinuteCombo.getValue();
        String endTime = endHourCombo.getValue() + ":" + endMinuteCombo.getValue();

        // Validate time range
        int startMinutes = timeToMinutes(startTime);
        int endMinutes = timeToMinutes(endTime);

        if (endMinutes <= startMinutes) {
            showStatus("End time must be after start time!", Color.RED);
            return;
        }

        // Check for conflicts
        boolean hasConflict = DataStore.hasConflict(selectedRoom.getName(), date, startTime, endTime);

        if (hasConflict) {
            // Show warning but allow submission
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setTitle("Time Conflict Detected");
            warning.setHeaderText("This time slot conflicts with an approved reservation");
            warning.setContentText("Your reservation will be marked as PENDING and may be rejected by the admin.\n\nDo you want to continue?");

            ButtonType result = warning.showAndWait().orElse(ButtonType.CANCEL);
            if (result != ButtonType.OK) {
                return;
            }
        }

        // Create reservation
        String username = currentUser != null ? currentUser.getUsername() : customerField.getText();
        String roomName = selectedRoom.getName();

        // Save reservation with PENDING status (admin will approve)
        Reservation reservation = new Reservation(username, roomName, date, startTime, endTime, "pending");
        DataStore.addReservation(reservation);

        // Show success
        showStatus("Request submitted successfully!", Color.GREEN);
        showAlert(Alert.AlertType.INFORMATION, "Reservation Submitted",
                "Your reservation request for '" + roomName + "' on " + date +
                        " from " + startTime + " to " + endTime + " is now PENDING.\n\n" +
                        "The admin will review and approve your request.");

        // Simulate confirmation email
        simulateConfirmationEmail(username, roomName, date + " " + startTime + " - " + endTime + " (PENDING)");

        // Refresh and close
        if (parentController != null) {
            parentController.refreshCards();
        }
        closeForm();
    }

    // Helper: Convert time string to minutes
    private int timeToMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
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
                Thread.sleep(1000);
                javafx.application.Platform.runLater(() -> {
                    System.out.println("[EMAIL API] Sending confirmation email...");
                    System.out.println("[EMAIL API] To: " + username + "@example.com");
                    System.out.println("[EMAIL API] Subject: Room Reservation Request Received");
                    System.out.println("[EMAIL API] Body: Your reservation request for '" + roomName +
                            "' on " + dateTime + " has been received and is pending admin approval.");
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