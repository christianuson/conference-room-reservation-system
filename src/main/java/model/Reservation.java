package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Reservation {
    private String username;
    private String roomName;
    private String date;
    private String startTime;  // NEW: e.g., "09:00"
    private String endTime;    // NEW: e.g., "11:00"
    private String status;     // NEW: "pending" or "approved"

    // Legacy constructor (for backwards compatibility)
    public Reservation(String username, String roomName, String date) {
        this.username = username;
        this.roomName = roomName;
        this.date = date;
        this.startTime = "00:00";
        this.endTime = "23:59";
        this.status = "pending";
    }

    // NEW: Full constructor with time range and status
    public Reservation(String username, String roomName, String date, String startTime, String endTime, String status) {
        this.username = username;
        this.roomName = roomName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status != null ? status : "pending";
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

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Helper: Get formatted time range display
    public String getTimeRange() {
        return startTime + " - " + endTime;
    }

    // Helper: Get full display string
    public String getFullDateTime() {
        return date + " " + getTimeRange();
    }

    // Helper: Check if this reservation conflicts with another time range
    public boolean conflictsWith(String checkDate, String checkStartTime, String checkEndTime) {
        if (!this.date.equals(checkDate)) {
            return false; // Different dates = no conflict
        }

        // Convert times to minutes for easier comparison
        int thisStart = timeToMinutes(this.startTime);
        int thisEnd = timeToMinutes(this.endTime);
        int checkStart = timeToMinutes(checkStartTime);
        int checkEnd = timeToMinutes(checkEndTime);

        // Check for overlap: (StartA < EndB) && (EndA > StartB)
        return (thisStart < checkEnd) && (thisEnd > checkStart);
    }

    // Helper: Convert time string "HH:MM" to minutes since midnight
    private int timeToMinutes(String time) {
        if (time == null || time.isEmpty()) return 0;
        String[] parts = time.split(":");
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return hours * 60 + minutes;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "username='" + username + '\'' +
                ", roomName='" + roomName + '\'' +
                ", date='" + date + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}