package controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import model.Reservation;
import model.Room;
import model.User;
import util.DataStore;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class AdminController {
    @FXML private TableView<Room> roomTable;
    @FXML private TableColumn<Room, String> roomNameColumn;
    @FXML private TableColumn<Room, String> roomStatusColumn;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> userNameColumn;
    @FXML private TableColumn<User, String> userEmailColumn;
    @FXML private TableColumn<User, String> userRoleColumn;

    @FXML private TableView<Reservation> reservationTable;
    @FXML private TableColumn<Reservation, String> resUsernameColumn;
    @FXML private TableColumn<Reservation, String> resRoomColumn;
    @FXML private TableColumn<Reservation, String> resDateColumn;

    @FXML private Label statusLabel;
    @FXML private TextField roomNameField;
    @FXML private ComboBox<String> roomStatusField;
    @FXML private Label roomPreviewLabel;

    private final Timer backupTimer = new Timer(true);
    private final Timer refreshTimer = new Timer(true);
    private TimerTask backupTask;
    private TimerTask refreshTask;

    @FXML
    public void initialize() {
        // Window Event: Load data from JSON on open
        loadAllData();

        // Setup table columns
        setupRoomTable();
        setupUserTable();
        setupReservationTable();

        // attach right-click Approve/Reject on reservations
        attachReservationContextMenu();

        // Set observable data for tables
        roomTable.setItems(DataStore.getRooms());
        userTable.setItems(DataStore.getUsers());
        reservationTable.setItems(DataStore.getReservations());

        // Timer Event: Auto-backup every 30 seconds
        startAutoBackup();

        // Timer Event: Auto-refresh dashboard data every 30 seconds
        startAutoRefresh();

        // Mouse Event: Double-click reservation to open detail window
        setupReservationDoubleClick();

        // Text Event: Live preview of room details
        setupLiveRoomPreview();

        // Color Event: Highlight overdue reservations
        highlightOverdueReservations();

        // Window Event: Setup close handler
        setupWindowCloseHandler();

        statusLabel.setText("Admin Dashboard loaded successfully");
    }

    // -------------------- WINDOW EVENT: LOAD DATA --------------------
    private void loadAllData() {
        DataStore.loadUsers("src/main/resources/data/users.json");
        DataStore.loadRooms();
        DataStore.loadReservations();
        System.out.println("[ADMIN] All data loaded from JSON files");
    }

    // -------------------- TABLE SETUP --------------------
    private void setupRoomTable() {
        roomNameColumn = new TableColumn<>("Room Name");
        roomNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        roomStatusColumn = new TableColumn<>("Status");
        roomStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        if (roomTable.getColumns().isEmpty()) {
            roomTable.getColumns().addAll(roomNameColumn, roomStatusColumn);
        }
    }

    private void setupUserTable() {
        userNameColumn = new TableColumn<>("Username");
        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));

        userEmailColumn = new TableColumn<>("Email");
        userEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        userRoleColumn = new TableColumn<>("Role");
        userRoleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

        if (userTable.getColumns().isEmpty()) {
            userTable.getColumns().addAll(userNameColumn, userEmailColumn, userRoleColumn);
        }
    }

    private void setupReservationTable() {
        resUsernameColumn = new TableColumn<>("Username");
        resUsernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));

        resRoomColumn = new TableColumn<>("Room");
        resRoomColumn.setCellValueFactory(new PropertyValueFactory<>("roomName"));

        resDateColumn = new TableColumn<>("Date");
        resDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        if (reservationTable.getColumns().isEmpty()) {
            reservationTable.getColumns().addAll(resUsernameColumn, resRoomColumn, resDateColumn);
        }
    }

    // -------------------- ACTION EVENT: ROOM MANAGEMENT --------------------

    private void attachReservationContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem approve = new MenuItem("Approve Reservation");
        approve.setOnAction(e -> {
            Reservation sel = reservationTable.getSelectionModel().getSelectedItem();
            if (sel == null) return;

            Room r = DataStore.getRoomByName(sel.getRoomName());
            if (r == null) {
                statusLabel.setTextFill(Color.RED);
                statusLabel.setText("Room not found for this reservation.");
                return;
            }
            // Only approve if currently Pending
            if (!"Pending".equalsIgnoreCase(r.getStatus())) {
                statusLabel.setTextFill(Color.RED);
                statusLabel.setText("Only 'Pending' reservations can be approved.");
                return;
            }

            r.setStatus("Reserved");
            DataStore.updateRoom(r);
            roomTable.refresh();
            reservationTable.refresh();
            statusLabel.setTextFill(Color.GREEN);
            statusLabel.setText("Reservation approved. Room set to RESERVED.");
        });

        MenuItem reject = new MenuItem("Reject / Cancel Reservation");
        reject.setOnAction(e -> {
            Reservation sel = reservationTable.getSelectionModel().getSelectedItem();
            if (sel == null) return;

            // Set room back to Available and remove reservation
            Room r = DataStore.getRoomByName(sel.getRoomName());
            if (r != null) {
                r.setStatus("Available");
                DataStore.updateRoom(r);
            }
            DataStore.deleteReservation(sel);

            roomTable.refresh();
            reservationTable.getItems().remove(sel);
            reservationTable.refresh();

            statusLabel.setTextFill(Color.ORANGE);
            statusLabel.setText("Reservation rejected/cancelled. Room set to AVAILABLE.");
        });

        menu.getItems().addAll(approve, reject);

        reservationTable.setRowFactory(tv -> {
            TableRow<Reservation> row = new TableRow<>();
            row.setOnContextMenuRequested(evt -> {
                if (!row.isEmpty()) {
                    reservationTable.getSelectionModel().select(row.getItem());
                    menu.show(row, evt.getScreenX(), evt.getScreenY());
                }
            });
            // Hide on left click
            row.setOnMouseClicked(evt -> menu.hide());
            return row;
        });
    }

    private ComboBox<String> buildStatusBox(String initial) {
        ComboBox<String> box = new ComboBox<>(
                FXCollections.observableArrayList("Available", "Reserved", "Pending")
        );
        box.setEditable(false);
        box.setValue(initial != null ? initial : "Available");
        return box;
    }

    // Approve: only if the room tied to the selected reservation is currently Pending
    @FXML
    private void approveSelectedReservation() {
        Reservation sel = reservationTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Select a reservation to approve.");
            return;
        }

        Room r = DataStore.getRoomByName(sel.getRoomName());
        if (r == null) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Room not found for this reservation.");
            return;
        }

        if (!"Pending".equalsIgnoreCase(r.getStatus())) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Only 'Pending' reservations can be approved.");
            return;
        }

        r.setStatus("Reserved");
        DataStore.updateRoom(r);

        // refresh views
        roomTable.refresh();
        reservationTable.refresh();

        statusLabel.setTextFill(Color.GREEN);
        statusLabel.setText("Reservation approved. Room set to RESERVED.");
    }

    // Reject/Cancel: return room to Available and remove the reservation record
    @FXML
    private void rejectSelectedReservation() {
        Reservation sel = reservationTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Select a reservation to reject/cancel.");
            return;
        }

        Room r = DataStore.getRoomByName(sel.getRoomName());
        if (r != null) {
            r.setStatus("Available");
            DataStore.updateRoom(r);
        }

        DataStore.deleteReservation(sel);

        // update tables
        reservationTable.getItems().remove(sel);
        reservationTable.refresh();
        roomTable.refresh();

        statusLabel.setTextFill(Color.ORANGE);
        statusLabel.setText("Reservation rejected/cancelled. Room set to AVAILABLE.");
    }

    @FXML
    private void addRoom() {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Add New Room");
        dialog.setHeaderText("Enter room details");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Room Name");
        ComboBox<String> statusBox = buildStatusBox("Available");
        TextField statusField = new TextField("Available");
        statusField.setPromptText("Status");

        // Key Event: Press Enter to confirm
        nameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                ((Button) dialog.getDialogPane().lookupButton(addButtonType)).fire();
            }
        });

        grid.add(new Label("Room Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusBox, 1, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return new Room(nameField.getText(), statusBox.getValue());  // << use box value
            }
            return null;
        });

        Optional<Room> result = dialog.showAndWait();
        result.ifPresent(room -> {
            DataStore.addRoom(room);
            statusLabel.setTextFill(Color.GREEN);
            statusLabel.setText("Room '" + room.getName() + "' added successfully!");

            // Network Event: Simulate data sync
            simulateGitHubSync("ADD_ROOM", room.getName());
        });
    }

    @FXML
    private void editRoom() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("No room selected for editing.");
            return;
        }

        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Edit Room");
        dialog.setHeaderText("Edit room details for: " + selected.getName());

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(selected.getName());
        ComboBox<String> statusBox = buildStatusBox(selected.getStatus());

        // Key Event: Press Enter to confirm edits
        nameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                nameField.requestFocus();
            }
        });

        nameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                ((Button) dialog.getDialogPane().lookupButton(saveButtonType)).fire();
            }
        });

        grid.add(new Label("Room Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusBox, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                selected.setName(nameField.getText());
                selected.setStatus(statusBox.getValue()); // << use box value
                return selected;
            }
            return null;
        });

        Optional<Room> result = dialog.showAndWait();
        result.ifPresent(room -> {
            DataStore.saveRooms();
            roomTable.refresh();
            statusLabel.setTextFill(Color.GREEN);
            statusLabel.setText("Room updated successfully!");

            // Network Event: Simulate data sync
            simulateGitHubSync("EDIT_ROOM", room.getName());
        });
    }

    @FXML
    private void deleteRoom() {
        Room selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("No room selected for deletion.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Room");
        alert.setHeaderText("Delete room: " + selected.getName());
        alert.setContentText("Are you sure you want to delete this room?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            DataStore.removeRoom(selected);
            statusLabel.setTextFill(Color.ORANGE);
            statusLabel.setText("Room '" + selected.getName() + "' deleted.");

            // Network Event: Simulate data sync
            simulateGitHubSync("DELETE_ROOM", selected.getName());
        }
    }

    // -------------------- ACTION EVENT: USER MANAGEMENT --------------------
    @FXML
    private void removeUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("No user selected for removal.");
            return;
        }

        if ("admin".equalsIgnoreCase(selected.getRole())) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Cannot remove admin users!");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove User");
        alert.setHeaderText("Remove user: " + selected.getUsername());
        alert.setContentText("Are you sure you want to remove this user?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            DataStore.getUsers().remove(selected);
            DataStore.saveUsers();
            statusLabel.setTextFill(Color.ORANGE);
            statusLabel.setText("User '" + selected.getUsername() + "' removed.");

            // Network Event: Simulate data sync
            simulateGitHubSync("REMOVE_USER", selected.getEmail());
        }
    }

    @FXML
    private void approveUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("No user selected for approval.");
            return;
        }

        selected.setRole("approved_user");
        DataStore.saveUsers();
        userTable.refresh();
        statusLabel.setTextFill(Color.GREEN);
        statusLabel.setText("User '" + selected.getUsername() + "' approved!");
    }

    // -------------------- MOUSE EVENT: DOUBLE-CLICK RESERVATION --------------------
    private void setupReservationDoubleClick() {
        reservationTable.setRowFactory(tv -> {
            TableRow<Reservation> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2) {
                    Reservation clickedReservation = row.getItem();
                    openReservationDetailWindow(clickedReservation);
                }
            });
            return row;
        });
    }

    private void openReservationDetailWindow(Reservation reservation) {
        Stage detailStage = new Stage();
        detailStage.initModality(Modality.APPLICATION_MODAL);
        detailStage.setTitle("Reservation Details");

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Reservation Details");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label userLabel = new Label("User: " + reservation.getUsername());
        userLabel.setStyle("-fx-font-size: 14px;");

        Label roomLabel = new Label("Room: " + reservation.getRoomName());
        roomLabel.setStyle("-fx-font-size: 14px;");

        Label dateLabel = new Label("Date: " + reservation.getDate());
        dateLabel.setStyle("-fx-font-size: 14px;");

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> detailStage.close());
        closeButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");

        vbox.getChildren().addAll(titleLabel, userLabel, roomLabel, dateLabel, closeButton);

        Scene scene = new Scene(vbox, 350, 250);
        detailStage.setScene(scene);
        detailStage.show();

        System.out.println("[ADMIN] Opened detail window for reservation: " +
                reservation.getUsername() + " - " + reservation.getRoomName());
    }

    // -------------------- TEXT EVENT: LIVE ROOM PREVIEW --------------------
    private void setupLiveRoomPreview() {
        if (roomNameField != null) {
            roomNameField.textProperty().addListener((obs, o, n) -> updateRoomPreview());
        }
        if (roomStatusField != null) {
            roomStatusField.valueProperty().addListener((obs, o, n) -> updateRoomPreview());
            // ensure options exist even if not set in FXML
            if (roomStatusField.getItems().isEmpty()) {
                roomStatusField.setItems(FXCollections.observableArrayList("Available","Reserved","Pending"));
            }
            if (roomStatusField.getValue() == null) roomStatusField.setValue("Available");
        }
    }

    private void updateRoomPreview() {
        if (roomPreviewLabel != null) {
            String name = roomNameField != null ? roomNameField.getText() : "";
            String status = (roomStatusField != null && roomStatusField.getValue() != null)
                    ? roomStatusField.getValue() : "";
            roomPreviewLabel.setText("Preview: " + name + " - " + status);
            roomPreviewLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");
        }
    }

    // -------------------- COLOR EVENT: HIGHLIGHT OVERDUE RESERVATIONS --------------------
    private void highlightOverdueReservations() {
        reservationTable.setRowFactory(tv -> new TableRow<Reservation>() {
            @Override
            protected void updateItem(Reservation item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setStyle("");
                } else {
                    // Simulate overdue logic: if date contains "2024" or earlier
                    if (item.getDate() != null && item.getDate().contains("2024")) {
                        setStyle("-fx-background-color: orange; -fx-text-fill: white;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    // -------------------- TIMER EVENT: AUTO-BACKUP --------------------
    private void startAutoBackup() {
        backupTask = new TimerTask() {
            public void run() {
                DataStore.saveAll();
                Platform.runLater(() -> {
                    System.out.println("[AUTO-BACKUP] Complete at " +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                });
            }
        };
        backupTimer.schedule(backupTask, 5000, 30000); // First backup after 5s, then every 30s
    }

    // -------------------- TIMER EVENT: AUTO-REFRESH --------------------
    private void startAutoRefresh() {
        refreshTask = new TimerTask() {
            public void run() {
                Platform.runLater(() -> {
                    DataStore.reloadAll();
                    roomTable.refresh();
                    userTable.refresh();
                    reservationTable.refresh();
                    System.out.println("[AUTO-REFRESH] Dashboard data refreshed at " +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                });
            }
        };
        refreshTimer.schedule(refreshTask, 30000, 30000); // Every 30 seconds
    }

    // -------------------- NETWORK EVENT: SIMULATE GITHUB SYNC --------------------
    private void simulateGitHubSync(String action, String data) {
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate network delay
                Platform.runLater(() -> {
                    System.out.println("[GITHUB SYNC] POST " + action + ": " + data);
                    System.out.println("[GITHUB SYNC] Status: Success (200 OK)");
                    System.out.println("[GITHUB SYNC] Timestamp: " +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // -------------------- REPORTS --------------------
    @FXML
    private void generateTextReport() {
        try {
            String filename = "report_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
            FileWriter writer = new FileWriter(filename);

            writer.write("===== ADMIN DASHBOARD REPORT =====\n");
            writer.write("Generated: " + LocalDateTime.now() + "\n\n");

            writer.write("--- ROOMS ---\n");
            for (Room room : DataStore.getRooms()) {
                writer.write("Room: " + room.getName() + " | Status: " + room.getStatus() + "\n");
            }

            writer.write("\n--- USERS ---\n");
            for (User user : DataStore.getUsers()) {
                writer.write("User: " + user.getUsername() + " | Email: " +
                        user.getEmail() + " | Role: " + user.getRole() + "\n");
            }

            writer.write("\n--- RESERVATIONS ---\n");
            for (Reservation res : DataStore.getReservations()) {
                writer.write("User: " + res.getUsername() + " | Room: " +
                        res.getRoomName() + " | Date: " + res.getDate() + "\n");
            }

            writer.close();

            statusLabel.setTextFill(Color.GREEN);
            statusLabel.setText("Text report generated: " + filename);
        } catch (IOException e) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Error generating report: " + e.getMessage());
        }
    }

    @FXML
    private void generateJsonReport() {
        try {
            String filename = "report_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";
            FileWriter writer = new FileWriter(filename);

            writer.write("{\n");
            writer.write("  \"generated\": \"" + LocalDateTime.now() + "\",\n");
            writer.write("  \"total_rooms\": " + DataStore.getRooms().size() + ",\n");
            writer.write("  \"total_users\": " + DataStore.getUsers().size() + ",\n");
            writer.write("  \"total_reservations\": " + DataStore.getReservations().size() + "\n");
            writer.write("}\n");

            writer.close();

            statusLabel.setTextFill(Color.GREEN);
            statusLabel.setText("JSON report generated: " + filename);
        } catch (IOException e) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Error generating report: " + e.getMessage());
        }
    }

    // -------------------- WINDOW EVENT: CLOSE HANDLER --------------------
    private void setupWindowCloseHandler() {
        Platform.runLater(() -> {
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setOnCloseRequest(this::handleWindowClose);
        });
    }

    private void handleWindowClose(WindowEvent event) {
        // Save all data and logs
        DataStore.saveAll();

        try {
            String logFile = "admin_log_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".txt";
            FileWriter writer = new FileWriter(logFile, true);
            writer.write("[" + LocalDateTime.now() + "] Admin session ended\n");
            writer.close();
            System.out.println("[ADMIN] Session log saved: " + logFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Cancel timers
        if (backupTask != null) backupTask.cancel();
        if (refreshTask != null) refreshTask.cancel();
        backupTimer.cancel();
        refreshTimer.cancel();

        System.out.println("[ADMIN] Dashboard closed, all data saved");
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("All data will be saved.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Save current data
                DataStore.saveAll();

                // Stop background timers when leaving this dashboard
                if (backupTask != null) backupTask.cancel();
                if (refreshTask != null) refreshTask.cancel();
                backupTimer.cancel();
                refreshTimer.cancel();

                // Load login screen
                javafx.fxml.FXMLLoader loader =
                        new javafx.fxml.FXMLLoader(getClass().getResource("/view/login.fxml"));
                javafx.scene.Parent loginRoot = loader.load();

                // Reuse the same Stage (window)
                Stage stage = (Stage) statusLabel.getScene().getWindow();
                stage.setTitle("Login");
                // Keep current window size; or set a fixed size if you prefer:
                stage.setWidth(540); stage.setHeight(570);
                stage.setScene(new javafx.scene.Scene(loginRoot));
                stage.centerOnScreen();
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
                statusLabel.setTextFill(javafx.scene.paint.Color.RED);
                statusLabel.setText("Failed to return to login: " + e.getMessage());
            }
        }
    }
}