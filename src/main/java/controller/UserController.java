package controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class UserController {

    @FXML private BorderPane mainPane;
    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;
    @FXML private FlowPane roomCardsContainer;
    @FXML private TextField searchField;
    @FXML private VBox myReservationsSection;
    @FXML private TableView<Reservation> myReservationsTable;
    @FXML private TableColumn<Reservation, String> myResRoomColumn;
    @FXML private TableColumn<Reservation, String> myResDateColumn;
    @FXML private TableColumn<Reservation, String> myResStatusColumn;
    @FXML private Button cancelButton;

    private User currentUser;
    private final Timer refreshTimer = new Timer(true);
    private TimerTask refreshTask;

    @FXML
    public void initialize() {
        DataStore.initialize();

        setupMyReservationsTable();
        setupSearchFilter();
        loadRoomCards();
        startAutoRefresh();

        statusLabel.setText("Ready to make a reservation");
        System.out.println("[USER DASHBOARD] Initialized successfully");
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + user.getUsername() + "!");
        }
        System.out.println("[USER DASHBOARD] Current user set: " + user.getUsername());
    }

    private void setupMyReservationsTable() {
        myResRoomColumn.setCellValueFactory(new PropertyValueFactory<>("roomName"));

        myResDateColumn.setCellValueFactory(cd -> {
            Reservation r = cd.getValue();
            String dateTime = r.getDate() + " " + r.getStartTime() + " - " + r.getEndTime();
            return new javafx.beans.property.SimpleStringProperty(dateTime);
        });

        myResStatusColumn.setCellValueFactory(cd -> {
            String raw = cd.getValue().getStatus();
            String nice = (raw == null || raw.isBlank()) ? "Pending"
                    : raw.substring(0,1).toUpperCase() + raw.substring(1).toLowerCase();
            return new javafx.beans.property.SimpleStringProperty(nice);
        });

        myReservationsTable.setRowFactory(tv -> new TableRow<Reservation>() {
            @Override
            protected void updateItem(Reservation item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    String status = item.getStatus();
                    if ("approved".equalsIgnoreCase(status) || "reserved".equalsIgnoreCase(status)) {
                        setStyle("-fx-background-color: #e8f5e9;");
                    } else if ("pending".equalsIgnoreCase(status)) {
                        setStyle("-fx-background-color: #fff3cd;");
                    } else if ("rejected".equalsIgnoreCase(status)) {
                        setStyle("-fx-background-color: #ffebee;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void setupSearchFilter() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> {
                filterRoomCards(newV);
            });
        }
    }

    private void filterRoomCards(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadRoomCards();
            return;
        }

        String lowerQuery = query.toLowerCase().trim();
        List<Room> filteredRooms = DataStore.getRooms().stream()
                .filter(room -> room.getName().toLowerCase().contains(lowerQuery) ||
                        room.getStatus().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());

        displayRoomCards(filteredRooms);
        statusLabel.setText("Found " + filteredRooms.size() + " room(s) matching '" + query + "'");
    }

    private void loadRoomCards() {
        displayRoomCards(DataStore.getRooms());
    }

    private void displayRoomCards(List<Room> rooms) {
        roomCardsContainer.getChildren().clear();

        for (Room room : rooms) {
            VBox card = createRoomCard(room);
            roomCardsContainer.getChildren().add(card);
        }
    }

    private VBox createRoomCard(Room room) {
        VBox card = new VBox(0);
        card.setPrefSize(320, 200);
        card.setMaxSize(320, 200);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #ddd; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");

        // Create main horizontal layout
        HBox mainContent = new HBox(0);
        mainContent.setPrefSize(316, 196);

        // Left side - Image (landscape)
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(180, 196);
        imageContainer.setMaxSize(180, 196);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(196);
        imageView.setPreserveRatio(false);

        if (room.getImagePath() != null && !room.getImagePath().isEmpty()) {
            try {
                File imageFile = new File(room.getImagePath());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    imageView.setImage(image);
                } else {
                    imageView.setStyle("-fx-background-color: #f5f5f5;");
                }
            } catch (Exception e) {
                System.err.println("[USER DASHBOARD] Error loading image: " + e.getMessage());
                imageView.setStyle("-fx-background-color: #f5f5f5;");
            }
        } else {
            imageView.setStyle("-fx-background-color: #f5f5f5;");
        }

        imageContainer.getChildren().add(imageView);

        // Right side - Content
        VBox contentBox = new VBox(8);
        contentBox.setPrefSize(136, 196);
        contentBox.setPadding(new Insets(15, 12, 15, 12));
        contentBox.setAlignment(Pos.TOP_LEFT);

        // Room name
        Label nameLabel = new Label(room.getName());
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #333;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(112);

        // Status with current computation
        String currentStatus = DataStore.computeRoomStatusNow(room.getName());
        Label statusLabel = new Label(currentStatus);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-padding: 4 8; -fx-background-radius: 3;");
        statusLabel.setMaxWidth(112);

        String borderColor = "#ddd";
        if ("Available".equalsIgnoreCase(currentStatus)) {
            statusLabel.setStyle(statusLabel.getStyle() +
                    "-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");
            borderColor = "#4CAF50";
        } else if ("Occupied".equalsIgnoreCase(currentStatus)) {
            statusLabel.setStyle(statusLabel.getStyle() +
                    "-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-weight: bold;");
            borderColor = "#f44336";
        } else if ("Pending".equalsIgnoreCase(currentStatus)) {
            statusLabel.setStyle(statusLabel.getStyle() +
                    "-fx-background-color: #fff3cd; -fx-text-fill: #f57c00; -fx-font-weight: bold;");
            borderColor = "#FFC107";
        }

        // Update card border based on status
        String finalBorderColor = borderColor;
        card.setStyle(card.getStyle() + "-fx-border-color: " + finalBorderColor + ";");

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Hint at bottom
        Label hintLabel = new Label("Double-click to reserve");
        hintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999; -fx-font-style: italic;");
        hintLabel.setWrapText(true);
        hintLabel.setMaxWidth(112);

        contentBox.getChildren().addAll(nameLabel, statusLabel, spacer, hintLabel);

        // Status icon at bottom right
        StackPane statusIconPane = new StackPane();
        statusIconPane.setPrefSize(30, 30);
        statusIconPane.setMaxSize(30, 30);
        statusIconPane.setAlignment(Pos.BOTTOM_RIGHT);
        statusIconPane.setStyle("-fx-background-radius: 15; -fx-padding: 0 8 8 0;");

        Label iconLabel = new Label();
        iconLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        if ("Available".equalsIgnoreCase(currentStatus)) {
            iconLabel.setText("✓");
            iconLabel.setStyle(iconLabel.getStyle() + "-fx-text-fill: #4CAF50;");
        } else if ("Occupied".equalsIgnoreCase(currentStatus)) {
            iconLabel.setText("✗");
            iconLabel.setStyle(iconLabel.getStyle() + "-fx-text-fill: #f44336;");
        } else if ("Pending".equalsIgnoreCase(currentStatus)) {
            iconLabel.setText("⏱");
            iconLabel.setStyle(iconLabel.getStyle() + "-fx-text-fill: #FFC107;");
        }

        statusIconPane.getChildren().add(iconLabel);
        StackPane.setAlignment(iconLabel, Pos.BOTTOM_RIGHT);

        // Combine everything
        mainContent.getChildren().addAll(imageContainer, contentBox);

        // Use StackPane to overlay icon
        StackPane cardStack = new StackPane();
        cardStack.getChildren().addAll(mainContent, statusIconPane);
        StackPane.setAlignment(statusIconPane, Pos.BOTTOM_RIGHT);

        card.getChildren().add(cardStack);

        // Mouse events
        card.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                openReservationForm(room);
            }
        });

        String originalStyle = card.getStyle();
        card.setOnMouseEntered(e -> {
            String glowColor;
            if ("Available".equalsIgnoreCase(currentStatus)) {
                glowColor = "rgba(76, 175, 80, 0.4)";
            } else if ("Occupied".equalsIgnoreCase(currentStatus)) {
                glowColor = "rgba(244, 67, 54, 0.4)";
            } else {
                glowColor = "rgba(255, 193, 7, 0.4)";
            }
            card.setStyle(originalStyle +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, " + glowColor + ", 15, 0.5, 0, 0); " +
                    "-fx-scale-x: 1.02; -fx-scale-y: 1.02;");
        });

        card.setOnMouseExited(e -> {
            card.setStyle(originalStyle);
        });

        return card;
    }

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
            stage.setTitle("Reserve " + room.getName());
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error opening reservation form!");
            statusLabel.setTextFill(Color.RED);
        }
    }

    @FXML
    private void showHome() {
        myReservationsSection.setVisible(false);
        myReservationsSection.setManaged(false);
        statusLabel.setText("Viewing available rooms");
    }

    @FXML
    private void showReservations() {
        if (currentUser == null) {
            statusLabel.setText("Error: No user logged in");
            statusLabel.setTextFill(Color.RED);
            return;
        }

        List<Reservation> userReservations = DataStore.getReservationsByUser(currentUser.getUsername());
        myReservationsTable.setItems(FXCollections.observableArrayList(userReservations));

        myReservationsSection.setVisible(true);
        myReservationsSection.setManaged(true);

        statusLabel.setText("Showing " + userReservations.size() + " reservation(s)");
        statusLabel.setTextFill(Color.BLACK);
    }

    @FXML
    private void closeMyReservations() {
        myReservationsSection.setVisible(false);
        myReservationsSection.setManaged(false);
        statusLabel.setText("Ready to make a reservation");
    }

    @FXML
    private void cancelReservation() {
        Reservation selected = myReservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Please select a reservation to cancel");
            statusLabel.setTextFill(Color.RED);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Reservation");
        alert.setHeaderText("Cancel reservation for " + selected.getRoomName() + "?");
        alert.setContentText("Date: " + selected.getDate() + "\nTime: " + selected.getStartTime() + " - " + selected.getEndTime());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            DataStore.deleteReservation(selected);
            showReservations();
            refreshCards();
            statusLabel.setText("Reservation cancelled successfully");
            statusLabel.setTextFill(Color.GREEN);
        }
    }

    @FXML
    private void refreshRooms() {
        DataStore.reloadAll();
        loadRoomCards();
        statusLabel.setText("Room list refreshed at " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        statusLabel.setTextFill(Color.GREEN);
    }

    public void refreshCards() {
        DataStore.reloadAll();
        loadRoomCards();
    }

    private void startAutoRefresh() {
        refreshTask = new TimerTask() {
            public void run() {
                Platform.runLater(() -> {
                    DataStore.reloadAll();
                    loadRoomCards();
                    System.out.println("[AUTO-REFRESH] Room cards refreshed at " +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                });
            }
        };
        refreshTimer.schedule(refreshTask, 30000, 30000);
    }

    @FXML
    private void exitApp() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Application");
        alert.setHeaderText("Are you sure you want to exit?");
        alert.setContentText("You will be logged out.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (refreshTask != null) refreshTask.cancel();
            refreshTimer.cancel();

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
                Parent loginRoot = loader.load();

                Stage stage = (Stage) mainPane.getScene().getWindow();
                stage.setTitle("Login");
                stage.setWidth(540);
                stage.setHeight(570);
                stage.setScene(new Scene(loginRoot));
                stage.centerOnScreen();
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
                statusLabel.setText("Error returning to login!");
                statusLabel.setTextFill(Color.RED);
            }
        }
    }
}