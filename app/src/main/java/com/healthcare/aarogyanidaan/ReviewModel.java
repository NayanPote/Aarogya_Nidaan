package com.healthcare.aarogyanidaan;

public class ReviewModel {
    private String userId;
    private String userName;
    private String userType;
    private String userIdentifier;
    private float rating;
    private String review;
    private long timestamp;
    private String formattedDate;

    private ReviewReply adminReply;

    // Required empty constructor for Firebase
    public ReviewModel() {
    }

    // Constructor with parameters
    public ReviewModel(String userId, String userName, String userType, String userIdentifier,
                       float rating, String review, long timestamp, String formattedDate) {
        this.userId = userId;
        this.userName = userName;
        this.userType = userType;
        this.userIdentifier = userIdentifier;
        this.rating = rating;
        this.review = review;
        this.timestamp = timestamp;
        this.formattedDate = formattedDate;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public ReviewReply getAdminReply() { return adminReply; }
    public void setAdminReply(ReviewReply adminReply) { this.adminReply = adminReply; }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public void setFormattedDate(String formattedDate) {
        this.formattedDate = formattedDate;
    }
}