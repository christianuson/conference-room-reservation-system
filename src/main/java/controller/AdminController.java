package controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import model.Reservation;
import model.Room;
import model.User;
import util.DataStore;

import java.io.File;
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
    @FXML private TableColumn<Reservation, String> resStatusColumn;

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
        loadAllData();
        setupRoomTable();
        setupUserTable();
        setupReservationTable();
        attachReservationContextMenu();

        roomTable.setItems(DataStore.getRooms());
        userTable.setItems(DataStore.getUsers());
        reservationTable.setItems(DataStore.getReservations());

        startAutoBackup();
        startAutoRefresh();
        setupReservationDoubleClick();
        setupLiveRoomPreview();
        highlightOverdueReservations();
        setupWindowCloseHandler();

        statusLabel.setText("Admin Dashboard loaded successfully");
    }

    private void loadAllData() {
        DataStore.loadUsers("src/main/resources/data/users.json");
        DataStore.loadRooms();
        DataStore.loadReservations();
        System.out.println("[ADMIN] All data loaded from JSON files");
    }

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
        if (resUsernameColumn == null) {
            resUsernameColumn = new TableColumn<>("Username");
        }
        resUsernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));

        if (resRoomColumn == null) {
            resRoomColumn = new TableColumn<>("Room");
        }
        resRoomColumn.setCellValueFactory(new PropertyValueFactory<>("roomName"));

        if (resDateColumn == null) {
            resDateColumn = new TableColumn<>("Date & Time");
        }
        resDateColumn.setCellValueFactory(cd -> {
            Reservation r = cd.getValue();
            String d  = r.getDate() == null ? "" : r.getDate();
            String st = r.getStartTime() == null ? "" : r.getStartTime();
            String en = r.getEndTime()   == null ? "" : r.getEndTime();
            String combined = d;
            if (!st.isEmpty() && !en.isEmpty()) combined += " " + st + " - " + en;
            return new javafx.beans.property.SimpleStringProperty(combined.trim());
        });

        if (resStatusColumn == null) {
            resStatusColumn = new TableColumn<>("Status");
        }
        resStatusColumn.setCellValueFactory(cd -> {
            String raw = cd.getValue().getStatus();
            String nice = (raw == null || raw.isBlank()) ? "Pending"
                    : raw.substring(0,1).toUpperCase() + raw.substring(1).toLowerCase();
            return new javafx.beans.property.SimpleStringProperty(nice);
        });

        if (reservationTable.getColumns().isEmpty()) {
            reservationTable.getColumns().addAll(
                    resUsernameColumn, resRoomColumn, resDateColumn, resStatusColumn
            );
        }
    }

    private void attachReservationContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem approve = new MenuItem("Approve Reservation");
        approve.setOnAction(e -> {
            Reservation sel = reservationTable.getSelectionModel().getSelectedItem();
            if (sel == null) return;

            if (!"Pending".equalsIgnoreCase(sel.getStatus())) {
                statusLabel.setTextFill(Color.RED);
                statusLabel.setText("Only 'Pending' reservations can be approved.");
                return;
            }
            DataStore.updateReservationStatus(sel, "Reserved");
            DataStore.reloadAll();
            reservationTable.refresh();
            roomTable.refresh();
            statusLabel.setTextFill(Color.GREEN);
            statusLabel.setText("Reservation approved.");
        });

        MenuItem reject = new MenuItem("Reject / Cancel Reservation");
        reject.setOnAction(e -> {
            Reservation sel = reservationTable.getSelectionModel().getSelectedItem();
            if (sel == null) return;

            if (!"Pending".equalsIgnoreCase(sel.getStatus())) {
                statusLabel.setTextFill(Color.RED);
                statusLabel.setText("Only 'Pending' reservations can be rejected.");
                return;
            }
            DataStore.updateReservationStatus(sel, "Rejected");
            DataStore.reloadAll();
            reservationTable.refresh();
            roomTable.refresh();
            statusLabel.setTextFill(Color.ORANGE);
            statusLabel.setText("Reservation rejected.");
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

    @FXML
    private void approveSelectedReservation() {
        Reservation sel = reservationTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Select a reservation to approve.");
            return;
        }
        if (!"pending".equalsIgnoreCase(sel.getStatus())) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Only 'Pending' reservations can be approved.");
            return;
        }

        DataStore.updateReservationStatus(sel, "approved");
        reservationTable.refresh();
        statusLabel.setTextFill(Color.GREEN);
        statusLabel.setText("Reservation approved.");

        // Send approval email to user
        User user = DataStore.getUserByEmail(sel.getUsername() + "@example.com");
        if (user != null) {
            util.EmailService.getInstance().sendReservationApproval(user, sel);
            System.out.println("[EMAIL] Approval email sent for reservation of " + sel.getRoomName());
        }
    }

    @FXML
    private void rejectSelectedReservation() {
        Reservation sel = reservationTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Select a reservation to reject.");
            return;
        }
        DataStore.updateReservationStatus(sel, "rejected");
        reservationTable.refresh();
        statusLabel.setTextFill(Color.ORANGE);
        statusLabel.setText("Reservation rejected.");

        // Send rejection email to user
        User user = DataStore.getUserByEmail(sel.getUsername() + "@example.com");
        if (user != null) {
            util.EmailService.getInstance().sendReservationRejection(user, sel);
            System.out.println("[EMAIL] Rejection email sent for reservation of " + sel.getRoomName());
        }
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

        // NEW: Image selection
        Label imageLabel = new Label("No image selected");
        imageLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
        final String[] selectedImagePath = {null};

        Button browseButton = new Button("Browse Image...");
        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Room Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
            );

            File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
            if (selectedFile != null) {
                selectedImagePath[0] = selectedFile.getAbsolutePath();
                imageLabel.setText(selectedFile.getName());
                imageLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            }
        });

        nameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                ((Button) dialog.getDialogPane().lookupButton(addButtonType)).fire();
            }
        });

        grid.add(new Label("Room Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusBox, 1, 1);
        grid.add(new Label("Room Image:"), 0, 2);
        grid.add(browseButton, 1, 2);
        grid.add(imageLabel, 1, 3);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return new Room(nameField.getText(), statusBox.getValue(), selectedImagePath[0]);
            }
            return null;
        });

        Optional<Room> result = dialog.showAndWait();
        result.ifPresent(room -> {
            DataStore.addRoom(room);
            statusLabel.setTextFill(Color.GREEN);
            statusLabel.setText("Room '" + room.getName() + "' added successfully!");
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

        // NEW: Image selection for editing
        Label imageLabel = new Label(selected.getImagePath() != null ?
                new File(selected.getImagePath()).getName() : "No image selected");
        imageLabel.setStyle(selected.getImagePath() != null ?
                "-fx-text-fill: #4CAF50; -fx-font-weight: bold;" :
                "-fx-text-fill: #666; -fx-font-style: italic;");
        final String[] selectedImagePath = {selected.getImagePath()};

        Button browseButton = new Button("Change Image...");
        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Room Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
            );

            File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
            if (selectedFile != null) {
                selectedImagePath[0] = selectedFile.getAbsolutePath();
                imageLabel.setText(selectedFile.getName());
                imageLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            }
        });

        Button clearImageButton = new Button("Clear Image");
        clearImageButton.setOnAction(e -> {
            selectedImagePath[0] = null;
            imageLabel.setText("No image selected");
            imageLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
        });

        HBox imageButtons = new HBox(10, browseButton, clearImageButton);

        nameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                ((Button) dialog.getDialogPane().lookupButton(saveButtonType)).fire();
            }
        });

        grid.add(new Label("Room Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusBox, 1, 1);
        grid.add(new Label("Room Image:"), 0, 2);
        grid.add(imageButtons, 1, 2);
        grid.add(imageLabel, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                selected.setName(nameField.getText());
                selected.setStatus(statusBox.getValue());
                selected.setImagePath(selectedImagePath[0]);
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
            simulateGitHubSync("DELETE_ROOM", selected.getName());
        }
    }

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
            DataStore.deleteUser(selected);                 // perform actual DB delete
            DataStore.reloadAll();                          // reload lists from MySQL
            userTable.setItems(DataStore.getUsers());       // rebind updated list
            userTable.refresh();                            // force visual refresh
            statusLabel.setTextFill(Color.ORANGE);
            statusLabel.setText("User '" + selected.getUsername() + "' removed from database.");
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

    private void setupLiveRoomPreview() {
        if (roomNameField != null) {
            roomNameField.textProperty().addListener((obs, o, n) -> updateRoomPreview());
        }
        if (roomStatusField != null) {
            roomStatusField.valueProperty().addListener((obs, o, n) -> updateRoomPreview());
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

    private void highlightOverdueReservations() {
        reservationTable.setRowFactory(tv -> new TableRow<Reservation>() {
            @Override
            protected void updateItem(Reservation item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setStyle("");
                } else {
                    if (item.getDate() != null && item.getDate().contains("2024")) {
                        setStyle("-fx-background-color: orange; -fx-text-fill: white;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

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
        backupTimer.schedule(backupTask, 5000, 30000);
    }

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
        refreshTimer.schedule(refreshTask, 30000, 30000);
    }

    private void simulateGitHubSync(String action, String data) {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
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

    private void setupWindowCloseHandler() {
        Platform.runLater(() -> {
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setOnCloseRequest(this::handleWindowClose);
        });
    }

    private void handleWindowClose(WindowEvent event) {
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
                DataStore.saveAll();

                if (backupTask != null) backupTask.cancel();
                if (refreshTask != null) refreshTask.cancel();
                backupTimer.cancel();
                refreshTimer.cancel();

                javafx.fxml.FXMLLoader loader =
                        new javafx.fxml.FXMLLoader(getClass().getResource("/view/login.fxml"));
                javafx.scene.Parent loginRoot = loader.load();

                Stage stage = (Stage) statusLabel.getScene().getWindow();
                stage.setTitle("Login");
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