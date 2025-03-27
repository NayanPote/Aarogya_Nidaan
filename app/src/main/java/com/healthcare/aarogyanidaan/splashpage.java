package com.healthcare.aarogyanidaan;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class splashpage extends AppCompatActivity {

    private static final String SHARED_PREFS = "sharedPrefs";
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private TextView copyright;
    private ImageView logo;
    private TextView appname, tagline;
    private Animation topAnim, bottomAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splashpage);



        initializeUI();
        setupAnimations();
        checkAuthState();
    }

    private void initializeUI() {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        copyright = findViewById(R.id.copyright);
        logo = findViewById(R.id.logo);
        appname = findViewById(R.id.appname);
        tagline = findViewById(R.id.tagline);
        LinearProgressIndicator progressIndicator = findViewById(R.id.progress_indicator);
        // Progress indicator animation
        progressIndicator.setAlpha(0f);
        progressIndicator.setProgress(0);
        progressIndicator.animate()
                .alpha(1f)
                .setStartDelay(900)
                .setDuration(600)
                .start();

        // Animate progress
        ValueAnimator progressAnimation = ValueAnimator.ofInt(0, 100);
        progressAnimation.setDuration(2000);
        progressAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnimation.addUpdateListener(animation ->
                progressIndicator.setProgress((int) animation.getAnimatedValue()));
        progressAnimation.setStartDelay(1000);
        progressAnimation.start();

        LottieAnimationView lottieAnimationView = findViewById(R.id.lottie_animation);
        lottieAnimationView.playAnimation();
    }

    private void setupAnimations() {
        topAnim = AnimationUtils.loadAnimation(this, R.anim.top_animation);
        bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation);
        copyright.setAnimation(bottomAnim);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void checkAuthState() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        new Handler().postDelayed(() -> {
            if (isLoggedIn && auth.getCurrentUser() != null) {
                String userId = auth.getCurrentUser().getUid();
                checkUserTypeAndRedirect(userId);
            } else {
                startActivity(new Intent(splashpage.this, loginpage.class));
                finish();
            }
        }, 2000); // Show splash screen for 2 seconds
    }

    private void checkUserTypeAndRedirect(String userId) {
        // First check if user is an admin
        database.getReference("Admin").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            startActivity(new Intent(splashpage.this, Admin.class));
                            finish();
                        } else {
                            // If not admin, check if patient
                            checkInPatientBranch(userId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getApplicationContext(), "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        redirectToLogin();
                    }
                });
    }

    private void checkInPatientBranch(String userId) {
        database.getReference("patient").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            startActivity(new Intent(splashpage.this, patientdashboard.class));
                            finish();
                        } else {
                            checkInDoctorsBranch(userId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getApplicationContext(), "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        redirectToLogin();
                    }
                });
    }

    private void checkInDoctorsBranch(String userId) {
        database.getReference("doctor").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            startActivity(new Intent(splashpage.this, doctordashboard.class));
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(), "User not found in database", Toast.LENGTH_SHORT).show();
                            auth.signOut();
                            redirectToLogin();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getApplicationContext(), "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        redirectToLogin();
                    }
                });
    }

    private void redirectToLogin() {
        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();

        startActivity(new Intent(splashpage.this, loginpage.class));
        finish();
    }
}