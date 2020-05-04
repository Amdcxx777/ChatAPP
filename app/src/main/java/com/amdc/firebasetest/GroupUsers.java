package com.amdc.firebasetest;

public class GroupUsers {
    private String userID, date, time, status;
    public GroupUsers() {
    }

    public GroupUsers(String userID, String date, String time, String status) {
        this.userID = userID;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
