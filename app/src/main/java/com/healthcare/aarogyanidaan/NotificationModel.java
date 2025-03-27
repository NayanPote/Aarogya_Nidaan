package com.healthcare.aarogyanidaan;

import java.util.List;

public class NotificationModel {
    private String id;
    private String title;
    private String message;
    private String type;
    private boolean isPermanent;
    private long timestamp;
    private long expirationTime;
    private String priority;
    private String targetUsers;
    private List<String> specificUserIds;

    public NotificationModel() {
        // Required empty constructor for Firebase
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isPermanent() {
        return isPermanent;
    }

    public void setPermanent(boolean permanent) {
        isPermanent = permanent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getTargetUsers() {
        return targetUsers;
    }

    public void setTargetUsers(String targetUsers) {
        this.targetUsers = targetUsers;
    }

    public List<String> getSpecificUserIds() {
        return specificUserIds;
    }

    public void setSpecificUserIds(List<String> specificUserIds) {
        this.specificUserIds = specificUserIds;
    }
}
