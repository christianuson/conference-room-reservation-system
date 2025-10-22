package model;

public class Reservation {
    private String username;
    private String roomName;
    private String date;

    // Constructor
    public Reservation(String username, String roomName, String date) {
        this.username = username;
        this.roomName = roomName;
        this.date = date;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "username='" + username + '\'' +
                ", roomName='" + roomName + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}