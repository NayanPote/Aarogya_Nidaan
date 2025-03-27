package com.healthcare.aarogyanidaan;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AllArticlesActivity extends AppCompatActivity {

    private RecyclerView articlesRecyclerView;
    private ProgressBar progressBar;
    private ArticleAdapter articleAdapter;
    private List<Article> articlesList = new ArrayList<>();
    private ImageButton backButton;
    private TextView titleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_articles);

        // Initialize views
        articlesRecyclerView = findViewById(R.id.allArticlesRecyclerView);
        progressBar = findViewById(R.id.allArticlesProgressBar);
        backButton = findViewById(R.id.backButton);
        titleTextView = findViewById(R.id.titleTextView);

        // Set title
        titleTextView.setText("Health & Fitness Articles");

        // Setup back button
        backButton.setOnClickListener(v -> onBackPressed());

        // Setup RecyclerView
        articlesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        articleAdapter = new ArticleAdapter(this, articlesList);
        articlesRecyclerView.setAdapter(articleAdapter);

        // Load articles
        loadAllArticles();
    }

    // In AllArticlesActivity.java - update loadAllArticles() method
    private void loadAllArticles() {
        progressBar.setVisibility(View.VISIBLE);

        HealthNewsManager healthNewsManager = new HealthNewsManager(this);
        healthNewsManager.loadHealthArticles(progressBar, new HealthNewsManager.NewsLoadCallback() {
            @Override
            public void onArticlesLoaded(List<Article> articles) {
                progressBar.setVisibility(View.GONE);

                if (articles != null && !articles.isEmpty()) {
                    articlesList.clear();
                    articlesList.addAll(articles);
                    articleAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(AllArticlesActivity.this,
                            "No health articles available",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AllArticlesActivity.this,
                        "Failed to load articles: " + message,
                        Toast.LENGTH_SHORT).show();
            }
        }, 30); // Load up to 30 articles for the full listing
    }

    private class FetchAllRssTask extends AsyncTask<String, Void, List<Article>> {
        @Override
        protected List<Article> doInBackground(String... urls) {
            List<Article> result = new ArrayList<>();

            for (String urlString : urls) {
                InputStream stream = null;
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(15000);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.connect();

                    stream = conn.getInputStream();
                    RssParser parser = new RssParser();

                    // Fix: Pass both InputStream and URL string to the parse() method
                    List<Article> articles = parser.parse(stream, urlString);

                    // Add up to 10 articles from each feed
                    int count = 0;
                    for (Article article : articles) {
                        result.add(article);
                        count++;
                        if (count >= 10) break;
                    }

                } catch (IOException | XmlPullParserException e) {
                    e.printStackTrace();
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<Article> articles) {
            progressBar.setVisibility(View.GONE);

            if (articles != null && !articles.isEmpty()) {
                articlesList.clear();
                articlesList.addAll(articles);
                articleAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(AllArticlesActivity.this,
                        "Failed to load articles",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
