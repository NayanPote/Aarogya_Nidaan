package com.healthcare.aarogyanidaan;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private Context context;
    private List<ReviewModel> reviews;
    private String currentUserType;
    private String currentUserId;
    private String currentUserName;
    private DatabaseReference mDatabase;

    private int[] avatarColors = new int[] {
            android.graphics.Color.parseColor("#4CAF50"),
            android.graphics.Color.parseColor("#2196F3"),
            android.graphics.Color.parseColor("#FF9800"),
            android.graphics.Color.parseColor("#9C27B0"),
            android.graphics.Color.parseColor("#F44336"),
            android.graphics.Color.parseColor("#009688")
    };
    private Random random = new Random();

    // Constructor for regular users
    public ReviewAdapter(Context context, List<ReviewModel> reviews) {
        this.context = context;
        this.reviews = reviews;
        this.currentUserType = "patient"; // Default to patient
        this.currentUserId = "";
        this.currentUserName = "";
    }

    // Constructor with user information for admin functionality
    public ReviewAdapter(Context context, List<ReviewModel> reviews, String userType, String userId, String userName) {
        this.context = context;
        this.reviews = reviews;
        this.currentUserType = userType;
        this.currentUserId = userId;
        this.currentUserName = userName;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ReviewModel review = reviews.get(position);

        // Check for null values before using them
        String userName = (review.getUserName() != null) ? review.getUserName() : "Anonymous";
        String userType = (review.getUserType() != null) ? review.getUserType() : "";
        String userIdentifier = (review.getUserIdentifier() != null) ? review.getUserIdentifier() : "N/A";
        String reviewText = (review.getReview() != null) ? review.getReview() : "";

        // Set avatar based on user type
        if ("doctor".equals(userType)) {
            holder.imgUserAvatar.setImageResource(R.drawable.doctoravatar2);
        } else {
            holder.imgUserAvatar.setImageResource(R.drawable.patientavatar2);
        }

        // Set background color for the avatar
        holder.imgUserAvatar.setBackgroundColor(
                avatarColors[Math.abs(userName.hashCode()) % avatarColors.length]
        );

        // Set reviewer information
        if ("doctor".equals(userType)) {
            holder.tvReviewerName.setText("Dr. " + userName);
        } else {
            holder.tvReviewerName.setText(userName);
        }
        holder.tvReviewerId.setText(userIdentifier);

        // Set rating safely
        Float rating = review.getRating();
        holder.ratingBarReview.setRating((rating != null) ? rating : 0.0f);

        // Format date for display safely
        String formattedDate = (review.getFormattedDate() != null) ? formatDate(review.getFormattedDate()) : "Unknown Date";
        holder.tvReviewDate.setText(formattedDate);

        // Set review content
        if (!reviewText.isEmpty()) {
            holder.tvReviewContent.setText(reviewText);
            holder.tvReviewContent.setVisibility(View.VISIBLE);
        } else {
            holder.tvReviewContent.setVisibility(View.GONE);
        }

        // Handle admin reply
        if (review.getAdminReply() != null) {
            holder.layoutAdminReply.setVisibility(View.VISIBLE);
            holder.tvAdminReplyName.setText("Admin: " + review.getAdminReply().getAdminName());
            holder.tvAdminReplyDate.setText(formatDate(review.getAdminReply().getFormattedDate()));
            holder.tvAdminReplyContent.setText(review.getAdminReply().getReplyContent());
        } else {
            holder.layoutAdminReply.setVisibility(View.GONE);
        }

        // Show add/edit reply option only for admins
        if ("admin".equals(currentUserType)) {
            holder.btnAddReply.setVisibility(View.VISIBLE);
            holder.btnAddReply.setText(review.getAdminReply() != null ? "Edit Reply" : "Add Reply");
            holder.btnAddReply.setOnClickListener(v -> showReplyDialog(review));
        } else {
            holder.btnAddReply.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    // Helper method to format date
    private String formatDate(String timestampStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(timestampStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return timestampStr;
        }
    }

    private void showReplyDialog(ReviewModel review) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Admin Reply");

        // Set up the input
        final EditText input = new EditText(context);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setLines(5);
        input.setGravity(Gravity.TOP | Gravity.START);

        // Pre-fill with existing reply if any
        if (review.getAdminReply() != null) {
            input.setText(review.getAdminReply().getReplyContent());
        }

        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Submit", (dialog, which) -> {
            String replyContent = input.getText().toString().trim();
            if (!TextUtils.isEmpty(replyContent)) {
                submitReply(review, replyContent);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void submitReply(ReviewModel review, String replyContent) {
        ReviewReply reply = new ReviewReply();
        reply.setAdminId(currentUserId);
        reply.setAdminName(currentUserName);
        reply.setReplyContent(replyContent);
        reply.setTimestamp(System.currentTimeMillis());
        reply.setFormattedDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date()));

        // Find the review in the database and update it
        mDatabase.child("reviews").orderByChild("timestamp").equalTo(review.getTimestamp())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String reviewKey = snapshot.getKey();
                            if (reviewKey != null) {
                                mDatabase.child("reviews").child(reviewKey).child("adminReply")
                                        .setValue(reply)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(context, "Reply submitted successfully", Toast.LENGTH_SHORT).show();
                                            // Update the review object in the list and notify adapter
                                            review.setAdminReply(reply);
                                            notifyDataSetChanged();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(context, "Failed to submit reply: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(context, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgUserAvatar;
        TextView tvReviewerName, tvReviewerId, tvReviewDate, tvReviewContent;
        RatingBar ratingBarReview;
        // Admin reply components
        LinearLayout layoutAdminReply;
        TextView tvAdminReplyName, tvAdminReplyDate, tvAdminReplyContent;
        Button btnAddReply;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUserAvatar = itemView.findViewById(R.id.imgUserAvatar);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            tvReviewerId = itemView.findViewById(R.id.tvReviewerId);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            tvReviewContent = itemView.findViewById(R.id.tvReviewContent);
            ratingBarReview = itemView.findViewById(R.id.ratingBarReview);

            // Admin reply components
            layoutAdminReply = itemView.findViewById(R.id.layoutAdminReply);
            tvAdminReplyName = itemView.findViewById(R.id.tvAdminReplyName);
            tvAdminReplyDate = itemView.findViewById(R.id.tvAdminReplyDate);
            tvAdminReplyContent = itemView.findViewById(R.id.tvAdminReplyContent);
            btnAddReply = itemView.findViewById(R.id.btnAddReply);
        }
    }
}