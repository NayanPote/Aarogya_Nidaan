package com.healthcare.aarogyanidaan;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class HealthNewsManager {
    private static final String TAG = "HealthNewsManager";
    private static final String CACHE_FILE_NAME = "health_articles_cache.dat";
    private static final long CACHE_EXPIRY_TIME = 3600000; // Cache expires after 1 hour (in milliseconds)

    private static final String[] HEALTH_RSS_FEEDS = {
            "https://www.health.harvard.edu/blog/feed",
            "https://www.medicalnewstoday.com/newsfeeds/rss/medical_news_today.xml",
            "https://rss.medicalnewstoday.com/fitness.xml",
            "https://www.everydayhealth.com/rss/",
            "https://www.webmd.com/rss/feeds/default.xml",
            "https://rss.healthline.com/health-news",
            "https://rss.healthline.com/diabetesmine",
            "https://www.mayoclinic.org/rss/all-health-information-topics",
            "https://medlineplus.gov/feeds/whatsnew.xml",
            "https://www.runnersworld.com/rss",
            "https://www.menshealth.com/rss",
            "https://www.womenshealthmag.com/rss",
            "https://www.shape.com/feeds/all/rss.xml"
    };

    private Context context;
    private NewsLoadCallback callback;

    public interface NewsLoadCallback {
        void onArticlesLoaded(List<Article> articles);
        void onError(String message);
    }

    public HealthNewsManager(Context context) {
        this.context = context;
    }

    public void loadHealthArticles(ProgressBar progressBar, NewsLoadCallback callback, int limit) {
        this.callback = callback;

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // First check if we have a valid cache
        List<Article> cachedArticles = readFromCache();
        if (cachedArticles != null && !cachedArticles.isEmpty()) {
            // We have cached articles, use them and refresh in background
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            // Return top articles based on limit
            List<Article> limitedArticles = limitArticles(cachedArticles, limit);
            callback.onArticlesLoaded(limitedArticles);

            // Refresh in background if we have network
            if (isNetworkAvailable()) {
                new FetchHealthArticlesTask(context, progressBar, callback, limit, true).execute(HEALTH_RSS_FEEDS);
            }
        } else {
            // No cache or expired cache, load fresh data
            if (isNetworkAvailable()) {
                new FetchHealthArticlesTask(context, progressBar, callback, limit, false).execute(HEALTH_RSS_FEEDS);
            } else {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                callback.onError("No internet connection and no cached articles available");
            }
        }
    }

    private List<Article> limitArticles(List<Article> articles, int limit) {
        Collections.sort(articles, new Comparator<Article>() {
            @Override
            public int compare(Article a1, Article a2) {
                return Long.compare(a2.getTimestamp(), a1.getTimestamp()); // Newest first
            }
        });

        if (articles.size() <= limit) {
            return new ArrayList<>(articles);
        } else {
            return new ArrayList<>(articles.subList(0, limit));
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private List<Article> readFromCache() {
        File cacheFile = new File(context.getCacheDir(), CACHE_FILE_NAME);
        if (!cacheFile.exists()) {
            return null;
        }

        // Check if cache has expired
        if (System.currentTimeMillis() - cacheFile.lastModified() > CACHE_EXPIRY_TIME) {
            Log.d(TAG, "Cache expired");
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFile))) {
            @SuppressWarnings("unchecked")
            List<Article> articles = (List<Article>) ois.readObject();
            Log.d(TAG, "Read " + articles.size() + " articles from cache");
            return articles;
        } catch (Exception e) {
            Log.e(TAG, "Error reading cache", e);
            return null;
        }
    }

    private void writeToCache(List<Article> articles) {
        File cacheFile = new File(context.getCacheDir(), CACHE_FILE_NAME);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
            oos.writeObject(articles);
            Log.d(TAG, "Wrote " + articles.size() + " articles to cache");
        } catch (Exception e) {
            Log.e(TAG, "Error writing cache", e);
        }
    }

    private static class FetchHealthArticlesTask extends AsyncTask<String, Void, List<Article>> {
        private final WeakReference<Context> contextRef;
        private final WeakReference<ProgressBar> progressBarRef;
        private final WeakReference<NewsLoadCallback> callbackRef;
        private int limit;
        private boolean isBackgroundRefresh;
        private String errorMessage;

        FetchHealthArticlesTask(Context context, ProgressBar progressBar, NewsLoadCallback callback,
                                int limit, boolean isBackgroundRefresh) {
            this.contextRef = new WeakReference<>(context);
            this.progressBarRef = new WeakReference<>(progressBar);
            this.callbackRef = new WeakReference<>(callback);
            this.limit = limit;
            this.isBackgroundRefresh = isBackgroundRefresh;

        }

        @Override
        protected List<Article> doInBackground(String... urls) {
            List<Article> result = new ArrayList<>();
            Context context = contextRef.get();
            if (context == null) return result;

            for (String urlString : urls) {
                InputStream stream = null;
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(15000);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);

                    // Add User-Agent to avoid being blocked
                    conn.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (compatible; AarogyaNidaan/1.0; +http://www.aarogyanidaan.com)");

                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        Log.w(TAG, "HTTP error code: " + responseCode + " for URL: " + urlString);
                        continue;
                    }

                    stream = conn.getInputStream();
                    RssParser parser = new RssParser();

                    List<Article> articles = parser.parse(stream, urlString);

                    for (Article article : articles) {
                        // Set timestamp from current time if not available from parsing
                        if (article.getTimestamp() <= 0) {
                            article.setTimestamp(System.currentTimeMillis());
                        }
                        result.add(article);
                    }

                } catch (IOException | XmlPullParserException e) {
                    Log.e(TAG, "Error fetching RSS: " + urlString, e);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Error closing stream", e);
                        }
                    }
                }
            }

            // Sort by timestamp (newest first)
            Collections.sort(result, new Comparator<Article>() {
                @Override
                public int compare(Article a1, Article a2) {
                    return Long.compare(a2.getTimestamp(), a1.getTimestamp());
                }
            });

            // Cache the full list
            HealthNewsManager manager = new HealthNewsManager(context);
            manager.writeToCache(result);

            // Return limited list
            return result.size() <= limit ? result : new ArrayList<>(result.subList(0, limit));
        }

        @Override
        protected void onPostExecute(List<Article> articles) {
            Context context = contextRef.get();
            ProgressBar progressBar = progressBarRef.get();
            NewsLoadCallback callback = callbackRef.get();

            if (context == null || callback == null) {
                return;
            }

            if (progressBar != null && !isBackgroundRefresh) {
                progressBar.setVisibility(View.GONE);
            }

            if (articles != null && !articles.isEmpty()) {
                callback.onArticlesLoaded(articles);
            } else if (!isBackgroundRefresh) {
                // Only show error if this is not a background refresh
                callback.onError(errorMessage != null ?
                        errorMessage : "No health articles available");
            }
        }
    }
}