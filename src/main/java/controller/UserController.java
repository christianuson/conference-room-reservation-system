package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import model.Reservation;
import model.Room;
import model.User;
import util.DataStore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class UserController {

    @FXML private BorderPane mainPane;
    @FXML private TableView<Room> roomTable;
    @FXML private TableColumn<Room, String> roomNameColumn;
    @FXML private TableColumn<Room, String> roomStatusColumn;

    @FXML private TableView<Reservation> myReservationsTable;
    @FXML private TableColumn<Reservation, String> myResRoomColumn;
    @FXML private TableColumn<Reservation, String> myResDateColumn;
    @FXML private TableColumn<Reservation, String> myResStatusColumn;

    @FXML private TextField bookingRoomField;
    @FXML private DatePicker bookingDatePicker;
    @FXML private TextField bookingTimeField;
    @FXML private TextField customerNameField;
    @FXML private TextArea notesField;

    @FXML private Button reserveButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label bookingPreviewLabel;
    @FXML private javafx.scene.layout.VBox myReservationsSection;

    private Timer refreshTimer = new Timer(true);
    private TimerTask refreshTask;
    private User currentUser;
    private Room selectedRoom;

    @FXML
    public void initialize() {
        // Window Event: Load persisted data on open
        DataStore.loadUsers("src/main/resources/data/users.json");
        DataStore.loadRooms();
        DataStore.loadReservations();

        // Setup tables
        setupRoomTable();
        setupMyReservationsTable();

        // Bind rooms to TableView
        roomTable.setItems(DataStore.getRooms());

        // Timer Event: Auto-refresh availability every 30 seconds
        startAutoRefresh();

        // Mouse Event: Hover over room to show availability
        setupRoomHoverEffect();

        // Mouse Event: Click on room to select for booking
        setupRoomSelection();

        // Key Event: Press Enter to confirm booking
        setupKeyEventHandlers();

        // Text Event: Live preview of booking details
        setupBookingPreview();

        // Color Event: Highlight rooms by status
        highlightRoomsByStatus();

        statusLabel.setText("Ready to make a reservation");
    }

    // -------------------- TABLE SETUP --------------------
    private void setupRoomTable() {
        roomNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        roomStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void setupMyReservationsTable() {
        myResRoomColumn.setCellValueFactory(new PropertyValueFactory<>("roomName"));
        myResDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        myResStatusColumn.setCellValueFactory(new PropertyValueFactory<>("date")); // Can be customized
    }

    // -------------------- USER SETTER --------------------
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + user.getUsername() + "!");
        }
        if (customerNameField != null) {
            customerNameField.setText(user.getUsername());
        }
        statusLabel.setText("Hello " + user.getUsername() + ", select a room to get started!");
    }

    // -------------------- MOUSE EVENT: HOVER EFFECT --------------------
    private void setupRoomHoverEffect() {
        roomTable.setRowFactory(tv -> {
            TableRow<Room> row = new TableRow<>();

            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) {
                    Room room = row.getItem();
                    statusLabel.setTextFill(Color.BLUE);
                    statusLabel.setText("Room: " + room.getName() + " - Status: " + room.getStatus());
                }
            });

            row.setOnMouseExited(e -> {
                if (selectedRoom == null) {
                    statusLabel.setTextFill(Color.BLACK);
                    statusLabel.setText("Ready to make a reservation");
                }
            });

            return row;
        });
    }

    // -------------------- MOUSE EVENT: ROOM SELECTION --------------------
    private void setupRoomSelection() {
        roomTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedRoom = newSelection;
                bookingRoomField.setText(newSelection.getName());

                statusLabel.setTextFill(Color.GREEN);
                statusLabel.setText("Selected: " + newSelection.getName() + " (" + newSelection.getStatus() + ")");

                updateBookingPreview();
            }
        });
    }

    // -------------------- KEY EVENT: ENTER TO CONFIRM --------------------
    private void setupKeyEventHandlers() {
        // Press Enter in any field to confirm booking
        if (bookingDatePicker != null) {
            bookingDatePicker.setOnKeyPressed(this::handleEnterKey);
        }
        if (bookingTimeField != null) {
            bookingTimeField.setOnKeyPressed(this::handleEnterKey);
        }
        if (customerNameField != null) {
            customerNameField.setOnKeyPressed(this::handleEnterKey);
        }
        if (notesField != null) {
            notesField.setOnKeyPressed(this::handleEnterKey);
        }
    }

    private void handleEnterKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            reserveRoom();
        }
    }

    // -------------------- TEXT EVENT: LIVE BOOKING PREVIEW --------------------
    private void setupBookingPreview() {
        if (bookingDatePicker != null) {
            bookingDatePicker.valueProperty().addListener((obs, oldV, newV) -> updateBookingPreview());
        }
        if (bookingTimeField != null) {
            bookingTimeField.textProperty().addListener((obs, oldV, newV) -> updateBookingPreview());
        }
        if (customerNameField != null) {
            customerNameField.textProperty().addListener((obs, oldV, newV) -> updateBookingPreview());
        }
    }

    private void updateBookingPreview() {
        if (bookingPreviewLabel != null) {
            String room = bookingRoomField.getText();
            String date = bookingDatePicker.getValue() != null ?
                    bookingDatePicker.getValue().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "Not selected";
            String time = bookingTimeField.getText().isEmpty() ? "Not specified" : bookingTimeField.getText();
            String customer = customerNameField.getText().isEmpty() ? "Not specified" : customerNameField.getText();

            if (room.isEmpty() || room.equals("Select a room from the table")) {
                bookingPreviewLabel.setText("Select a room to start booking");
            } else {
                bookingPreviewLabel.setText(
                        String.format("Booking: %s | Date: %s | Time: %s | Customer: %s",
                                room, date, time, customer)
                );
            }
        }
    }

    // -------------------- COLOR EVENT: HIGHLIGHT ROOMS BY STATUS --------------------
    private void highlightRoomsByStatus() {
        roomTable.setRowFactory(tv -> new TableRow<Room>() {
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);

                if (room == null || empty) {
                    setStyle("");
                } else {
                    // Color Event: Available = Green, Reserved = Red, Pending = Yellow
                    if ("Available".equalsIgnoreCase(room.getStatus())) {
                        setStyle("-fx-background-color: #e8f5e9;"); // Light green
                    } else if ("Reserved".equalsIgnoreCase(room.getStatus())) {
                        setStyle("-fx-background-color: #ffebee;"); // Light red
                    } else if ("Pending".equalsIgnoreCase(room.getStatus())) {
                        setStyle("-fx-background-color: #fff9c4;"); // Light yellow
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    // -------------------- ACTION EVENT: RESERVE ROOM --------------------
    @FXML
    private void reserveRoom() {
        // Validation
        if (selectedRoom == null) {
            showStatus("Please select a room from the table!", Color.RED);
            return;
        }

        if (!selectedRoom.getStatus().equalsIgnoreCase("Available")) {
            showStatus("Reservation Denied! Room is not available.", Color.RED);
            showAlert(Alert.AlertType.ERROR, "Reservation Denied",
                    "The selected room '" + selectedRoom.getName() + "' is not available.");
            return;
        }

        if (bookingDatePicker.getValue() == null) {
            showStatus("Please select a date!", Color.RED);
            return;
        }

        if (bookingTimeField.getText().isEmpty()) {
            showStatus("Please enter a time!", Color.RED);
            return;
        }

        if (customerNameField.getText().isEmpty()) {
            showStatus("Please enter customer name!", Color.RED);
            return;
        }

        // Create reservation
        String username = currentUser != null ? currentUser.getUsername() : customerNameField.getText();
        String roomName = selectedRoom.getName();
        String dateTime = bookingDatePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) +
                " " + bookingTimeField.getText();

        // Update room status
        selectedRoom.setStatus("Reserved");

        // Save reservation
        DataStore.addReservation(username, roomName, dateTime);
        DataStore.saveRooms();
        DataStore.saveReservations();

        // Show success
        showStatus("Reservation Successful!", Color.GREEN);
        showAlert(Alert.AlertType.INFORMATION, "Success",
                "Room '" + roomName + "' has been reserved for " + dateTime);

        // Network Event: Simulate confirmation email
        simulateConfirmationEmail(username, roomName, dateTime);

        // Clear form
        clearBookingForm();
        roomTable.refresh();
    }

    // -------------------- ACTION EVENT: CANCEL RESERVATION --------------------
    @FXML
    private void cancelReservation() {
        if (selectedRoom == null) {
            showStatus("Please select a room to cancel reservation!", Color.RED);
            return;
        }

        if (selectedRoom.getStatus().equalsIgnoreCase("Available")) {
            showStatus("No reservation to cancel.", Color.RED);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Cancel Reservation");
        confirmAlert.setHeaderText("Cancel reservation for: " + selectedRoom.getName());
        confirmAlert.setContentText("Are you sure you want to cancel this reservation?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            selectedRoom.setStatus("Available");
            DataStore.saveRooms();
            DataStore.saveReservations();

            showStatus("Reservation Cancelled!", Color.ORANGE);
            roomTable.refresh();
            clearBookingForm();
        }
    }

    // -------------------- NAVIGATION ACTIONS --------------------
    @FXML
    private void showHome() {
        statusLabel.setText("Home - View all available rooms");
        roomTable.setItems(DataStore.getRooms());
        hideMyReservations();
        clearBookingForm();
    }

    @FXML
    private void openReservationForm() {
        statusLabel.setText("Select a room to reserve from the table below");
        roomTable.requestFocus();
        hideMyReservations();
    }

    @FXML
    private void showReservations() {
        if (currentUser == null) {
            showStatus("User not logged in!", Color.RED);
            return;
        }

        statusLabel.setText("My Reservations:");

        // Filter reservations for current user
        javafx.collections.ObservableList<Reservation> userReservations =
                DataStore.getReservations().filtered(
                        res -> res.getUsername().equals(currentUser.getUsername())
                );

        myReservationsTable.setItems(userReservations);

        // Show the reservations section
        if (myReservationsSection != null) {
            myReservationsSection.setVisible(true);
            myReservationsSection.setManaged(true);
        }

        // Filter room table to show only user's reserved rooms
        roomTable.setItems(DataStore.getRooms().filtered(
                room -> userReservations.stream()
                        .anyMatch(res -> res.getRoomName().equals(room.getName()))
        ));
    }

    @FXML
    private void closeMyReservations() {
        hideMyReservations();
        showHome();
    }

    private void hideMyReservations() {
        if (myReservationsSection != null) {
            myReservationsSection.setVisible(false);
            myReservationsSection.setManaged(false);
        }
    }

    @FXML
    private void refreshRooms() {
        DataStore.loadRooms();
        roomTable.refresh();
        showStatus("Rooms refreshed!", Color.BLUE);
    }

    @FXML
    private void exitApp() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Application");
        alert.setHeaderText("Are you sure you want to exit?");
        alert.setContentText("All your data is saved.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            DataStore.saveAll();
            stopAutoRefresh();
            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.close();
        }
    }

    // -------------------- TIMER EVENT: AUTO-REFRESH --------------------
    private void startAutoRefresh() {
        refreshTask = new TimerTask() {
            public void run() {
                Platform.runLater(() -> {
                    DataStore.loadRooms();
                    roomTable.refresh();
                    System.out.println("[AUTO-REFRESH] Room availability updated at " +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                });
            }
        };
        refreshTimer.schedule(refreshTask, 30000, 30000); // Every 30 seconds
    }

    private void stopAutoRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        refreshTimer.cancel();
    }

    // -------------------- NETWORK EVENT: CONFIRMATION EMAIL --------------------
    private void simulateConfirmationEmail(String username, String roomName, String dateTime) {
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate network delay
                Platform.runLater(() -> {
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
    private void clearBookingForm() {
        selectedRoom = null;
        bookingRoomField.clear();
        bookingDatePicker.setValue(null);
        bookingTimeField.clear();
        notesField.clear();
        bookingPreviewLabel.setText("No booking details yet");
    }

    private void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setTextFill(color);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}