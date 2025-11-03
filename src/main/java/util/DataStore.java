package util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Reservation;
import model.Room;
import model.User;

import java.sql.*;

public class DataStore {
    private static final String DB_URL = "jdbc:sqlite:conference_room.db";
    private static Connection connection;

    private static final ObservableList<User> userList = FXCollections.observableArrayList();
    private static final ObservableList<Room> rooms = FXCollections.observableArrayList();
    private static final ObservableList<Reservation> reservations = FXCollections.observableArrayList();

    // -------------------- INITIALIZATION --------------------
    public static void initialize() {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Establish connection
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("[DATABASE] Connected to SQLite database");

            // Create tables if they don't exist
            createTables();

            // Initialize with sample data if empty
            initializeSampleData();
        } catch (ClassNotFoundException e) {
            System.err.println("[DATABASE ERROR] SQLite JDBC driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Connection failed!");
            e.printStackTrace();
        }
    }

    private static void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        // Create Users table
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL," +
                "email TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL," +
                "role TEXT NOT NULL" +
                ")";
        stmt.execute(createUsersTable);

        // Create Rooms table
        String createRoomsTable = "CREATE TABLE IF NOT EXISTS rooms (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE," +
                "status TEXT NOT NULL" +
                ")";
        stmt.execute(createRoomsTable);

        // Create Reservations table
        String createReservationsTable = "CREATE TABLE IF NOT EXISTS reservations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL," +
                "room_name TEXT NOT NULL," +
                "date TEXT NOT NULL" +
                ")";
        stmt.execute(createReservationsTable);

        stmt.close();
        System.out.println("[DATABASE] Tables created successfully");
    }

    private static void initializeSampleData() throws SQLException {
        // Check if data already exists
        if (countRooms() == 0) {
            // Add sample rooms
            addRoom(new Room("Conference Room A", "Available"));
            addRoom(new Room("Conference Room B", "Available"));
            addRoom(new Room("Conference Room C", "Reserved"));
            addRoom(new Room("Meeting Room 1", "Available"));
            addRoom(new Room("Meeting Room 2", "Pending"));
            System.out.println("[DATABASE] Sample rooms initialized");
        }

        if (countUsers() == 0) {
            // Add sample users
            addUser("admin", "admin@example.com", "admin123", "admin");
            addUser("john_doe", "john@example.com", "password123", "user");
            addUser("jane_smith", "jane@example.com", "password123", "user");
            System.out.println("[DATABASE] Sample users initialized");
        }
    }

    private static int countUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        int count = rs.getInt(1);
        stmt.close();
        return count;
    }

    private static int countRooms() throws SQLException {
        String sql = "SELECT COUNT(*) FROM rooms";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        int count = rs.getInt(1);
        stmt.close();
        return count;
    }

    // -------------------- USER METHODS --------------------
    public static void loadUsers(String filePath) {
        // Initialize database instead of loading from file
        if (connection == null) {
            initialize();
        }
        syncUsersFromDB();
    }

    private static void syncUsersFromDB() {
        userList.clear();
        String sql = "SELECT * FROM users";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User(
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role")
                );
                userList.add(user);
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to sync users: " + e.getMessage());
        }
    }

    public static ObservableList<User> getUsers() {
        if (connection == null) {
            initialize();
        }
        syncUsersFromDB();
        return userList;
    }

    public static boolean validateUser(String email, String password) {
        if (connection == null) {
            initialize();
        }

        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to validate user: " + e.getMessage());
            return false;
        }
    }

    public static boolean userExists(String email) {
        if (connection == null) {
            initialize();
        }

        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to check user existence: " + e.getMessage());
            return false;
        }
    }

    public static void addUser(String username, String email, String password, String role) {
        if (userExists(email)) return;

        String sql = "INSERT INTO users (username, email, password, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            pstmt.executeUpdate();
            syncUsersFromDB(); // Refresh cache
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to add user: " + e.getMessage());
        }
    }

    public static boolean isAdmin(String email) {
        if (connection == null) {
            initialize();
        }

        String sql = "SELECT role FROM users WHERE email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return "admin".equalsIgnoreCase(rs.getString("role"));
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to check admin status: " + e.getMessage());
        }
        return false;
    }

    public static User getUserByEmail(String email) {
        if (connection == null) {
            initialize();
        }

        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role")
                );
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to get user: " + e.getMessage());
        }
        return null;
    }

    public static void saveUsers() {
        // No-op: Auto-saved to database in real-time
        System.out.println("[DATABASE] Users already persisted to database");
    }

    public static void updateUser(User user) {
        String sql = "UPDATE users SET username = ?, password = ?, role = ? WHERE email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());
            pstmt.setString(4, user.getEmail());
            pstmt.executeUpdate();
            syncUsersFromDB();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to update user: " + e.getMessage());
        }
    }

    public static void deleteUser(String email) {
        String sql = "DELETE FROM users WHERE email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.executeUpdate();
            syncUsersFromDB();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to delete user: " + e.getMessage());
        }
    }

    public static void removeUser(User user) {
        deleteUser(user.getEmail());
    }

    // -------------------- ROOM METHODS --------------------
    public static void loadRooms() {
        if (connection == null) {
            initialize();
        }
        syncRoomsFromDB();
    }

    private static void syncRoomsFromDB() {
        rooms.clear();
        String sql = "SELECT * FROM rooms";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Room room = new Room(
                        rs.getString("name"),
                        rs.getString("status")
                );
                rooms.add(room);
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to sync rooms: " + e.getMessage());
        }
    }

    public static ObservableList<Room> getRooms() {
        if (connection == null) {
            initialize();
        }
        syncRoomsFromDB();
        return rooms;
    }

    public static void addRoom(Room room) {
        String sql = "INSERT INTO rooms (name, status) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, room.getName());
            pstmt.setString(2, room.getStatus());
            pstmt.executeUpdate();
            syncRoomsFromDB();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to add room: " + e.getMessage());
        }
    }

    public static void removeRoom(Room room) {
        String sql = "DELETE FROM rooms WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, room.getName());
            pstmt.executeUpdate();
            syncRoomsFromDB();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to remove room: " + e.getMessage());
        }
    }

    public static void saveRooms() {
        // Update all rooms in database
        for (Room room : rooms) {
            String sql = "UPDATE rooms SET status = ? WHERE name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, room.getStatus());
                pstmt.setString(2, room.getName());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[DATABASE ERROR] Failed to save room: " + e.getMessage());
            }
        }
        System.out.println("[DATABASE] Rooms saved to database");
    }

    public static Room getRoomByName(String name) {
        String sql = "SELECT * FROM rooms WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Room(
                        rs.getString("name"),
                        rs.getString("status")
                );
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to get room: " + e.getMessage());
        }
        return null;
    }

    public static void updateRoom(Room room) {
        String sql = "UPDATE rooms SET status = ? WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, room.getStatus());
            pstmt.setString(2, room.getName());
            pstmt.executeUpdate();
            syncRoomsFromDB();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to update room: " + e.getMessage());
        }
    }

    // -------------------- RESERVATION METHODS --------------------
    public static void loadReservations() {
        if (connection == null) {
            initialize();
        }
        syncReservationsFromDB();
    }

    private static void syncReservationsFromDB() {
        reservations.clear();
        String sql = "SELECT * FROM reservations";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Reservation reservation = new Reservation(
                        rs.getString("username"),
                        rs.getString("room_name"),
                        rs.getString("date")
                );
                reservations.add(reservation);
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to sync reservations: " + e.getMessage());
        }
    }

    public static ObservableList<Reservation> getReservations() {
        if (connection == null) {
            initialize();
        }
        syncReservationsFromDB();
        return reservations;
    }

    public static void addReservation(String username, String roomName, String date) {
        String sql = "INSERT INTO reservations (username, room_name, date) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, roomName);
            pstmt.setString(3, date);
            pstmt.executeUpdate();
            syncReservationsFromDB();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to add reservation: " + e.getMessage());
        }
    }

    public static void addReservation(Reservation reservation) {
        addReservation(reservation.getUsername(), reservation.getRoomName(), reservation.getDate());
    }

    public static java.util.List<Reservation> getReservationsByUser(String username) {
        java.util.List<Reservation> userReservations = new java.util.ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Reservation reservation = new Reservation(
                        rs.getString("username"),
                        rs.getString("room_name"),
                        rs.getString("date")
                );
                userReservations.add(reservation);
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to get user reservations: " + e.getMessage());
        }

        return userReservations;
    }

    public static void deleteReservation(Reservation reservation) {
        String sql = "DELETE FROM reservations WHERE username = ? AND room_name = ? AND date = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, reservation.getUsername());
            pstmt.setString(2, reservation.getRoomName());
            pstmt.setString(3, reservation.getDate());
            pstmt.executeUpdate();
            syncReservationsFromDB();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to delete reservation: " + e.getMessage());
        }
    }

    public static void saveReservations() {
        // No-op: Auto-saved to database in real-time
        System.out.println("[DATABASE] Reservations already persisted to database");
    }

    // -------------------- UTILITY --------------------
    public static void saveAll() {
        saveUsers();
        saveRooms();
        saveReservations();
        System.out.println("[DATABASE] All data persisted");
    }

    public static void reloadAll() {
        syncUsersFromDB();
        syncRoomsFromDB();
        syncReservationsFromDB();
        System.out.println("[DATABASE] All data reloaded from database");
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DATABASE] Connection closed");
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to close connection: " + e.getMessage());
        }
    }

    public static Connection getConnection() {
        return connection;
    }
}