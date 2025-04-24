package com.healthcare.aarogyanidaan;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminNotificationActivity extends AppCompatActivity {
    private static final String TAG = "AdminNotification";

    // UI Components
    private ImageButton backButton;
    private TextInputEditText editTextTitle;
    private TextInputEditText editTextMessage;
    private TextInputEditText editTextUserIds;
    private TextInputLayout layoutUserIds;
    private RadioGroup radioGroupNotificationType;
    private RadioGroup radioGroupDuration;
    private RadioGroup radioGroupTargetUsers;
    private Spinner spinnerPriority;
    private MaterialButton buttonSendNotification;
    private ProgressBar progressBar;
    private TextView textViewResult;
    private RecyclerView sentNotificationsRecyclerView;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // Data
    private List<NotificationModel> notificationList;
    private AdminNotificationAdapter notificationAdapter;

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

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI components
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();

        // Load existing notifications
        loadNotifications();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backbutton);
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextMessage = findViewById(R.id.editTextMessage);
        editTextUserIds = findViewById(R.id.editTextUserIds);
        layoutUserIds = findViewById(R.id.layoutUserIds);
        radioGroupNotificationType = findViewById(R.id.radioGroupNotificationType);
        radioGroupDuration = findViewById(R.id.radioGroupDuration);
        radioGroupTargetUsers = findViewById(R.id.radioGroupTargetUsers);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        buttonSendNotification = findViewById(R.id.buttonSendNotification);
        progressBar = findViewById(R.id.progressBar);
        textViewResult = findViewById(R.id.textViewResult);
        sentNotificationsRecyclerView = findViewById(R.id.sentnotifications);
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
        notificationAdapter = new AdminNotificationAdapter(notificationList);
        sentNotificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sentNotificationsRecyclerView.setAdapter(notificationAdapter);
    }

    private void setupListeners() {
        // Show/hide user IDs input based on the target selection
        radioGroupTargetUsers.setOnCheckedChangeListener((group, checkedId) -> {
            layoutUserIds.setVisibility(checkedId == R.id.radioSpecificUsers ? View.VISIBLE : View.GONE);
        });

        // Send notification button click
        buttonSendNotification.setOnClickListener(v -> validateAndSendNotification());
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
                        notificationList.add(notification);
                    }
                }
                // Sort by timestamp (newest first)
                Collections.sort(notificationList, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                notificationAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminNotificationActivity.this, "Error loading notifications: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void validateAndSendNotification() {
        String title = editTextTitle.getText().toString().trim();
        String message = editTextMessage.getText().toString().trim();

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please enter both title and message", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonSendNotification.setEnabled(false);

        // Get notification type
        String notificationType = getSelectedNotificationType();

        // Get duration type
        boolean isPermanent = ((RadioButton)findViewById(R.id.radioPermanent)).isChecked();

        // Get priority
        String priority = spinnerPriority.getSelectedItem().toString();

        // Get target users
        String targetUsers = getSelectedTargetUsers();

        // Get specific user IDs if applicable
        List<String> specificUserIds = new ArrayList<>();
        if (targetUsers.equals("specific")) {
            String userIdsText = editTextUserIds.getText().toString().trim();
            if (!userIdsText.isEmpty()) {
                specificUserIds = Arrays.asList(userIdsText.split(","));
                // Trim whitespace from IDs
                for (int i = 0; i < specificUserIds.size(); i++) {
                    specificUserIds.set(i, specificUserIds.get(i).trim());
                }
            } else {
                progressBar.setVisibility(View.GONE);
                buttonSendNotification.setEnabled(true);
                Toast.makeText(this, "Please enter at least one user ID", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Create notification object
        NotificationModel notification = new NotificationModel();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(notificationType);
        notification.setPermanent(isPermanent);
        notification.setPriority(priority);
        notification.setTargetUsers(targetUsers);
        notification.setTimestamp(System.currentTimeMillis());

        // Set expiration time (7 days from now) if not permanent
        if (!isPermanent) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 7);
            notification.setExpirationTime(calendar.getTimeInMillis());
        }

        // If specific users, set the list
        if (targetUsers.equals("specific")) {
            notification.setSpecificUserIds(specificUserIds);
        }

        // Save to Firebase
        sendNotification(notification);
    }

    private String getSelectedNotificationType() {
        int selectedId = radioGroupNotificationType.getCheckedRadioButtonId();
        if (selectedId == R.id.radioBot) {
            return "bot";
        } else {
            return "standard";
        }
    }

    private String getSelectedTargetUsers() {
        int selectedId = radioGroupTargetUsers.getCheckedRadioButtonId();
        if (selectedId == R.id.radioAllUsers) {
            return "all";
        } else if (selectedId == R.id.radioPatient) {
            return "patient";
        } else if (selectedId == R.id.radioDoctor) {
            return "doctor";
        } else {
            return "specific";
        }
    }

    private void sendNotification(NotificationModel notification) {
        // Generate a new push ID
        String notificationId = mDatabase.child("notifications").push().getKey();

        if (notificationId != null) {
            // Save to notifications collection
            mDatabase.child("notifications").child(notificationId).setValue(notification)
                    .addOnSuccessListener(aVoid -> {
                        // Update notification alert node to trigger listeners
                        mDatabase.child("notificationAlerts").setValue(System.currentTimeMillis());

                        // Clear form
                        clearForm();

                        progressBar.setVisibility(View.GONE);
                        buttonSendNotification.setEnabled(true);
                        textViewResult.setText("Notification sent successfully!");
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        buttonSendNotification.setEnabled(true);
                        textViewResult.setText("Failed to send notification: " + e.getMessage());
                    });
        } else {
            progressBar.setVisibility(View.GONE);
            buttonSendNotification.setEnabled(true);
            textViewResult.setText("Failed to generate notification ID");
        }
    }

    private void clearForm() {
        editTextTitle.setText("");
        editTextMessage.setText("");
        editTextUserIds.setText("");
        radioGroupNotificationType.check(R.id.radioStandard);
        radioGroupDuration.check(R.id.radioTemporary);
        radioGroupTargetUsers.check(R.id.radioAllUsers);
        spinnerPriority.setSelection(0);
        layoutUserIds.setVisibility(View.GONE);
    }

    private void deleteNotification(String notificationId) {
        if (notificationId != null) {
            progressBar.setVisibility(View.VISIBLE);

            mDatabase.child("notifications").child(notificationId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AdminNotificationActivity.this, "Notification deleted", Toast.LENGTH_SHORT).show();

                        // Update notification alert node to trigger listeners
                        mDatabase.child("notificationAlerts").setValue(System.currentTimeMillis());
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AdminNotificationActivity.this, "Failed to delete notification", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Notification Model class
    public static class NotificationModel {
        private String id;
        private String title;
        private String message;
        private String type;
        private boolean permanent;
        private String priority;
        private String targetUsers;
        private List<String> specificUserIds;
        private long timestamp;
        private long expirationTime;

        // Required empty constructor for Firebase
        public NotificationModel() {
        }

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

    // Adapter for displaying admin notifications
    private class AdminNotificationAdapter extends RecyclerView.Adapter<AdminNotificationAdapter.NotificationViewHolder> {

        private final List<NotificationModel> notificationList;

        public AdminNotificationAdapter(List<NotificationModel> notificationList) {
            this.notificationList = notificationList;
        }

        @NonNull
        @Override
        public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_notification, parent, false);
            return new NotificationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
            NotificationModel notification = notificationList.get(position);

            holder.textTitle.setText(notification.getTitle());
            holder.textMessage.setText(notification.getMessage());

            // Format timestamp into readable date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(new Date(notification.getTimestamp()));
            holder.textTimestamp.setText(formattedDate);

            // Set target audience
            String targetUsers = notification.getTargetUsers();
            if ("all".equals(targetUsers)) {
                holder.textTarget.setText("All Users");
            } else if ("patient".equals(targetUsers)) {
                holder.textTarget.setText("Patients");
            } else if ("doctor".equals(targetUsers)) {
                holder.textTarget.setText("Doctors");
            } else {
                holder.textTarget.setText("Specific Users");
            }

            // Set priority
            holder.textPriority.setText(notification.getPriority());

            // Set notification type
            holder.textType.setText("standard".equals(notification.getType()) ? "Standard" : "Bot");

            // Set permanent status
            holder.textPermanent.setText(notification.isPermanent() ? "Permanent" : "Temporary");

            // Set delete button action
            holder.deleteButton.setOnClickListener(v -> {
                // Ask for confirmation before deleting
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(AdminNotificationActivity.this);
                builder.setTitle("Delete Notification")
                        .setMessage("Are you sure you want to delete this notification?")
                        .setPositiveButton("Delete", (dialog, which) -> deleteNotification(notification.getId()))
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return notificationList.size();
        }

        class NotificationViewHolder extends RecyclerView.ViewHolder {
            TextView textTitle, textMessage, textTimestamp, textTarget, textPriority, textType, textPermanent;
            ImageButton deleteButton;

            public NotificationViewHolder(@NonNull View itemView) {
                super(itemView);
                textTitle = itemView.findViewById(R.id.textNotificationTitle);
                textMessage = itemView.findViewById(R.id.textNotificationMessage);
                textTimestamp = itemView.findViewById(R.id.textNotificationTimestamp);
                textTarget = itemView.findViewById(R.id.textTarget);
                textPriority = itemView.findViewById(R.id.textPriority);
                textType = itemView.findViewById(R.id.textType);
                textPermanent = itemView.findViewById(R.id.textPermanent);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }
}