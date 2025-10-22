package model;

public class Room {
    private String name;
    private String status;

    public Room(String name, String status) {
        this.name = name;
        this.status = status;
    }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
