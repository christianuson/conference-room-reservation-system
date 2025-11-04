package util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Reservation;
import model.Room;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DataStore {
    private static final String DB_URL = "jdbc:sqlite:conference_room.db";
    private static Connection connection;

    private static final ObservableList<User> userList = FXCollections.observableArrayList();
    private static final ObservableList<Room> rooms = FXCollections.observableArrayList();
    private static final ObservableList<Reservation> reservations = FXCollections.observableArrayList();

    // -------------------- INITIALIZATION --------------------
    public static void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("[DATABASE] Connected to SQLite database");

            createTables();
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

        // Create Reservations table with time range and status
        String createReservationsTable = "CREATE TABLE IF NOT EXISTS reservations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL," +
                "room_name TEXT NOT NULL," +
                "date TEXT NOT NULL," +
                "startTime TEXT NOT NULL DEFAULT '00:00'," +
                "endTime TEXT NOT NULL DEFAULT '23:59'," +
                "status TEXT NOT NULL DEFAULT 'pending'" +
                ")";
        stmt.execute(createReservationsTable);

        stmt.close();
        System.out.println("[DATABASE] Tables created successfully");
    }

    private static void initializeSampleData() throws SQLException {
        if (countRooms() == 0) {
            addRoom(new Room("Conference Room A", "Available"));
            addRoom(new Room("Conference Room B", "Available"));
            addRoom(new Room("Conference Room C", "Available"));
            addRoom(new Room("Meeting Room 1", "Available"));
            addRoom(new Room("Meeting Room 2", "Available"));
            System.out.println("[DATABASE] Sample rooms initialized");
        }

        if (countUsers() == 0) {
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

    // -------------------- USER METHODS (unchanged) --------------------
    public static void loadUsers(String filePath) {
        if (connection == null) initialize();
        syncUsersFromDB();
    }

    private static void syncUsersFromDB() {
        userList.clear();
        String sql = "SELECT * FROM users";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                userList.add(new User(
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to sync users: " + e.getMessage());
        }
    }

    public static ObservableList<User> getUsers() {
        if (connection == null) initialize();
        syncUsersFromDB();
        return userList;
    }

    public static boolean validateUser(String email, String password) {
        if (connection == null) initialize();
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to validate user: " + e.getMessage());
            return false;
        }
    }

    public static boolean userExists(String email) {
        if (connection == null) initialize();
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
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
            syncUsersFromDB();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to add user: " + e.getMessage());
        }
    }

    public static boolean isAdmin(String email) {
        if (connection == null) initialize();
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
        if (connection == null) initialize();
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

    // -------------------- ROOM METHODS (unchanged) --------------------
    public static void loadRooms() {
        if (connection == null) initialize();
        syncRoomsFromDB();
    }

    private static void syncRoomsFromDB() {
        rooms.clear();
        String sql = "SELECT * FROM rooms";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rooms.add(new Room(
                        rs.getString("name"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to sync rooms: " + e.getMessage());
        }
    }

    public static ObservableList<Room> getRooms() {
        if (connection == null) initialize();
        syncRoomsFromDB();
        return rooms;
    }

    public static String computeRoomStatusNow(String roomName) {
        String sql = "SELECT status, date, startTime, endTime FROM reservations WHERE room_name = ?";
        boolean hasPending = false;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, roomName);
            try (ResultSet rs = ps.executeQuery()) {
                LocalDate today = LocalDate.now();
                LocalTime now = LocalTime.now();
                DateTimeFormatter tf = DateTimeFormatter.ofPattern("H:mm").withResolverStyle(java.time.format.ResolverStyle.LENIENT);

                while (rs.next()) {
                    String dateStr = rs.getString("date");         // yyyy-MM-dd
                    String stStr   = rs.getString("startTime");    // HH:mm
                    String enStr   = rs.getString("endTime");      // HH:mm
                    String status  = rs.getString("status");       // pending / approved / rejected

                    if (dateStr == null || stStr == null || enStr == null) continue;
                    if (!today.toString().equals(dateStr)) continue;

                    LocalTime st = LocalTime.parse(stStr, tf);
                    LocalTime en = LocalTime.parse(enStr, tf);
                    boolean overlapsNow = !now.isBefore(st) && now.isBefore(en);

                    if (overlapsNow) {
                        if ("approved".equalsIgnoreCase(status)) return "Occupied";
                        if ("pending".equalsIgnoreCase(status)) hasPending = true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] computeRoomStatusNow: " + e.getMessage());
        }
        return hasPending ? "Pending" : "Available";
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

    // -------------------- RESERVATION METHODS (WITH TIME RANGE) --------------------
    public static void loadReservations() {
        if (connection == null) initialize();
        syncReservationsFromDB();
    }

    private static void syncReservationsFromDB() {
        reservations.clear();
        String sql = "SELECT * FROM reservations";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reservations.add(new Reservation(
                        rs.getString("username"),
                        rs.getString("room_name"),
                        rs.getString("date"),
                        rs.getString("startTime"),
                        rs.getString("endTime"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to sync reservations: " + e.getMessage());
        }
    }

    public static ObservableList<Reservation> getReservations() {
        if (connection == null) initialize();
        syncReservationsFromDB();
        return reservations;
    }

    // Legacy method (backwards compatibility)
    public static void addReservation(String username, String roomName, String date) {
        addReservation(username, roomName, date, "00:00", "23:59", "pending");
    }

    // NEW: Add reservation with time range and status
    public static void addReservation(String username, String roomName, String date,
                                      String startTime, String endTime, String status) {
        String sql = "INSERT INTO reservations (username, room_name, date, startTime, endTime, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, roomName);
            pstmt.setString(3, date);
            pstmt.setString(4, startTime);
            pstmt.setString(5, endTime);
            pstmt.setString(6, status);
            pstmt.executeUpdate();
            syncReservationsFromDB();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to add reservation: " + e.getMessage());
        }
    }

    public static void addReservation(Reservation reservation) {
        addReservation(
                reservation.getUsername(),
                reservation.getRoomName(),
                reservation.getDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus()
        );
    }

    public static List<Reservation> getReservationsByUser(String username) {
        List<Reservation> userReservations = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                userReservations.add(new Reservation(
                        rs.getString("username"),
                        rs.getString("room_name"),
                        rs.getString("date"),
                        rs.getString("startTime"),
                        rs.getString("endTime"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to get user reservations: " + e.getMessage());
        }
        return userReservations;
    }

    // NEW: Check if a time slot conflicts with approved reservations
    public static boolean hasConflict(String roomName, String date, String startTime, String endTime) {
        String sql = "SELECT * FROM reservations WHERE room_name = ? AND date = ? AND status = 'approved'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, roomName);
            pstmt.setString(2, date);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Reservation existing = new Reservation(
                        rs.getString("username"),
                        rs.getString("room_name"),
                        rs.getString("date"),
                        rs.getString("startTime"),
                        rs.getString("endTime"),
                        rs.getString("status")
                );

                if (existing.conflictsWith(date, startTime, endTime)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to check conflicts: " + e.getMessage());
        }
        return false;
    }

    // NEW: Get room status for specific date/time (for dynamic card display)
    public static String getRoomStatusForTime(String roomName, String date, String startTime, String endTime) {
        String sql = "SELECT * FROM reservations WHERE room_name = ? AND date = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, roomName);
            pstmt.setString(2, date);
            ResultSet rs = pstmt.executeQuery();

            boolean hasPending = false;
            boolean hasApproved = false;

            while (rs.next()) {
                Reservation existing = new Reservation(
                        rs.getString("username"),
                        rs.getString("room_name"),
                        rs.getString("date"),
                        rs.getString("startTime"),
                        rs.getString("endTime"),
                        rs.getString("status")
                );

                if (existing.conflictsWith(date, startTime, endTime)) {
                    if ("approved".equalsIgnoreCase(existing.getStatus())) {
                        hasApproved = true;
                    } else if ("pending".equalsIgnoreCase(existing.getStatus())) {
                        hasPending = true;
                    }
                }
            }

            if (hasApproved) return "Approved";
            if (hasPending) return "Pending";
            return "Available";

        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to get room status: " + e.getMessage());
        }
        return "Available";
    }

    public static void deleteReservation(Reservation reservation) {
        String sql = "DELETE FROM reservations WHERE username = ? AND room_name = ? AND date = ? AND startTime = ? AND endTime = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, reservation.getUsername());
            pstmt.setString(2, reservation.getRoomName());
            pstmt.setString(3, reservation.getDate());
            pstmt.setString(4, reservation.getStartTime());
            pstmt.setString(5, reservation.getEndTime());
            pstmt.executeUpdate();
            syncReservationsFromDB();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to delete reservation: " + e.getMessage());
        }
    }

    // NEW: Update reservation status (pending -> approved)
    public static void updateReservationStatus(Reservation reservation, String newStatus) {
        String sql = "UPDATE reservations SET status = ? WHERE username = ? AND room_name = ? AND date = ? AND startTime = ? AND endTime = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, reservation.getUsername());
            pstmt.setString(3, reservation.getRoomName());
            pstmt.setString(4, reservation.getDate());
            pstmt.setString(5, reservation.getStartTime());
            pstmt.setString(6, reservation.getEndTime());
            pstmt.executeUpdate();
            syncReservationsFromDB();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to update reservation status: " + e.getMessage());
        }
    }

    public static void saveReservations() {
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