package com.healthcare.aarogyanidaan;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RssParser {
    private static final String TAG = "RssParser";
    private static final String ns = null;
    private String feedSource; // Track the current feed source
    private String feedName; // Human-readable name for the source

    public List<Article> parse(InputStream inputStream, String source) throws XmlPullParserException, IOException {
        this.feedSource = source;
        this.feedName = extractFeedName(source);

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);

            // Look for the root element
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT) {
                eventType = parser.next();
            }

            if (eventType == XmlPullParser.END_DOCUMENT) {
                return new ArrayList<>();
            }

            // Handle different root elements (rss, feed for Atom)
            String rootElement = parser.getName();
            if ("rss".equals(rootElement)) {
                return readRssFeed(parser);
            } else if ("feed".equals(rootElement)) {
                return readAtomFeed(parser);
            } else {
                Log.w(TAG, "Unknown feed type: " + rootElement);
                return new ArrayList<>();
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing input stream", e);
            }
        }
    }

    private String extractFeedName(String url) {
        // Extract a readable name from the URL
        if (url == null) {
            return "Unknown Source";
        }

        if (url.contains("harvard.edu")) {
            return "Harvard Health";
        } else if (url.contains("medicalnewstoday")) {
            return "Medical News Today";
        } else if (url.contains("everydayhealth")) {
            return "Everyday Health";
        } else if (url.contains("webmd")) {
            return "WebMD";
        } else if (url.contains("fitness")) {
            return "Fitness News";
        } else if (url.contains("healthline")) {
            return "Healthline";
        } else if (url.contains("mayoclinic")) {
            return "Mayo Clinic";
        } else if (url.contains("nih.gov")) {
            return "NIH Health";
        } else if (url.contains("cdc.gov")) {
            return "CDC";
        } else if (url.contains("medscape")) {
            return "Medscape";
        }

        // Extract domain name as fallback
        try {
            Pattern pattern = Pattern.compile("https?://(?:www\\.)?([^/]+)");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting domain", e);
        }

        return "Health News";
    }

    private List<Article> readRssFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Article> articles = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, ns, "rss");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("channel")) {
                articles = readChannel(parser);
            } else {
                skip(parser);
            }
        }

        return articles;
    }

    private List<Article> readAtomFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Article> articles = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, ns, "feed");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("entry")) {
                Article article = readAtomEntry(parser);
                if (isHealthRelated(article)) {
                    articles.add(article);
                }
            } else {
                skip(parser);
            }
        }

        return articles;
    }

    private Article readAtomEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "entry");
        String title = null;
        String description = null;
        String link = null;
        String pubDate = null;
        String imageUrl = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            switch (name) {
                case "title":
                    title = readText(parser, "title");
                    break;
                case "summary":
                case "content":
                    description = readText(parser, name);
                    if (imageUrl == null) {
                        imageUrl = extractImageUrlFromDescription(description);
                    }
                    break;
                case "link":
                    link = parser.getAttributeValue(null, "href");
                    skip(parser);
                    break;
                case "published":
                case "updated":
                    pubDate = readText(parser, name);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        String formattedDate = formatDate(pubDate);

        if (description != null) {
            description = cleanDescription(description);
        }

        if (title == null || title.isEmpty()) {
            title = "Unknown Title";
        }

        return new Article(title, description, imageUrl, link, formattedDate, feedName);
    }

    private List<Article> readChannel(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Article> articles = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, ns, "channel");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("item")) {
                try {
                    Article article = readItem(parser);
                    if (article != null && isHealthRelated(article)) {
                        articles.add(article);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error reading item", e);
                    skip(parser); // Skip this problematic item
                }
            } else {
                skip(parser);
            }
        }

        return articles;
    }

    // Check if an article is health-related based on keywords
    private boolean isHealthRelated(Article article) {
        if (article == null || (article.getTitle() == null && article.getDescription() == null)) {
            return false;
        }

        String[] healthKeywords = {
                "health", "medical", "doctor", "hospital", "clinic", "wellness", "fitness",
                "disease", "treatment", "medicine", "healthcare", "diet", "nutrition",
                "exercise", "workout", "training", "gym", "weight loss", "strength", "cardio",
                "mental health", "yoga", "meditation", "mindfulness", "physical therapy",
                "running", "cycling", "swimming", "protein", "muscle", "recovery",
                "heart", "cancer", "diabetes", "obesity", "stroke", "hypertension", "blood pressure",
                "immune system", "vaccination", "injury", "prevention", "sleep", "stress"
        };

        String titleAndDesc = (article.getTitle() + " " + article.getDescription()).toLowerCase();

        for (String keyword : healthKeywords) {
            if (titleAndDesc.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        // If article comes from a known health source, assume it's health-related
        return article.getSource() != null &&
                (article.getSource().contains("Health") ||
                        article.getSource().contains("Medical") ||
                        article.getSource().contains("Clinic") ||
                        article.getSource().contains("WebMD") ||
                        article.getSource().contains("Fitness"));
    }

    private Article readItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "item");
        String title = null;
        String description = null;
        String link = null;
        String pubDate = null;
        String imageUrl = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            switch (name) {
                case "title":
                    title = readText(parser, "title");
                    break;
                case "description":
                    description = readText(parser, "description");
                    if (imageUrl == null) {
                        imageUrl = extractImageUrlFromDescription(description);
                    }
                    break;
                case "link":
                    link = readText(parser, "link");
                    break;
                case "pubDate":
                    pubDate = readText(parser, "pubDate");
                    break;
                case "media:content":
                case "enclosure":
                    // Some RSS feeds use these tags for images
                    String mediaUrl = parser.getAttributeValue(null, "url");
                    String type = parser.getAttributeValue(null, "type");
                    if (mediaUrl != null && (
                            (type != null && type.startsWith("image/")) ||
                                    mediaUrl.endsWith(".jpg") ||
                                    mediaUrl.endsWith(".png") ||
                                    mediaUrl.endsWith(".jpeg") ||
                                    mediaUrl.endsWith(".gif"))) {
                        imageUrl = mediaUrl;
                    }
                    skip(parser);
                    break;
                case "content:encoded":
                    // Some RSS feeds include images in content:encoded
                    String content = readText(parser, "content:encoded");
                    if (imageUrl == null) {
                        imageUrl = extractImageUrlFromDescription(content);
                    }
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        String formattedDate = formatDate(pubDate);

        if (description != null) {
            description = cleanDescription(description);
        }

        if (title == null || title.isEmpty()) {
            title = "Unknown Title";
        }

        return new Article(title, description, imageUrl, link, formattedDate, feedName);
    }

    private String cleanDescription(String description) {
        if (description == null) return null;

        // Remove HTML tags
        description = description.replaceAll("<[^>]*>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .trim();

        // Limit description length for better UI
        if (description.length() > 150) {
            description = description.substring(0, 147) + "...";
        }

        return description;
    }

    private String readText(XmlPullParser parser, String tagName) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, tagName);
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, ns, tagName);
        return result;
    }

    private String formatDate(String pubDate) {
        if (pubDate == null || pubDate.isEmpty()) {
            return "";
        }

        try {
            // Handle different date formats from different feeds
            SimpleDateFormat[] possibleFormats = {
                    new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH),
                    new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
            };

            Date date = null;
            for (SimpleDateFormat format : possibleFormats) {
                try {
                    date = format.parse(pubDate);
                    if (date != null) break;
                } catch (ParseException e) {
                    // Try next format
                }
            }

            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
                return outputFormat.format(date);
            } else {
                return pubDate;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date: " + e.getMessage());
            return pubDate;
        }
    }

    private String extractImageUrlFromDescription(String description) {
        if (description == null) {
            return null;
        }

        // Pattern to extract image URL from HTML
        Pattern pattern = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
        Matcher matcher = pattern.matcher(description);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}