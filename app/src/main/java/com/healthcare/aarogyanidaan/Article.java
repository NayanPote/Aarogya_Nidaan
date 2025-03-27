package com.healthcare.aarogyanidaan;

import java.io.Serializable;

public class Article implements Serializable {
    private String title;
    private String description;
    private String imageUrl;
    private String link;
    private String pubDate;
    private String source; // Track which RSS feed the article came from
    private long timestamp; // For sorting by recency

    public Article(String title, String description, String imageUrl, String link, String pubDate, String source) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.link = link;
        this.pubDate = pubDate;
        this.source = source;
        this.timestamp = System.currentTimeMillis(); // Default to current time if we can't parse the date
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLink() {
        return link;
    }

    public String getPubDate() {
        return pubDate;
    }

    public String getSource() {
        return source;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // For debugging purposes
    @Override
    public String toString() {
        return "Article{" +
                "title='" + title + '\'' +
                ", source='" + source + '\'' +
                ", pubDate='" + pubDate + '\'' +
                '}';
    }
}