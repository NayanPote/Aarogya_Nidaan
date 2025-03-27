package com.healthcare.aarogyanidaan;

import java.util.Objects;

public class Conversation {
    private String conversationId;
    private String doctorId;
    private String patientId;
    private String doctorName;
    private String patientName;
    private String doctorSpecialization;
    private long lastMessageTimestamp;
    private String lastMessage;
    private String lastMessageSenderId;  // Added field
    private int unreadCount;
    private String unreadCountForUser;   // Added field

    // No-argument constructor (Required for Firebase)
    public Conversation() {
    }

    // Primary Constructor
    public Conversation(String conversationId, String doctorId, String patientId,
                        String doctorName, String doctorSpecialization, String patientName,
                        long lastMessageTimestamp) {
        this.conversationId = conversationId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.doctorName = doctorName;
        this.doctorSpecialization = doctorSpecialization;
        this.patientName = patientName;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastMessage = "";           // Default value
        this.lastMessageSenderId = "";   // Default value
        this.unreadCount = 0;            // Default value
        this.unreadCountForUser = "";    // Default value
    }

    // Secondary Constructor (Without Specialization)
    public Conversation(String conversationId, String doctorId, String patientId,
                        String doctorName, String patientName, long lastMessageTimestamp) {
        this(conversationId, doctorId, patientId, doctorName, "", patientName, lastMessageTimestamp);
    }

    // Methods to Manage Messages and Unread Count
    public void updateLastMessage(String message, String senderId, long timestamp) {
        this.lastMessage = message;
        this.lastMessageSenderId = senderId;
        this.lastMessageTimestamp = timestamp;
    }

    public void incrementUnreadCount(String userId) {
        this.unreadCount++;
        this.unreadCountForUser = userId;
    }

    public void clearUnreadCount() {
        this.unreadCount = 0;
        this.unreadCountForUser = "";
    }

    // Getters and Setters
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorSpecialization() { return doctorSpecialization; }
    public void setDoctorSpecialization(String doctorSpecialization) { this.doctorSpecialization = doctorSpecialization; }

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public String getUnreadCountForUser() { return unreadCountForUser; }
    public void setUnreadCountForUser(String unreadCountForUser) { this.unreadCountForUser = unreadCountForUser; }

    // Helper Methods
    public String getId() {
        return conversationId;
    }

    public boolean hasUnreadMessages(String userId) {
        return unreadCount > 0 && userId.equals(unreadCountForUser);
    }

    public boolean isUserRecipient(String userId) {
        return lastMessageSenderId != null && !userId.equals(lastMessageSenderId);
    }

    // Overridden Methods for Correct Object Comparison
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return Objects.equals(conversationId, that.conversationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId);
    }
}