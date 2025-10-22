package util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Room;
import model.User;
import model.Reservation;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class DataStore {
    private static final String USERS_FILE = "src/main/resources/data/users.json";
    private static final String ROOMS_FILE = "src/main/resources/data/rooms.json";
    private static final String RESERVATIONS_FILE = "src/main/resources/data/reservations.json";
    private static final Gson gson = new Gson();

    private static final ObservableList<User> userList = FXCollections.observableArrayList();
    private static final ObservableList<Room> rooms = FXCollections.observableArrayList();
    private static final ObservableList<Reservation> reservations = FXCollections.observableArrayList();

    // ---------- USER METHODS ----------

    public static void loadUsers(String filePath) {
        userList.clear();
        try (FileReader reader = new FileReader(filePath)) {
            Type type = new TypeToken<List<User>>() {}.getType();
            List<User> loadedUsers = gson.fromJson(reader, type);
            if (loadedUsers != null) userList.addAll(loadedUsers);
        } catch (IOException e) {
            System.out.println("No users.json found — initializing empty user list.");
        }
    }

    public static ObservableList<User> getUsers() {
        if (userList.isEmpty()) loadUsers(USERS_FILE);
        return userList;
    }

    public static boolean validateUser(String email, String password) {
        if (userList.isEmpty()) loadUsers(USERS_FILE);
        return userList.stream().anyMatch(u -> u.getEmail().equals(email) && u.getPassword().equals(password));
    }

    public static boolean userExists(String email) {
        if (userList.isEmpty()) loadUsers(USERS_FILE);
        return userList.stream().anyMatch(u -> u.getEmail().equals(email));
    }

    public static void addUser(String username, String email, String password, String role) {
        if (userExists(email)) return;
        User newUser = new User(username, email, password, role);
        userList.add(newUser);
        saveUsers();
    }

    public static boolean isAdmin(String email) {
        if (userList.isEmpty()) loadUsers(USERS_FILE);
        return userList.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email) && "admin".equalsIgnoreCase(u.getRole()));
    }

    public static void saveUsers() {
        try (FileWriter writer = new FileWriter(USERS_FILE)) {
            gson.toJson(userList, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------- ROOM METHODS ----------

    public static void loadRooms() {
        if (!rooms.isEmpty()) return;
        try (FileReader reader = new FileReader(ROOMS_FILE)) {
            Type type = new TypeToken<List<Room>>() {}.getType();
            List<Room> roomList = gson.fromJson(reader, type);
            if (roomList != null) rooms.addAll(roomList);
        } catch (IOException e) {
            // Ignore missing file
        }
    }

    public static ObservableList<Room> getRooms() {
        if (rooms.isEmpty()) loadRooms();
        return rooms;
    }

    public static void addRoom(Room room) {
        rooms.add(room);
        saveRooms();
    }

    public static void removeRoom(Room room) {
        rooms.remove(room);
        saveRooms();
    }

    public static void saveRooms() {
        try (FileWriter writer = new FileWriter(ROOMS_FILE)) {
            gson.toJson(rooms, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------- RESERVATION METHODS ----------

    public static void loadReservations() {
        if (!reservations.isEmpty()) return;
        try (FileReader reader = new FileReader(RESERVATIONS_FILE)) {
            Type type = new TypeToken<List<Reservation>>() {}.getType();
            List<Reservation> reservationList = gson.fromJson(reader, type);
            if (reservationList != null) reservations.addAll(reservationList);
        } catch (IOException e) {
            // Ignore missing file
        }
    }

    public static ObservableList<Reservation> getReservations() {
        if (reservations.isEmpty()) loadReservations();
        return reservations;
    }

    public static void addReservation(String username, String roomName, String date) {
        Reservation reservation = new Reservation(username, roomName, date);
        reservations.add(reservation);
        saveReservations();
    }

    /*public static void removeReservation(Reservation reservation) {
        reservations.remove(reservation);
        saveReservations();
    }*/

    public static void saveReservations() {
        try (FileWriter writer = new FileWriter(RESERVATIONS_FILE)) {
            gson.toJson(reservations, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------- UTILITY ----------

    public static void saveAll() {
        saveUsers();
        saveRooms();
        saveReservations();
    }

    public static void reloadAll() {
        userList.clear();
        rooms.clear();
        reservations.clear();
        loadUsers(USERS_FILE);
        loadRooms();
        loadReservations();
    }
}
