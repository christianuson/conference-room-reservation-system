package model;

public class Room {
    private String name;
    private String status;

    // Constructor
    public Room(String name, String status) {
        this.name = name;
        this.status = status;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Room{" +
                "name='" + name + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}