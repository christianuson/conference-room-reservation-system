package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import model.Reservation;
import model.Room;
import model.User;
import util.DataStore;

import java.util.Timer;
import java.util.TimerTask;

public class UserController {

    @FXML private TableView<Room> roomTable;
    @FXML private Button reserveButton, cancelButton;
    @FXML private Label statusLabel;
    @FXML private BorderPane mainPane;

    private Timer refreshTimer = new Timer();
    private User currentUser;

    @FXML
    public void initialize() {
        // Load persisted data
        DataStore.loadUsers("src/main/resources/data/users.json");
        DataStore.loadRooms();
        DataStore.loadReservations();

        // Bind rooms to TableView
        roomTable.setItems(DataStore.getRooms());
        startAutoRefresh();

        // Show room info on hover
        roomTable.setRowFactory(tv -> {
            TableRow<Room> row = new TableRow<>();
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) {
                    statusLabel.setText("Room: " + row.getItem().getName() + " - " + row.getItem().getStatus());
                }
            });
            return row;
        });
    }

    // Optional setter to identify which user is logged in
    public void setCurrentUser(User user) {
        this.currentUser = user;
        statusLabel.setText("Welcome, " + user.getUsername() + "!");
    }

    /*** Room Reservation Logic ***/
    @FXML
    private void reserveRoom() {
        Room room = roomTable.getSelectionModel().getSelectedItem();

        if (room == null || !room.getStatus().equals("Available")) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Reservation Denied!");
            return;
        }

        room.setStatus("Reserved");
        Reservation reservation = new Reservation(
                currentUser != null ? currentUser.getUsername() : "Guest",
                room.getName(),
                "Reserved"
        );

        DataStore.addReservation(reservation.getUsername(), reservation.getRoomName(), reservation.getDate());
        DataStore.saveRooms();
        DataStore.saveReservations();

        statusLabel.setTextFill(Color.GREEN);
        statusLabel.setText("Reservation Successful!");
    }

    @FXML
    private void cancelReservation() {
        Room room = roomTable.getSelectionModel().getSelectedItem();

        if (room == null || room.getStatus().equals("Available")) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("No reservation to cancel.");
            return;
        }

        room.setStatus("Available");
        DataStore.saveRooms();
        DataStore.saveReservations();

        statusLabel.setTextFill(Color.ORANGE);
        statusLabel.setText("Reservation Cancelled!");
    }

    /*** Toolbar Button Actions ***/
    @FXML
    private void showHome() {
        statusLabel.setText("Home Page");
        // Optionally reset the TableView or show default content
        roomTable.setItems(DataStore.getRooms());
    }

    @FXML
    private void openReservationForm() {
        statusLabel.setText("Select a room to reserve from the table below.");
        // Focus the TableView for reserving
        roomTable.requestFocus();
    }

    @FXML
    private void showReservations() {
        statusLabel.setText("My Reservations:");
        // Filter TableView to only show reservations of current user
        roomTable.setItems(DataStore.getRooms().filtered(
                room -> DataStore.getReservations().stream()
                        .anyMatch(res -> res.getUsername().equals(currentUser.getUsername())
                                && res.getRoomName().equals(room.getName()))
        ));
    }

    @FXML
    private void exitApp() {
        // Close the app gracefully
        Stage stage = (Stage) mainPane.getScene().getWindow();
        stage.close();
    }

    /*** Auto-refresh rooms every 30 seconds ***/
    private void startAutoRefresh() {
        refreshTimer.schedule(new TimerTask() {
            public void run() {
                DataStore.loadRooms();
            }
        }, 0, 30000);
    }
}
