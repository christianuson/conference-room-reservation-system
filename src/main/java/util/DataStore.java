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

/**
 * MySQL Database Manager for Conference Room System
 * Migrated from SQLite to MySQL for network database support
 */
public class DataStore {
    // MySQL Connection Configuration
    private static final String DB_HOST = System.getenv("DB_HOST") != null ?
            System.getenv("DB_HOST") : "localhost";
    private static final String DB_PORT = System.getenv("DB_PORT") != null ?
            System.getenv("DB_PORT") : "3306";
    private static final String DB_NAME = System.getenv("DB_NAME") != null ?
            System.getenv("DB_NAME") : "conference_room_db";
    private static final String DB_USER = System.getenv("DB_USER") != null ?
            System.getenv("DB_USER") : "root";
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD") != null ?
            System.getenv("DB_PASSWORD") : "";

    private static final String DB_URL = String.format(
            "jdbc:mysql://%s:%s/%s?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
            DB_HOST, DB_PORT, DB_NAME
    );

    private static Connection connection;

    private static final ObservableList<User> userList = FXCollections.observableArrayList();
    private static final ObservableList<Room> rooms = FXCollections.observableArrayList();
    private static final ObservableList<Reservation> reservations = FXCollections.observableArrayList();

    // -------------------- INITIALIZATION --------------------
    public static void initialize() {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish connection
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("[DATABASE] Connected to MySQL database");
            System.out.println("[DATABASE] Host: " + DB_HOST + ":" + DB_PORT);
            System.out.println("[DATABASE] Database: " + DB_NAME);

            createTables();
            initializeSampleData();
        } catch (ClassNotFoundException e) {
            System.err.println("[DATABASE ERROR] MySQL JDBC driver not found!");
            System.err.println("[DATABASE ERROR] Add MySQL Connector/J to your classpath");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Connection failed!");
            System.err.println("[DATABASE ERROR] Check your MySQL server is running and credentials are correct");
            System.err.println("[DATABASE ERROR] Connection URL: " + DB_URL);
            e.printStackTrace();
        }
    }

    private static void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        // Create Users table
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(100) NOT NULL,
                email VARCHAR(255) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                role VARCHAR(50) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_email (email),
                INDEX idx_role (role)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        stmt.execute(createUsersTable);

        // Create Rooms table
        String createRoomsTable = """
            CREATE TABLE IF NOT EXISTS rooms (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,
                status VARCHAR(50) NOT NULL,
                imagePath TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_name (name),
                INDEX idx_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        stmt.execute(createRoomsTable);

        // Create Reservations table
        String createReservationsTable = """
            CREATE TABLE IF NOT EXISTS reservations (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(100) NOT NULL,
                room_name VARCHAR(255) NOT NULL,
                date DATE NOT NULL,
                startTime TIME NOT NULL,
                endTime TIME NOT NULL,
                status VARCHAR(50) NOT NULL DEFAULT 'pending',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_username (username),
                INDEX idx_room_name (room_name),
                INDEX idx_date (date),
                INDEX idx_status (status),
                INDEX idx_room_date (room_name, date),
                FOREIGN KEY (room_name) REFERENCES rooms(name) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        stmt.execute(createReservationsTable);

        stmt.close();
        System.out.println("[DATABASE] Tables created successfully with indexes");
    }

    private static void initializeSampleData() throws SQLException {
        if (countRooms() == 0) {
            addRoom(new Room("Conference Room A", "Available", null));
            addRoom(new Room("Conference Room B", "Available", null));
            addRoom(new Room("Conference Room C", "Available", null));
            addRoom(new Room("Meeting Room 1", "Available", null));
            addRoom(new Room("Meeting Room 2", "Available", null));
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
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static int countRooms() throws SQLException {
        String sql = "SELECT COUNT(*) FROM rooms";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // -------------------- USER METHODS --------------------
    public static void loadUsers(String filePath) {
        if (connection == null) initialize();
        syncUsersFromDB();
    }

    private static void syncUsersFromDB() {
        userList.clear();
        String sql = "SELECT * FROM users ORDER BY id";
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
            e.printStackTrace();
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

            // Send welcome email for new users
            if ("user".equalsIgnoreCase(role)) {
                User newUser = getUserByEmail(email);
                if (newUser != null) {
                    EmailService.getInstance().sendWelcomeEmail(newUser);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to add user: " + e.getMessage());
            e.printStackTrace();
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
        System.out.println("[DATABASE] Users already persisted to MySQL database");
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

    public static void deleteUser(User user) {
        if (connection == null) initialize();
        String sql = "DELETE FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                userList.remove(user); // keep local list in sync
                System.out.println("[DATABASE] User deleted from MySQL: " + user.getEmail());
            } else {
                System.out.println("[DATABASE] No user found to delete: " + user.getEmail());
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to delete user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void removeUser(User user) {
        deleteUser(user);
        syncUsersFromDB();
    }

    // -------------------- ROOM METHODS --------------------
    public static void loadRooms() {
        if (connection == null) initialize();
        syncRoomsFromDB();
    }

    private static void syncRoomsFromDB() {
        rooms.clear();
        String sql = "SELECT * FROM rooms ORDER BY id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rooms.add(new Room(
                        rs.getString("name"),
                        rs.getString("status"),
                        rs.getString("imagePath")
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
                DateTimeFormatter tf = DateTimeFormatter.ofPattern("H:mm[:ss]")
                        .withResolverStyle(java.time.format.ResolverStyle.LENIENT);

                while (rs.next()) {
                    Date dateObj = rs.getDate("date");
                    String stStr = rs.getString("startTime");
                    String enStr = rs.getString("endTime");
                    String status = rs.getString("status");

                    if (dateObj == null || stStr == null || enStr == null) continue;
                    LocalDate resDate = dateObj.toLocalDate();
                    if (!today.equals(resDate)) continue;

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
        String sql = "INSERT INTO rooms (name, status, imagePath) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, room.getName());
            pstmt.setString(2, room.getStatus());
            pstmt.setString(3, room.getImagePath());
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
            String sql = "UPDATE rooms SET status = ?, imagePath = ? WHERE name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, room.getStatus());
                pstmt.setString(2, room.getImagePath());
                pstmt.setString(3, room.getName());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[DATABASE ERROR] Failed to save room: " + e.getMessage());
            }
        }
        System.out.println("[DATABASE] Rooms saved to MySQL database");
    }

    public static Room getRoomByName(String name) {
        String sql = "SELECT * FROM rooms WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Room(
                        rs.getString("name"),
                        rs.getString("status"),
                        rs.getString("imagePath")
                );
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to get room: " + e.getMessage());
        }
        return null;
    }

    public static void updateRoom(Room room) {
        String sql = "UPDATE rooms SET status = ?, imagePath = ? WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, room.getStatus());
            pstmt.setString(2, room.getImagePath());
            pstmt.setString(3, room.getName());
            pstmt.executeUpdate();
            syncRoomsFromDB();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to update room: " + e.getMessage());
        }
    }

    // -------------------- RESERVATION METHODS --------------------
    public static void loadReservations() {
        if (connection == null) initialize();
        syncReservationsFromDB();
    }

    private static void syncReservationsFromDB() {
        reservations.clear();
        String sql = "SELECT * FROM reservations ORDER BY date DESC, startTime DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reservations.add(new Reservation(
                        rs.getString("username"),
                        rs.getString("room_name"),
                        rs.getDate("date").toString(),
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

    public static void addReservation(String username, String roomName, String date) {
        addReservation(username, roomName, date, "00:00", "23:59", "pending");
    }

    public static void addReservation(String username, String roomName, String date,
                                      String startTime, String endTime, String status) {
        String sql = "INSERT INTO reservations (username, room_name, date, startTime, endTime, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, roomName);
            pstmt.setDate(3, Date.valueOf(date));
            pstmt.setTime(4, Time.valueOf(startTime + ":00"));
            pstmt.setTime(5, Time.valueOf(endTime + ":00"));
            pstmt.setString(6, status);
            pstmt.executeUpdate();
            syncReservationsFromDB();

            // Send confirmation email
            User user = getUserByUsername(username);
            if (user != null) {
                Reservation newRes = new Reservation(username, roomName, date, startTime, endTime, status);
                EmailService.getInstance().sendReservationConfirmation(user, newRes);
            }
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

    public static User getUserByUsername(String username) {
        if (connection == null) initialize();
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
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
            System.err.println("[DATABASE ERROR] Failed to get user by username: " + e.getMessage());
        }
        return null;
    }

    public static List<Reservation> getReservationsByUser(String username) {
        List<Reservation> userReservations = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE username = ? ORDER BY date DESC, startTime DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                userReservations.add(new Reservation(
                        rs.getString("username"),
                        rs.getString("room_name"),
                        rs.getDate("date").toString(),
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

    public static boolean hasConflict(String roomName, String date, String startTime, String endTime) {
        String sql = "SELECT * FROM reservations WHERE room_name = ? AND date = ? AND status = 'approved'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, roomName);
            pstmt.setDate(2, Date.valueOf(date));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Reservation existing = new Reservation(
                        rs.getString("username"),
                        rs.getString("room_name"),
                        rs.getDate("date").toString(),
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

    public static String getRoomStatusForTime(String roomName, String date, String startTime, String endTime) {
        String sql = "SELECT * FROM reservations WHERE room_name = ? AND date = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, roomName);
            pstmt.setDate(2, Date.valueOf(date));
            ResultSet rs = pstmt.executeQuery();

            boolean hasPending = false;
            boolean hasApproved = false;

            while (rs.next()) {
                Reservation existing = new Reservation(
                        rs.getString("username"),
                        rs.getString("room_name"),
                        rs.getDate("date").toString(),
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
            pstmt.setDate(3, Date.valueOf(reservation.getDate()));
            pstmt.setTime(4, Time.valueOf(reservation.getStartTime() + ":00"));
            pstmt.setTime(5, Time.valueOf(reservation.getEndTime() + ":00"));
            pstmt.executeUpdate();
            syncReservationsFromDB();

            // Send cancellation email
            User user = getUserByUsername(reservation.getUsername());
            if (user != null) {
                EmailService.getInstance().sendReservationCancellation(user, reservation);
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to delete reservation: " + e.getMessage());
        }
    }

    public static void updateReservationStatus(Reservation reservation, String newStatus) {
        String sql = "UPDATE reservations SET status = ? WHERE username = ? AND room_name = ? AND date = ? AND startTime = ? AND endTime = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, reservation.getUsername());
            pstmt.setString(3, reservation.getRoomName());
            pstmt.setDate(4, Date.valueOf(reservation.getDate()));
            pstmt.setTime(5, safeParseTime(reservation.getStartTime()));
            pstmt.setTime(6, safeParseTime(reservation.getEndTime()));
            pstmt.executeUpdate();
            syncReservationsFromDB();

            // Send appropriate email based on new status
            User user = getUserByUsername(reservation.getUsername());
            if (user != null) {
                reservation.setStatus(newStatus);
                if ("approved".equalsIgnoreCase(newStatus) || "reserved".equalsIgnoreCase(newStatus)) {
                    EmailService.getInstance().sendReservationApproval(user, reservation);
                } else if ("rejected".equalsIgnoreCase(newStatus)) {
                    EmailService.getInstance().sendReservationRejection(user, reservation);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to update reservation status: " + e.getMessage());
        }
    }

    private static Time safeParseTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return Time.valueOf("00:00:00");
        String t = timeStr.trim();
        if (t.length() == 5) t += ":00"; // e.g. "13:30" → "13:30:00"
        return Time.valueOf(t);
    }

    public static void saveReservations() {
        System.out.println("[DATABASE] Reservations already persisted to MySQL database");
    }

    // -------------------- UTILITY --------------------
    public static void saveAll() {
        saveUsers();
        saveRooms();
        saveReservations();
        System.out.println("[DATABASE] All data persisted to MySQL");
    }

    public static void reloadAll() {
        syncUsersFromDB();
        syncRoomsFromDB();
        syncReservationsFromDB();
        System.out.println("[DATABASE] All data reloaded from MySQL database");
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DATABASE] MySQL connection closed");
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to close connection: " + e.getMessage());
        }
    }

    public static Connection getConnection() {
        return connection;
    }
}