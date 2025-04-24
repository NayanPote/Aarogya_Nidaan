package com.healthcare.aarogyanidaan;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppRatingandfeedbacks extends AppCompatActivity {

    // Firebase references
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;
    private String userType; // "doctor" or "patient"
    private String userName;
    private String userIdentifier;

    // UI elements
    private TextView tvAverageRating, tvTotalRatings, tvUserInfo;
    private RatingBar ratingBarAverage, ratingBarUser;
    private EditText etFeedback;
    private Button btnSubmitReview;
    private RecyclerView recyclerViewReviews;
    private ProgressBar[] progressBars = new ProgressBar[5];
    private TextView[] tvCounts = new TextView[5];

    private CardView cardWriteReview;

    // Review adapter
    private ReviewAdapter reviewAdapter;
    private List<ReviewModel> reviewList;

    // Rating statistics
    private float averageRating = 0;
    private int totalRatings = 0;
    private int[] ratingCounts = new int[5]; // Counts for 5, 4, 3, 2, 1 stars

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_app_ratingandfeedbacks);

        // Setup edge to edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI elements
        initializeUI();

        // Get current user details
        getCurrentUserDetails();

        // Load ratings and reviews
        loadRatingsStatistics();
        loadReviews();

        // Setup click listener for submit button
        btnSubmitReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReview();
            }
        });
    }

    private void initializeUI() {
        // Rating summary views
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvTotalRatings = findViewById(R.id.tvTotalRatings);
        ratingBarAverage = findViewById(R.id.ratingBarAverage);

        // Distribution views
        progressBars[0] = findViewById(R.id.progressBar5);
        progressBars[1] = findViewById(R.id.progressBar4);
        progressBars[2] = findViewById(R.id.progressBar3);
        progressBars[3] = findViewById(R.id.progressBar2);
        progressBars[4] = findViewById(R.id.progressBar1);

        tvCounts[0] = findViewById(R.id.tvCount5);
        tvCounts[1] = findViewById(R.id.tvCount4);
        tvCounts[2] = findViewById(R.id.tvCount3);
        tvCounts[3] = findViewById(R.id.tvCount2);
        tvCounts[4] = findViewById(R.id.tvCount1);

        // User review views
        tvUserInfo = findViewById(R.id.tvUserInfo);
        ratingBarUser = findViewById(R.id.ratingBarUser);
        etFeedback = findViewById(R.id.etFeedback);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        cardWriteReview = findViewById(R.id.cardWriteReview);

        // RecyclerView setup
        recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));

        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList, userType, userId, userName);
        recyclerViewReviews.setAdapter(reviewAdapter);
    }

    private void getCurrentUserDetails() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();

            // First check if user is admin
            mDatabase.child("Admin").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // User is an admin
                        userType = "admin";
                        userName = dataSnapshot.child("email").getValue(String.class);
                        userIdentifier = "Admin";

                        // Hide the write review card for admins
                        if (cardWriteReview != null) {
                            cardWriteReview.setVisibility(View.GONE);
                        }

                        // Show admin info if needed
                        if (tvUserInfo != null) {
                            tvUserInfo.setText("Admin: " + userName);
                        }

                        // Update the adapter with admin credentials
                        reviewAdapter = new ReviewAdapter(AppRatingandfeedbacks.this, reviewList, userType, userId, userName);
                        recyclerViewReviews.setAdapter(reviewAdapter);
                        reviewAdapter.notifyDataSetChanged();

                        // Show admin-specific UI elements or instructions
                        TextView tvAdminInstructions = new TextView(AppRatingandfeedbacks.this);
                        tvAdminInstructions.setText("As an admin, you can reply to user reviews. Tap on a review to add or edit your reply.");
                        tvAdminInstructions.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                        tvAdminInstructions.setPadding(16, 16, 16, 16);

                        // Locate the main layout correctly
                        ViewGroup mainLayout = findMainLayout();
                        if (mainLayout != null) {
                            // Ensure recyclerViewReviews exists before adding instructions
                            View recyclerView = findViewById(R.id.recyclerViewReviews);
                            if (recyclerView != null) {
                                mainLayout.addView(tvAdminInstructions, mainLayout.indexOfChild(recyclerView));
                            } else {
                                mainLayout.addView(tvAdminInstructions); // Add at the end if RecyclerView is not found
                            }
                        }
                    } else {
                        // Continue with existing checks for doctor/patient
                        checkIfDoctor();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(AppRatingandfeedbacks.this,
                            "Failed to load user details: " + databaseError.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // If no user is logged in, redirect to login screen
            Toast.makeText(this, "Please log in to view reviews", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Finds the main layout that contains views inside the NestedScrollView or CoordinatorLayout.
     * @return The ViewGroup that should contain the instructions, or null if not found.
     */
    private ViewGroup findMainLayout() {
        View rootView = findViewById(R.id.main); // This might be a CoordinatorLayout
        if (rootView instanceof ViewGroup) {
            ViewGroup rootGroup = (ViewGroup) rootView;

            // Try finding NestedScrollView inside CoordinatorLayout
            NestedScrollView nestedScrollView = rootGroup.findViewById(R.id.nestedScrollView);
            if (nestedScrollView != null && nestedScrollView.getChildCount() > 0) {
                View firstChild = nestedScrollView.getChildAt(0);
                if (firstChild instanceof ViewGroup) {
                    return (ViewGroup) firstChild; // The actual container inside NestedScrollView
                }
            }

            // If no NestedScrollView, assume root itself is the main layout
            return rootGroup;
        }
        return null;
    }


    // Rename the existing method to checkIfDoctor and modify it
    private void checkIfDoctor() {
        // Your existing code from getCurrentUserDetails that checks if user is a doctor
        mDatabase.child("doctor").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User is a doctor
                    userType = "doctor";
                    userName = dataSnapshot.child("doctor_name").getValue(String.class);
                    userIdentifier = dataSnapshot.child("doctor_id").getValue(String.class);
                    tvUserInfo.setText("Dr. " + userName + " (" + userIdentifier + ")");

                    // Check if user has already submitted a review
                    checkPreviousReview();
                } else {
                    // If not a doctor, check if they are a patient
                    checkIfPatient();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Existing error handling
            }
        });
    }

    private void checkIfPatient() {
        mDatabase.child("patient").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User is a patient
                    userType = "patient";
                    userName = dataSnapshot.child("patient_name").getValue(String.class);
                    userIdentifier = dataSnapshot.child("patient_id").getValue(String.class);
                    tvUserInfo.setText(userName + " (" + userIdentifier + ")");

                    // Check if user has already submitted a review
                    checkPreviousReview();
                } else {
                    // User not found in either collection
                    Toast.makeText(AppRatingandfeedbacks.this,
                            "User profile not found",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AppRatingandfeedbacks.this,
                        "Failed to load user details: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPreviousReview() {
        mDatabase.child("reviews").orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                ReviewModel review = snapshot.getValue(ReviewModel.class);
                                if (review != null) {
                                    // Pre-fill the form with existing review
                                    ratingBarUser.setRating(review.getRating());
                                    etFeedback.setText(review.getReview());
                                    btnSubmitReview.setText("Update Review");
                                    break;
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(AppRatingandfeedbacks.this,
                                "Failed to check previous reviews: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadRatingsStatistics() {
        mDatabase.child("reviews").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Reset counters
                totalRatings = 0;
                float ratingSum = 0;
                for (int i = 0; i < 5; i++) {
                    ratingCounts[i] = 0;
                }

                // Calculate statistics
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ReviewModel review = snapshot.getValue(ReviewModel.class);
                    if (review != null) {
                        totalRatings++;
                        ratingSum += review.getRating();

                        // Increment proper star count (5 stars at index 0, 1 star at index 4)
                        int starIndex = 5 - Math.round(review.getRating());
                        if (starIndex >= 0 && starIndex < 5) {
                            ratingCounts[starIndex]++;
                        }
                    }
                }

                // Calculate average rating
                if (totalRatings > 0) {
                    averageRating = ratingSum / totalRatings;
                    tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f", averageRating));
                    ratingBarAverage.setRating(averageRating);
                    tvTotalRatings.setText(String.format(Locale.getDefault(), "%,d ratings", totalRatings));
                } else {
                    tvAverageRating.setText("0.0");
                    ratingBarAverage.setRating(0);
                    tvTotalRatings.setText("0 ratings");
                }

                // Update distribution bars
                updateRatingDistribution();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AppRatingandfeedbacks.this,
                        "Failed to load ratings: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRatingDistribution() {
        // Calculate percentages for progress bars
        for (int i = 0; i < 5; i++) {
            int percentage = totalRatings > 0 ? (ratingCounts[i] * 100 / totalRatings) : 0;
            progressBars[i].setProgress(percentage);
            tvCounts[i].setText(String.valueOf(ratingCounts[i]));
        }
    }

    private void loadReviews() {
        mDatabase.child("reviews").orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                reviewList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ReviewModel review = snapshot.getValue(ReviewModel.class);
                    if (review != null) {
                        reviewList.add(review);
                    }
                }

                // Sort by timestamp (newest first)
                Collections.sort(reviewList, (r1, r2) ->
                        Long.compare(r2.getTimestamp(), r1.getTimestamp()));

                if ("admin".equals(userType)) {
                    reviewAdapter = new ReviewAdapter(AppRatingandfeedbacks.this, reviewList, userType, userId, userName);
                    recyclerViewReviews.setAdapter(reviewAdapter);
                } else {
                    reviewAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AppRatingandfeedbacks.this,
                        "Failed to load reviews: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void submitReview() {
        float rating = ratingBarUser.getRating();
        String feedback = etFeedback.getText().toString().trim();

        // Validate input
        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent multiple submissions
        btnSubmitReview.setEnabled(false);

        // Create review model
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        ReviewModel review = new ReviewModel();
        review.setUserId(userId);
        review.setUserName(userName);
        review.setUserType(userType);
        review.setUserIdentifier(userIdentifier);
        review.setRating(rating);
        review.setReview(feedback);
        review.setTimestamp(System.currentTimeMillis());
        review.setFormattedDate(timestamp);

        // Generate a unique key or find existing review
        mDatabase.child("reviews").orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String reviewKey;
                        boolean isUpdate = false;

                        if (dataSnapshot.exists()) {
                            // Update existing review
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                reviewKey = snapshot.getKey();
                                isUpdate = true;
                                saveReviewToFirebase(reviewKey, review, isUpdate);
                                break;
                            }
                        } else {
                            // Create new review
                            reviewKey = mDatabase.child("reviews").push().getKey();
                            saveReviewToFirebase(reviewKey, review, isUpdate);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        btnSubmitReview.setEnabled(true);
                        Toast.makeText(AppRatingandfeedbacks.this,
                                "Failed to submit review: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveReviewToFirebase(String reviewKey, ReviewModel review, boolean isUpdate) {
        if (reviewKey != null) {
            mDatabase.child("reviews").child(reviewKey).setValue(review)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            btnSubmitReview.setEnabled(true);

                            if (task.isSuccessful()) {
                                String message = isUpdate ? "Review updated successfully" : "Review submitted successfully";
                                Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_SHORT).show();

                                // If it's a new review, clear the form
                                if (!isUpdate) {
                                    ratingBarUser.setRating(0);
                                    etFeedback.setText("");
                                }
                            } else {
                                Toast.makeText(AppRatingandfeedbacks.this,
                                        "Failed to submit review: " + task.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}