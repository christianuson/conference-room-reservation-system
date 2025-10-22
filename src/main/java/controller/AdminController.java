package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Room;
import model.User;
import util.DataStore;

import java.util.Timer;
import java.util.TimerTask;

public class AdminController {
    @FXML private TableView<Room> roomTable;
    @FXML private TableView<User> userTable;
    @FXML private Label statusLabel;

    private final Timer backupTimer = new Timer();

    @FXML
    public void initialize() {
        // Load data from JSON
        DataStore.loadRooms();
        DataStore.loadUsers("src/main/resources/data/users.json");

        // Set observable data for tables
        roomTable.setItems(DataStore.getRooms());
        userTable.setItems(DataStore.getUsers());

        startAutoBackup();
    }

    // -------------------- ROOM MANAGEMENT --------------------
    @FXML
    private void addRoom() {
        Room newRoom = new Room("Room X", "Available");
        DataStore.addRoom(newRoom);
        statusLabel.setText("Room Added!");
    }

    @FXML
    private void deleteRoom() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            DataStore.removeRoom(selected);
            statusLabel.setText("Room Deleted.");
        } else {
            statusLabel.setText("No room selected.");
        }
    }

    // -------------------- AUTO BACKUP --------------------
    private void startAutoBackup() {
        backupTimer.schedule(new TimerTask() {
            public void run() {
                DataStore.saveAll();
                System.out.println("Auto-backup complete (users, rooms, reservations).");
            }
        }, 0, 30000); // every 30 seconds
    }
}
