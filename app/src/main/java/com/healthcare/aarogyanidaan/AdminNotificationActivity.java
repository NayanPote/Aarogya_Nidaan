package com.healthcare.aarogyanidaan;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AdminNotificationActivity extends AppCompatActivity {

    // Channel IDs for notifications
    private static final String CHANNEL_ID_STANDARD = "aarogya_nidaan_standard_channel";
    private static final String CHANNEL_ID_BOT = "aarogya_nidaan_bot_channel";
    private static final String TAG = "AdminNotificationActivity";

    // UI Components
    private ImageButton backButton;
    private TextInputEditText editTextTitle, editTextMessage, editTextUserIds;
    private TextInputLayout layoutUserIds;
    private RadioGroup radioGroupNotificationType, radioGroupDuration, radioGroupTargetUsers;
    private RadioButton radioStandard, radioBot, radioTemporary, radioPermanent, radioAllUsers,
            radioPatient, radioDoctor, radioSpecificUsers;
    private Spinner spinnerPriority;
    private MaterialButton buttonSendNotification;
    private ProgressBar progressBar;
    private TextView textViewResult;
    private RecyclerView recyclerViewNotifications;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // Adapter for RecyclerView
    private NotificationAdapter notificationAdapter;
    private List<NotificationModel> notificationList;

    // Notification permission launcher for Android 13+
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_notification);

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
                        // Permission granted
                        Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                    } else {
                        // Permission denied
                        Toast.makeText(this, "Notification permission denied. Some features may not work properly.",
                                Toast.LENGTH_LONG).show();
                    }
                }
        );

        // Request notification permission for Android 13+
        requestNotificationPermission();

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Create notification channels
        createNotificationChannels();

        // Initialize UI components
        initializeViews();
        setupToolbar();
        setupListeners();
        setupRecyclerView();
        loadNotifications();

        // Set up a listener for notification alerts to display local notifications
        setupNotificationAlertListener();
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

    private void createNotificationChannels() {
        // Only needed for Android 8.0 (API level 26) and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // Create the Standard notification channel
            NotificationChannel standardChannel = new NotificationChannel(
                    CHANNEL_ID_STANDARD,
                    "Aarogya Nidaan Standard Notifications",
                    NotificationManager.IMPORTANCE_HIGH); // Use HIGH for Android 10+
            standardChannel.setDescription("Standard notifications from Aarogya Nidaan");
            standardChannel.enableLights(true);
            standardChannel.setLightColor(Color.RED);
            standardChannel.enableVibration(true);
            standardChannel.setVibrationPattern(new long[]{0, 250, 250, 250});
            standardChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(standardChannel);

            // Create the Bot notification channel
            NotificationChannel botChannel = new NotificationChannel(
                    CHANNEL_ID_BOT,
                    "Aarogya Nidaan Bot Notifications",
                    NotificationManager.IMPORTANCE_HIGH); // Use HIGH for Android 10+
            botChannel.setDescription("Chatbot notifications from Aarogya Nidaan");
            botChannel.enableLights(true);
            botChannel.setLightColor(Color.BLUE);
            botChannel.enableVibration(true);
            botChannel.setVibrationPattern(new long[]{0, 250, 250, 250});
            botChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(botChannel);
        }
    }

    private void initializeViews() {
        // EditTexts
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextMessage = findViewById(R.id.editTextMessage);
        editTextUserIds = findViewById(R.id.editTextUserIds);

        // TextInputLayouts
        layoutUserIds = findViewById(R.id.layoutUserIds);

        // RadioGroups and RadioButtons
        radioGroupNotificationType = findViewById(R.id.radioGroupNotificationType);
        radioGroupDuration = findViewById(R.id.radioGroupDuration);
        radioGroupTargetUsers = findViewById(R.id.radioGroupTargetUsers);

        radioStandard = findViewById(R.id.radioStandard);
        radioBot = findViewById(R.id.radioBot);
        radioTemporary = findViewById(R.id.radioTemporary);
        radioPermanent = findViewById(R.id.radioPermanent);
        radioAllUsers = findViewById(R.id.radioAllUsers);
        radioPatient = findViewById(R.id.radioPatient);
        radioDoctor = findViewById(R.id.radioDoctor);
        radioSpecificUsers = findViewById(R.id.radioSpecificUsers);

        // Spinner
        spinnerPriority = findViewById(R.id.spinnerPriority);

        // Button
        buttonSendNotification = findViewById(R.id.buttonSendNotification);
        backButton = findViewById(R.id.backbutton);

        // Other Views
        progressBar = findViewById(R.id.progressBar);
        textViewResult = findViewById(R.id.textViewResult);
        recyclerViewNotifications = findViewById(R.id.sentnotifications);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupListeners() {
        // Back button click listener
        backButton.setOnClickListener(v -> onBackPressed());

        // Target users radio group listener
        radioGroupTargetUsers.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioSpecificUsers) {
                layoutUserIds.setVisibility(View.VISIBLE);
            } else {
                layoutUserIds.setVisibility(View.GONE);
            }
        });

        // Send notification button click listener
        buttonSendNotification.setOnClickListener(v -> validateAndSendNotification());
    }

    private void setupRecyclerView() {
        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(notificationList, this::deleteNotification);
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotifications.setAdapter(notificationAdapter);
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.child("notifications").orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                notificationList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    NotificationModel notification = snapshot.getValue(NotificationModel.class);
                    if (notification != null) {
                        notification.setId(snapshot.getKey());

                        // Check if notification has expired
                        if (!notification.isPermanent() && notification.getExpirationTime() > 0
                                && System.currentTimeMillis() > notification.getExpirationTime()) {
                            // Auto-delete expired notification
                            deleteNotification(notification.getId());
                        } else {
                            notificationList.add(notification);
                        }
                    }
                }

                // Sort notifications by timestamp (newest first)
                Collections.sort(notificationList, (n1, n2) ->
                        Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                notificationAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AdminNotificationActivity.this,
                        "Error loading notifications: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void setupNotificationAlertListener() {
        mDatabase.child("notificationAlerts").addChildEventListener(new com.google.firebase.database.ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                if (snapshot.exists()) {
                    String notificationId = snapshot.child("notificationId").getValue(String.class);
                    if (notificationId != null) {
                        // Fetch the actual notification
                        mDatabase.child("notifications").child(notificationId).addListenerForSingleValueEvent(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        NotificationModel notification = dataSnapshot.getValue(NotificationModel.class);
                                        if (notification != null) {
                                            // We'll check user eligibility asynchronously
                                            checkUserEligibilityAndShowNotification(notification);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        // Handle error
                                    }
                                });
                    }

                    // Clean up the alert after processing
                    snapshot.getRef().removeValue();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                // Not needed for this implementation
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // Not needed for this implementation
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {
                // Not needed for this implementation
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void checkUserEligibilityAndShowNotification(NotificationModel notification) {
        if (notification == null) {
            Log.e("NotificationError", "Notification object is null!");
            return;
        }

        // Get current user
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Log.e("NotificationError", "Current user is null!");
            return; // No logged-in user
        }

        // Check target user type
        String targetUsers = notification.getTargetUsers();
        if (targetUsers == null) {
            Log.e("NotificationError", "Target users field is null!");
            return;
        }

        // For "all" target, show to everyone
        if (targetUsers.equals("all")) {
            showSystemNotification(notification);
            return;
        }

        // Get the current user's role from Firebase
        DatabaseReference userRef = mDatabase.child("users").child(currentUserId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String userRole = dataSnapshot.child("role").getValue(String.class);

                    boolean shouldShow = false;
                    if ("patient".equals(targetUsers) && "patient".equals(userRole)) {
                        shouldShow = true;
                    } else if ("doctor".equals(targetUsers) && "doctor".equals(userRole)) {
                        shouldShow = true;
                    } else if ("specific".equals(targetUsers)) {
                        List<String> specificUserIds = notification.getSpecificUserIds();
                        if (specificUserIds != null && specificUserIds.contains(currentUserId)) {
                            shouldShow = true;
                        }
                    }

                    if (shouldShow) {
                        showSystemNotification(notification);
                    }
                } else {
                    Log.e("NotificationError", "User role data not found for user: " + currentUserId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error fetching user role: " + databaseError.getMessage());
            }
        });
    }

    private void showSystemNotification(NotificationModel notification) {
        if (notification == null) {
            Log.e("NotificationError", "Notification object is null!");
            return;
        }

        // Validate notification ID
        // Ensure notification ID is not null or empty
        String notificationIdString = notification.getId();
        if (notificationIdString == null || notificationIdString.isEmpty()) {
            Log.e("NotificationError", "Notification ID is null or empty. Generating fallback ID.");
            notificationIdString = UUID.randomUUID().toString(); // Fallback UUID
        }

        // Generate a unique integer ID using hashCode()
        int notificationId = notificationIdString.hashCode();


        String notificationType = notification.getType();
        if (notificationType == null) {
            Log.e("NotificationError", "Notification type is null!");
            return;
        }

        // Create an intent to open the app when notification is clicked
        Intent intent = new Intent(this, splashpage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Using FLAG_IMMUTABLE for Android 12 and above
        int flags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = PendingIntent.FLAG_UPDATE_CURRENT;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationId, intent, flags);

        // Choose channel ID based on notification type
        String channelId = notificationType.equals("standard") ? CHANNEL_ID_STANDARD : CHANNEL_ID_BOT;

        // Set icon based on notification type
        int notificationIcon = notificationType.equals("standard") ? R.drawable.aarogyanidaanlogo : R.drawable.chatbot;

        // If custom icons aren't available, use app icon
        if (getResources().getIdentifier("ic_notification_standard", "drawable", getPackageName()) == 0) {
            notificationIcon = getApplicationInfo().icon;
        }

        // Set color based on notification type
        int color = notificationType.equals("standard") ? Color.RED : Color.BLUE;

        // Set priority based on notification priority
        int priority;
        if (notification.getPriority() != null) {
            switch (notification.getPriority().toLowerCase()) {
                case "high":
                    priority = NotificationCompat.PRIORITY_HIGH;
                    break;
                case "medium":
                    priority = NotificationCompat.PRIORITY_DEFAULT;
                    break;
                default:
                    priority = NotificationCompat.PRIORITY_LOW;
                    break;
            }
        } else {
            priority = NotificationCompat.PRIORITY_LOW; // Default priority
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(notificationIcon)
                .setContentTitle(notification.getTitle() != null ? notification.getTitle() : "New Notification")
                .setContentText(notification.getMessage() != null ? notification.getMessage() : "You have a new message.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notification.getMessage() != null ? notification.getMessage() : "You have a new message."))
                .setPriority(priority)
                .setColor(color)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 250, 250, 250}); // Add vibration pattern

        // Generate unique notification ID
        int finalNotificationId = notificationId;

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        try {
            // Check for notification permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(finalNotificationId, builder.build());
                } else {
                    Toast.makeText(this,
                            "Cannot show notification. Please grant notification permission in settings.",
                            Toast.LENGTH_LONG).show();
                    requestNotificationPermission();
                }
            } else {
                // For Android 12 and below
                notificationManager.notify(finalNotificationId, builder.build());
            }
        } catch (SecurityException e) {
            Log.e("NotificationError", "SecurityException: " + e.getMessage());
            Toast.makeText(this,
                    "Cannot show notification. Please grant notification permission in settings.",
                    Toast.LENGTH_LONG).show();
        }
    }


    private void validateAndSendNotification() {
        String title = editTextTitle.getText().toString().trim();
        String message = editTextMessage.getText().toString().trim();

        // Validate input fields
        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Title and message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        buttonSendNotification.setEnabled(false);
        textViewResult.setText("");

        // Get selected notification type
        String notificationType = radioStandard.isChecked() ? "standard" : "bot";

        // Get selected notification duration
        boolean isPermanent = radioPermanent.isChecked();

        // Get selected priority level
        String priority = spinnerPriority.getSelectedItem().toString();

        // Get target users
        String targetUsers;
        List<String> specificUserIds = new ArrayList<>();

        if (radioAllUsers.isChecked()) {
            targetUsers = "all";
        } else if (radioPatient.isChecked()) {
            targetUsers = "patient";
        } else if (radioDoctor.isChecked()) {
            targetUsers = "doctor";
        } else {
            targetUsers = "specific";
            String userIdsString = editTextUserIds.getText().toString().trim();
            if (userIdsString.isEmpty()) {
                progressBar.setVisibility(View.GONE);
                buttonSendNotification.setEnabled(true);
                Toast.makeText(this, "Please enter user IDs", Toast.LENGTH_SHORT).show();
                return;
            }

            // Clean and process the user IDs
            String[] idArray = userIdsString.split(",");
            for (String id : idArray) {
                String cleanId = id.trim();
                if (!cleanId.isEmpty()) {
                    specificUserIds.add(cleanId);
                }
            }

            if (specificUserIds.isEmpty()) {
                progressBar.setVisibility(View.GONE);
                buttonSendNotification.setEnabled(true);
                Toast.makeText(this, "Please enter valid user IDs", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Create notification model
        String notificationId = UUID.randomUUID().toString();
        NotificationModel notification = new NotificationModel(
                notificationId,
                title,
                message,
                notificationType,
                isPermanent,
                priority,
                targetUsers,
                specificUserIds,
                System.currentTimeMillis()
        );

        // Calculate expiration time if not permanent (1 hour from now)
        if (!isPermanent) {
            long expirationTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 hour
            notification.setExpirationTime(expirationTime);
        }

        // Send notification to Firebase
        sendNotification(notification);
    }

    private void sendNotification(NotificationModel notification) {
        // First store the notification in database, using ServerValue.TIMESTAMP for accurate server time
        Map<String, Object> notificationValues = new HashMap<>();
        notificationValues.put("title", notification.getTitle());
        notificationValues.put("message", notification.getMessage());
        notificationValues.put("type", notification.getType());
        notificationValues.put("permanent", notification.isPermanent());
        notificationValues.put("priority", notification.getPriority());
        notificationValues.put("targetUsers", notification.getTargetUsers());
        notificationValues.put("specificUserIds", notification.getSpecificUserIds());
        notificationValues.put("timestamp", ServerValue.TIMESTAMP);
        notificationValues.put("expirationTime", notification.getExpirationTime());

        mDatabase.child("notifications").child(notification.getId()).setValue(notificationValues)
                .addOnSuccessListener(aVoid -> {
                    // Now broadcast to users by creating an alert that their app can detect
                    broadcastNotificationToAllDevices(notification);

                    // Reset UI
                    clearInputFields();
                    progressBar.setVisibility(View.GONE);
                    buttonSendNotification.setEnabled(true);
                    textViewResult.setText("Notification sent successfully!");

                    // Test the notification locally immediately
                    showSystemNotification(notification);

                    // Show a success toast
                    Toast.makeText(AdminNotificationActivity.this,
                            "Notification sent successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    buttonSendNotification.setEnabled(true);
                    textViewResult.setText("Failed to send notification: " + e.getMessage());

                    // Show an error toast
                    Toast.makeText(AdminNotificationActivity.this,
                            "Failed to send notification: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void broadcastNotificationToAllDevices(NotificationModel notification) {
        // For each target type, we need a different approach
        String targetUsers = notification.getTargetUsers();

        if (targetUsers.equals("all")) {
            // Create a global notification alert
            createGlobalNotificationAlert(notification);
        } else if (targetUsers.equals("specific")) {
            // Send to each specific user
            for (String userId : notification.getSpecificUserIds()) {
                createUserSpecificNotificationAlert(notification, userId);
            }
        } else {
            // For "patient" or "doctor" target groups, create a group alert
            createGroupNotificationAlert(notification, targetUsers);
        }
    }

    private void createGlobalNotificationAlert(NotificationModel notification) {
        DatabaseReference alertsRef = mDatabase.child("notificationAlerts");
        String alertId = alertsRef.push().getKey();

        if (alertId != null) {
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("notificationId", notification.getId());
            alertData.put("targetType", "all");
            alertData.put("timestamp", ServerValue.TIMESTAMP);

            alertsRef.child(alertId).setValue(alertData);
        }
    }

    private void createUserSpecificNotificationAlert(NotificationModel notification, String userId) {
        DatabaseReference userAlertsRef = mDatabase.child("userNotifications").child(userId);
        String alertId = userAlertsRef.push().getKey();

        if (alertId != null) {
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("notificationId", notification.getId());
            alertData.put("timestamp", ServerValue.TIMESTAMP);
            alertData.put("read", false);

            userAlertsRef.child(alertId).setValue(alertData);
        }
    }

    private void createGroupNotificationAlert(NotificationModel notification, String groupType) {
        DatabaseReference alertsRef = mDatabase.child("notificationAlerts");
        String alertId = alertsRef.push().getKey();

        if (alertId != null) {
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("notificationId", notification.getId());
            alertData.put("targetType", groupType); // "patient" or "doctor"
            alertData.put("timestamp", ServerValue.TIMESTAMP);

            alertsRef.child(alertId).setValue(alertData);
        }
    }

    private void deleteNotification(String notificationId) {
        mDatabase.child("notifications").child(notificationId).removeValue()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(AdminNotificationActivity.this,
                                "Notification deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(AdminNotificationActivity.this,
                                "Failed to delete notification: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void clearInputFields() {
        editTextTitle.setText("");
        editTextMessage.setText("");
        editTextUserIds.setText("");
        radioStandard.setChecked(true);
        radioTemporary.setChecked(true);
        radioAllUsers.setChecked(true);
        spinnerPriority.setSelection(0);
        layoutUserIds.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure we have proper notification channels when resuming
        createNotificationChannels();

        // Check notification permissions on resume for newer Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Notify user about missing permission
                Toast.makeText(this, "Notification permission not granted. Some features may not work properly.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // Notification Model class
    public static class NotificationModel {
        private String id;
        private String title;
        private String message;
        private String type; // "standard" or "bot"
        private boolean permanent;
        private String priority;
        private String targetUsers; // "all", "patient", "doctor", "specific"
        private List<String> specificUserIds;
        private long timestamp;
        private long expirationTime;

        // Required empty constructor for Firebase
        public NotificationModel() {
        }

        public NotificationModel(String id, String title, String message, String type,
                                 boolean permanent, String priority, String targetUsers,
                                 List<String> specificUserIds, long timestamp) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.type = type;
            this.permanent = permanent;
            this.priority = priority;
            this.targetUsers = targetUsers;
            this.specificUserIds = specificUserIds != null ? specificUserIds : new ArrayList<>();
            this.timestamp = timestamp;
            this.expirationTime = permanent ? 0 : timestamp + (60 * 60 * 1000); // 1 hour if temporary
        }

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isPermanent() {
            return permanent;
        }

        public void setPermanent(boolean permanent) {
            this.permanent = permanent;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getTargetUsers() {
            return targetUsers;
        }

        public void setTargetUsers(String targetUsers) {
            this.targetUsers = targetUsers;
        }

        public List<String> getSpecificUserIds() {
            return specificUserIds;
        }

        public void setSpecificUserIds(List<String> specificUserIds) {
            this.specificUserIds = specificUserIds;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public long getExpirationTime() {
            return expirationTime;
        }

        public void setExpirationTime(long expirationTime) {
            this.expirationTime = expirationTime;
        }
    }

    // Adapter for RecyclerView
    static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

        private final List<NotificationModel> notificationList;
        private final NotificationDeleteListener deleteListener;

        public interface NotificationDeleteListener {
            void onDelete(String notificationId);
        }

        public NotificationAdapter(List<NotificationModel> notificationList, NotificationDeleteListener deleteListener) {
            this.notificationList = notificationList;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
            NotificationModel notification = notificationList.get(position);

            holder.textTitle.setText(notification.getTitle());
            holder.textMessage.setText(notification.getMessage());

            // Format type display with icon
            String typeDisplay = notification.getType().equals("standard") ?
                    "Standard" : "Bot";
            holder.textType.setText("Type: " + typeDisplay);

            // Format target display
            String targetDisplay;
            switch (notification.getTargetUsers()) {
                case "all":
                    targetDisplay = "All Users";
                    break;
                case "patient":
                    targetDisplay = "Patients Only";
                    break;
                case "doctor":
                    targetDisplay = "Doctors Only";
                    break;
                case "specific":
                    int count = notification.getSpecificUserIds() != null ?
                            notification.getSpecificUserIds().size() : 0;
                    targetDisplay = count + " Specific User" + (count != 1 ? "s" : "");
                    break;
                default:
                    targetDisplay = notification.getTargetUsers();
            }
            holder.textTarget.setText("Target: " + targetDisplay);

            // Format timestamp into readable date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(new Date(notification.getTimestamp()));
            holder.textTimestamp.setText("Sent: " + formattedDate);

            // Add expiration display for temporary notifications
            if (!notification.isPermanent() && notification.getExpirationTime() > 0) {
                long timeRemaining = notification.getExpirationTime() - System.currentTimeMillis();
                if (timeRemaining > 0) {
                    int minutesRemaining = (int) (timeRemaining / (60 * 1000));
                    holder.textTimestamp.append(" • Expires in " + minutesRemaining + " min");
                }
            }

            // Set priority indicator color based on priority
            String priority = notification.getPriority();
            if (priority.equalsIgnoreCase("High")) {
                holder.priorityIndicator.setBackgroundResource(R.drawable.priority_high);
            } else if (priority.equalsIgnoreCase("Medium")) {
                holder.priorityIndicator.setBackgroundResource(R.drawable.priority_medium);
            } else {
                holder.priorityIndicator.setBackgroundResource(R.drawable.priority_low);
            }

            // Set delete button click listener
            holder.buttonDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(notification.getId());
                }
            });
        }

        @Override
        public int getItemCount() {
            return notificationList.size();
        }

        static class NotificationViewHolder extends RecyclerView.ViewHolder {
            TextView textTitle, textMessage, textType, textTarget, textTimestamp;
            View priorityIndicator;
            ImageButton buttonDelete;

            public NotificationViewHolder(@NonNull View itemView) {
                super(itemView);
                textTitle = itemView.findViewById(R.id.textNotificationTitle);
                textMessage = itemView.findViewById(R.id.textNotificationMessage);
                textType = itemView.findViewById(R.id.textNotificationType);
                textTarget = itemView.findViewById(R.id.textNotificationTarget);
                textTimestamp = itemView.findViewById(R.id.textNotificationTimestamp);
                priorityIndicator = itemView.findViewById(R.id.priorityIndicator);
                buttonDelete = itemView.findViewById(R.id.buttonDeleteNotification);
            }
        }
    }
}