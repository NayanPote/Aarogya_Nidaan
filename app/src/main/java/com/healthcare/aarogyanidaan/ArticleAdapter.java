package com.healthcare.aarogyanidaan;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder> {

    private List<Article> articles;
    private Context context;
    private boolean isHorizontalLayout;

    public ArticleAdapter(Context context, List<Article> articles) {
        this(context, articles, false);
    }

    public ArticleAdapter(Context context, List<Article> articles, boolean isHorizontalLayout) {
        this.context = context;
        this.articles = articles;
        this.isHorizontalLayout = isHorizontalLayout;
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = isHorizontalLayout ?
                R.layout.article_item_horizontal :
                R.layout.article_item;

        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        Article article = articles.get(position);
        if (article == null) {
            Log.e("ArticleAdapter", "Article at position " + position + " is null");
            return;
        }

        // ✅ Set title (null check added)
        if (holder.titleTextView != null && !TextUtils.isEmpty(article.getTitle())) {
            holder.titleTextView.setText(article.getTitle());
            holder.titleTextView.setVisibility(View.VISIBLE);
        } else if (holder.titleTextView != null) {
            holder.titleTextView.setVisibility(View.GONE);
        }

        // ✅ Set description (null check added)
        if (holder.descriptionTextView != null && !TextUtils.isEmpty(article.getDescription())) {
            holder.descriptionTextView.setText(article.getDescription());
            holder.descriptionTextView.setVisibility(View.VISIBLE);
        } else if (holder.descriptionTextView != null) {
            holder.descriptionTextView.setVisibility(View.GONE);
        }

        // ✅ Set date and source info
        String dateSource = "";
        if (!TextUtils.isEmpty(article.getPubDate())) {
            dateSource = article.getPubDate();
        }
        if (!TextUtils.isEmpty(article.getSource())) {
            if (!dateSource.isEmpty()) {
                dateSource += " • ";
            }
            dateSource += article.getSource();
        }
        if (holder.dateTextView != null) {
            if (!TextUtils.isEmpty(dateSource)) {
                holder.dateTextView.setText(dateSource);
                holder.dateTextView.setVisibility(View.VISIBLE);
            } else {
                holder.dateTextView.setVisibility(View.GONE);
            }
        }

        // ✅ Load image with Glide and rounded corners (null checks added)
        RequestOptions requestOptions = new RequestOptions()
                .transforms(new CenterCrop(), new RoundedCorners(16));

        if (holder.imageView != null) {
            if (!TextUtils.isEmpty(article.getImageUrl())) {
                holder.imageView.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(article.getImageUrl())
                        .apply(requestOptions)
                        .placeholder(R.drawable.health_placeholder)
                        .error(R.drawable.health_placeholder)
                        .into(holder.imageView);
            } else {
                holder.imageView.setVisibility(View.GONE);
            }
        }

        // ✅ Set click listener to open article in browser
        if (holder.cardView != null) {
            holder.cardView.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(article.getLink())) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getLink()));
                    context.startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return articles != null ? articles.size() : 0;
    }

    public void updateArticles(List<Article> newArticles) {
        if (newArticles != null) {
            this.articles = newArticles;
            notifyDataSetChanged();
        }
    }

    public void addArticles(List<Article> newArticles) {
        if (newArticles != null) {
            int startPosition = this.articles.size();
            this.articles.addAll(newArticles);
            notifyItemRangeInserted(startPosition, newArticles.size());
        }
    }

    static class ArticleViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;
        TextView descriptionTextView;
        TextView dateTextView;
        CardView cardView;

        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);

            // ✅ Find views safely (use try-catch to debug)
            try {
                cardView = itemView.findViewById(R.id.articleCardView);
                imageView = itemView.findViewById(R.id.articleImage);
                titleTextView = itemView.findViewById(R.id.articleTitle);
                descriptionTextView = itemView.findViewById(R.id.articleDescription);
                dateTextView = itemView.findViewById(R.id.articleDate);
            } catch (Exception e) {
                Log.e("ArticleAdapter", "Error finding views: " + e.getMessage());
            }
        }
    }
}
