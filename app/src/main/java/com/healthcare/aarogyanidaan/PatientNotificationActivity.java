package com.healthcare.aarogyanidaan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PatientNotificationActivity extends AppCompatActivity {

    private static final String TAG = "PatientNotification";

    // UI Components
    private ImageButton backButton, notificationSettings;
    private RecyclerView recyclerViewNotifications;
    private ProgressBar progressBar;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // Data
    private List<AdminNotificationActivity.NotificationModel> notificationList;
    private NotificationAdapter notificationAdapter;

    // Notification permission launcher
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_patientnotification);

        // Configure window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Register for notification permission results (Android 13+)
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Notification permission denied. You may miss important healthcare updates.",
                                Toast.LENGTH_LONG).show();
                    }
                }
        );

        notificationSettings = findViewById(R.id.notificationsettings);
        notificationSettings.setOnClickListener(v -> {
            startActivity(new Intent(PatientNotificationActivity.this, NotificationSchedulerActivity.class));
        });

        // Request notification permission for Android 13+
        requestNotificationPermission();

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI components
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();

        // Load notifications
        loadNotifications();

        // Register for new notifications in the background
        registerForNotifications();
    }

    private void requestNotificationPermission() {
        // For Android 13 (API 33) and above, we need to request POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(notificationList);
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotifications.setAdapter(notificationAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.purple_500);
        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        // Get current user ID
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            handleError("You must be logged in to view notifications");
            return;
        }

        // Query for notifications that are:
        // 1. Targeted to all users OR
        // 2. Targeted to patients OR
        // 3. Specifically targeted to this user
        Query notificationsQuery = mDatabase.child("notifications");

        notificationsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                notificationList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    AdminNotificationActivity.NotificationModel notification =
                            snapshot.getValue(AdminNotificationActivity.NotificationModel.class);

                    if (notification != null) {
                        notification.setId(snapshot.getKey());

                        // Check if notification has expired
                        if (!notification.isPermanent() && notification.getExpirationTime() > 0
                                && System.currentTimeMillis() > notification.getExpirationTime()) {
                            // Skip expired notifications
                            continue;
                        }

                        // Check if this notification is for the current user
                        String targetUsers = notification.getTargetUsers();
                        if ("all".equals(targetUsers) || "patient".equals(targetUsers)) {
                            notificationList.add(notification);
                        } else if ("specific".equals(targetUsers)) {
                            List<String> specificUserIds = notification.getSpecificUserIds();
                            if (specificUserIds != null && specificUserIds.contains(currentUserId)) {
                                notificationList.add(notification);
                            }
                        }
                    }
                }

                // Sort notifications by timestamp (newest first)
                Collections.sort(notificationList, (n1, n2) ->
                        Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                notificationAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                // Show empty view if no notifications
                if (notificationList.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    emptyView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                handleError("Error loading notifications: " + databaseError.getMessage());
            }
        });
    }

    private void registerForNotifications() {
        // Get current user ID
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            return;
        }

        // Listen for new notifications in the user's notification node
        mDatabase.child("userNotifications").child(currentUserId).addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Refresh the notification list when there are changes
                        loadNotifications();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error listening for notifications: " + databaseError.getMessage());
                    }
                });

        // Also listen for global notifications
        mDatabase.child("notificationAlerts").addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Refresh the notification list when there are global changes
                        loadNotifications();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error listening for global notifications: " + databaseError.getMessage());
                    }
                });
    }

    private void handleError(String errorMessage) {
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        Log.e(TAG, errorMessage);
    }

    // Adapter for RecyclerView
    static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

        private final List<AdminNotificationActivity.NotificationModel> notificationList;

        public NotificationAdapter(List<AdminNotificationActivity.NotificationModel> notificationList) {
            this.notificationList = notificationList;
        }

        @NonNull
        @Override
        public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_notification, parent, false);
            return new NotificationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
            AdminNotificationActivity.NotificationModel notification = notificationList.get(position);

            holder.textTitle.setText(notification.getTitle());
            holder.textMessage.setText(notification.getMessage());

            // Format timestamp into readable date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(new Date(notification.getTimestamp()));
            holder.textTimestamp.setText(formattedDate);

            // Set priority indicator
            String priority = notification.getPriority();
            if (priority != null) {
                if (priority.equalsIgnoreCase("High")) {
                    holder.priorityIndicator.setBackgroundResource(R.drawable.priority_high);
                    holder.textPriority.setText("High Priority");
                    holder.textPriority.setVisibility(View.VISIBLE);
                } else if (priority.equalsIgnoreCase("Medium")) {
                    holder.priorityIndicator.setBackgroundResource(R.drawable.priority_medium);
                    holder.textPriority.setVisibility(View.GONE);
                } else {
                    holder.priorityIndicator.setBackgroundResource(R.drawable.priority_low);
                    holder.textPriority.setVisibility(View.GONE);
                }
            }

            // Set notification type icon
            String type = notification.getType();
            if (type != null && type.equals("bot")) {
                holder.textType.setText("Health Assistant");
                holder.textType.setCompoundDrawablesWithIntrinsicBounds(R.drawable.chatbot, 0, 0, 0);
            } else {
                holder.textType.setText("Aarogya Nidaan");
                holder.textType.setCompoundDrawablesWithIntrinsicBounds(R.drawable.aarogyanidaanlogo, 0, 0, 0);
            }
        }

        @Override
        public int getItemCount() {
            return notificationList.size();
        }

        static class NotificationViewHolder extends RecyclerView.ViewHolder {
            TextView textTitle, textMessage, textTimestamp, textType, textPriority;
            View priorityIndicator;

            public NotificationViewHolder(@NonNull View itemView) {
                super(itemView);
                textTitle = itemView.findViewById(R.id.textNotificationTitle);
                textMessage = itemView.findViewById(R.id.textNotificationMessage);
                textTimestamp = itemView.findViewById(R.id.textNotificationTimestamp);
                textType = itemView.findViewById(R.id.textNotificationType);
                textPriority = itemView.findViewById(R.id.textPriority);
                priorityIndicator = itemView.findViewById(R.id.priorityIndicator);
            }
        }
    }
}