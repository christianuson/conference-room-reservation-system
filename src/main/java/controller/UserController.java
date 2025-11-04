package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Reservation;
import model.Room;
import model.User;
import util.DataStore;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class UserController {

    @FXML private BorderPane mainPane;
    @FXML private FlowPane roomCardsContainer;
    @FXML private TextField searchField;
    @FXML private TableView<Reservation> myReservationsTable;
    @FXML private TableColumn<Reservation, String> myResRoomColumn;
    @FXML private TableColumn<Reservation, String> myResDateColumn;
    @FXML private TableColumn<Reservation, String> myResStatusColumn;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;
    @FXML private Label welcomeLabel;
    @FXML private VBox myReservationsSection;

    private Timer refreshTimer = new Timer(true);
    private TimerTask refreshTask;
    private User currentUser;
    private Room selectedRoom;

    @FXML
    public void initialize() {
        // Initialize database
        DataStore.initialize();

        // Setup my reservations table
        setupMyReservationsTable();

        // Load and display room cards
        loadRoomCards();

        // Timer Event: Auto-refresh availability every 30 seconds
        startAutoRefresh();

        // Text Event: Search functionality
        setupSearchField();

        statusLabel.setText("Ready to make a reservation");
    }

    // -------------------- ROOM CARDS SETUP --------------------
    private void loadRoomCards() {
        roomCardsContainer.getChildren().clear();
        javafx.collections.ObservableList<Room> rooms = DataStore.getRooms();

        for (Room room : rooms) {
            VBox card = createRoomCard(room);
            roomCardsContainer.getChildren().add(card);
        }
    }

    private VBox createRoomCard(Room room) {
        VBox card = new VBox(10);
        card.setPrefSize(200, 250);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(15));
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #ddd; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"
        );

        // Placeholder image container
        VBox imageContainer = new VBox();
        imageContainer.setPrefSize(170, 120);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setStyle(
                "-fx-background-color: #e0e0e0; " +
                        "-fx-border-color: #999; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5;"
        );

        Label imagePlaceholder = new Label("📷");
        imagePlaceholder.setStyle("-fx-font-size: 40px; -fx-text-fill: #666;");
        imageContainer.getChildren().add(imagePlaceholder);

        // Room name
        Label nameLabel = new Label(room.getName());
        nameLabel.setStyle(
                "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #333;"
        );
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);

        // Status label with color coding
        String effective = DataStore.computeRoomStatusNow(room.getName());
        Label statusBadge = new Label(effective);
        statusBadge.setPadding(new Insets(5, 15, 5, 15));
        statusBadge.setStyle(getStatusStyle(effective));

        // Add components to card
        card.getChildren().addAll(imageContainer, nameLabel, statusBadge);

        // Mouse Events: Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: #f5f5f5; " +
                            "-fx-border-color: #2196F3; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 10; " +
                            "-fx-background-radius: 10; " +
                            "-fx-effect: dropshadow(gaussian, rgba(33,150,243,0.3), 10, 0, 0, 3); " +
                            "-fx-cursor: hand;"
            );
            statusLabel.setTextFill(Color.BLUE);
            statusLabel.setText("Room: " + room.getName());
        });

        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-border-color: #ddd; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 10; " +
                            "-fx-background-radius: 10; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"
            );
            if (selectedRoom == null) {
                statusLabel.setTextFill(Color.BLACK);
                statusLabel.setText("Ready to make a reservation");
            }
        });

        // Mouse Event: Double-click to open reservation form
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                openReservationForm(room);
            } else if (e.getClickCount() == 1) {
                selectedRoom = room;
                statusLabel.setTextFill(Color.GREEN);
                statusLabel.setText("Selected: " + room.getName());
            }
        });

        return card;
    }

    private String getStatusStyle(String status) {
        String baseStyle = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-border-radius: 15; -fx-background-radius: 15;";

        if ("Available".equalsIgnoreCase(status)) {
            return baseStyle + "-fx-background-color: #4CAF50; -fx-text-fill: white;";
        } else if ("Occupied".equalsIgnoreCase(status)) {
            return baseStyle + "-fx-background-color: #f44336; -fx-text-fill: white;";
        } else if ("Pending".equalsIgnoreCase(status)) {
            return baseStyle + "-fx-background-color: #FFC107; -fx-text-fill: white;";
        }
        return baseStyle + "-fx-background-color: #999; -fx-text-fill: white;";
    }

    // -------------------- SEARCH FUNCTIONALITY --------------------
    private void setupSearchField() {
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            filterRoomCards(newV.toLowerCase());
        });
    }

    private void filterRoomCards(String searchTerm) {
        roomCardsContainer.getChildren().clear();
        javafx.collections.ObservableList<Room> rooms = DataStore.getRooms();

        for (Room room : rooms) {
            if (searchTerm.isEmpty() ||
                    room.getName().toLowerCase().contains(searchTerm) ||
                    room.getStatus().toLowerCase().contains(searchTerm)) {
                VBox card = createRoomCard(room);
                roomCardsContainer.getChildren().add(card);
            }
        }

        if (roomCardsContainer.getChildren().isEmpty()) {
            Label noResults = new Label("No rooms found matching '" + searchTerm + "'");
            noResults.setStyle("-fx-font-size: 14px; -fx-text-fill: #999; -fx-padding: 20;");
            roomCardsContainer.getChildren().add(noResults);
        }
    }

    // -------------------- RESERVATION FORM --------------------
    private void openReservationForm(Room room) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/reservation_form.fxml"));
            Parent root = loader.load();

            ReservationFormController controller = loader.getController();
            controller.setRoom(room);
            controller.setCurrentUser(currentUser);
            controller.setParentController(this);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Reserve Room: " + room.getName());
            stage.setScene(new Scene(root, 500, 800));
            stage.showAndWait();

            // Refresh cards after reservation
            loadRoomCards();
        } catch (IOException e) {
            e.printStackTrace();
            showStatus("Error opening reservation form: " + e.getMessage(), Color.RED);
        }
    }

    // -------------------- TABLE SETUP --------------------
    private void setupMyReservationsTable() {
        myResRoomColumn.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getRoomName()));

        // Show "YYYY-MM-DD HH:mm - HH:mm"
        myResDateColumn.setCellValueFactory(cd -> {
            Reservation r = cd.getValue();
            String date = r.getDate() == null ? "" : r.getDate();
            String st   = r.getStartTime() == null ? "" : r.getStartTime();
            String en   = r.getEndTime()   == null ? "" : r.getEndTime();
            String combined = date;
            if (!st.isEmpty() && !en.isEmpty()) combined += " " + st + " - " + en;
            return new javafx.beans.property.SimpleStringProperty(combined.trim());
        });

        // Show the *room's* current status: Available / Pending / Reserved
        myResStatusColumn.setCellValueFactory(cd -> {
            String raw = cd.getValue().getStatus();
            String nice = (raw == null || raw.isBlank()) ? "Pending"
                    : raw.substring(0,1).toUpperCase() + raw.substring(1).toLowerCase();
            return new javafx.beans.property.SimpleStringProperty(nice);
        });
    }


    // -------------------- USER SETTER --------------------
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + user.getUsername() + "!");
        }
        statusLabel.setText("Hello " + user.getUsername() + ", double-click a room card to reserve!");
    }

    // -------------------- ACTION EVENT: CANCEL RESERVATION --------------------
    @FXML
    private void cancelReservation() {
        Reservation selected = myReservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Please select a reservation to cancel!", Color.RED);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Cancel Reservation");
        confirmAlert.setHeaderText("Cancel reservation for: " + selected.getRoomName());
        confirmAlert.setContentText("Are you sure you want to cancel this reservation?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            DataStore.deleteReservation(selected);

            // Update room status to Available
            Room room = DataStore.getRoomByName(selected.getRoomName());
            if (room != null) {
                room.setStatus("Available");
                DataStore.updateRoom(room);
            }

            showStatus("Reservation Cancelled!", Color.ORANGE);
            loadRoomCards();
            showReservations();
        }
    }

    // -------------------- NAVIGATION ACTIONS --------------------
    @FXML
    private void showHome() {
        statusLabel.setText("Home - View all available rooms");
        loadRoomCards();
        hideMyReservations();
        searchField.clear();
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
                javafx.collections.FXCollections.observableArrayList(
                        DataStore.getReservationsByUser(currentUser.getUsername())
                );

        myReservationsTable.setItems(userReservations);

        // Show the reservations section
        if (myReservationsSection != null) {
            myReservationsSection.setVisible(true);
            myReservationsSection.setManaged(true);
        }
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
        loadRoomCards();
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
            stopAutoRefresh();
            DataStore.closeConnection();
            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.close();
        }
    }

    // -------------------- TIMER EVENT: AUTO-REFRESH --------------------
    private void startAutoRefresh() {
        refreshTask = new TimerTask() {
            public void run() {
                Platform.runLater(() -> {
                    loadRoomCards();
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

    // -------------------- UTILITY METHODS --------------------
    private void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setTextFill(color);
    }

    public void refreshCards() {
        loadRoomCards();
    }
}