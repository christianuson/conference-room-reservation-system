package model;

public class Room {
    private String name;
    private String status;
    private String imagePath;  // NEW: Store image path

    // Constructor with image path
    public Room(String name, String status, String imagePath) {
        this.name = name;
        this.status = status;
        this.imagePath = imagePath;
    }

    // Constructor without image path (backwards compatibility)
    public Room(String name, String status) {
        this(name, status, null);
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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    public String toString() {
        return "Room{" +
                "name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", imagePath='" + imagePath + '\'' +
                '}';
    }
}