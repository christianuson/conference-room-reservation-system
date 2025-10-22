package model;

public class Reservation {
    private String roomName;
    private String username;
    private String date;

    public Reservation(String roomName, String username, String date) {
        this.roomName = roomName;
        this.username = username;
        this.date = date;
    }

    public String getRoomName() { return roomName; }
    public String getUsername() { return username; }
    public String getDate() { return date; }

    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setUsername(String username) { this.username = username; }
    public void setDate(String date) { this.date = date; }

    @Override
    public String toString() {
        return username + " reserved " + roomName + " on " + date;
    }
}
