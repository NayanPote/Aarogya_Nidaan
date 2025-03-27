package com.healthcare.aarogyanidaan;

public class Message {
    public static final int SENT_BY_USER = 0;
    public static final int SENT_BY_BOT = 1;
    public static final int DATE_SEPARATOR = 2;

    private String messageId;
    private String senderId;
    private String content;
    private long timestamp;
    private String conversationId;
    private boolean read;
    private String recipientId;
    private boolean notified;
    private String message;
    private int sentBy;
    private String mediaPath;
    private int mediaType; // 0 for text, 1 for image, 2 for file
    private int type;
    private int status;

    // Default constructor (needed for Firebase)
    public Message() {
        this.notified = false;
    }

    // Constructor for text messages
    public Message(String message, int sentBy) {
        this.message = message;
        this.sentBy = sentBy;
        this.timestamp = System.currentTimeMillis();
        this.mediaType = 0;
        this.notified = false;
    }

    // Constructor for messages with media
    public Message(String message, int sentBy, String mediaPath, int mediaType) {
        this(message, sentBy);
        this.mediaPath = mediaPath;
        this.mediaType = mediaType;
    }

    // Constructor with main fields
    public Message(String messageId, String senderId, String content, long timestamp,
                   String conversationId, String recipientId, boolean read) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
        this.conversationId = conversationId;
        this.recipientId = recipientId;
        this.read = read;
        this.notified = false;
        this.mediaPath = mediaPath;
        this.status = status;
    }

    // Constructor with all fields including notified
    public Message(String messageId, String senderId, String content, long timestamp,
                   String conversationId, String recipientId, boolean read, boolean notified) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
        this.conversationId = conversationId;
        this.recipientId = recipientId;
        this.read = read;
        this.notified = notified;
    }

    // Getters and Setters (keep all your existing ones)
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getSentBy() { return sentBy; }
    public void setSentBy(int sentBy) { this.sentBy = sentBy; }

    public String getMediaPath() { return mediaPath; }
    public void setMediaPath(String mediaPath) { this.mediaPath = mediaPath; }

    public int getMediaType() { return mediaType; }
    public void setMediaType(int mediaType) { this.mediaType = mediaType; }

    public boolean isDateSeparator() {
        return sentBy == DATE_SEPARATOR;
    }

    public int getStatus() {
        return status;
    }
    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }


}