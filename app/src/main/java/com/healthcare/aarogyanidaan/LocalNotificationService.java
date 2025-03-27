package com.healthcare.aarogyanidaan;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LocalNotificationService {
    private static final String TAG = "LocalNotificationService";
    private static final String CHANNEL_ID = "chat_messages_channel";
    private static final String CHANNEL_NAME = "Chat Messages";
    private static final String KEY_TEXT_REPLY = "key_text_reply";

    private static final String PREFS_NAME = "ChatPrefs";
    private static final String ACTIVE_CONVERSATION_KEY = "activeConversationId";
    private static final String USER_LOGGED_IN_KEY = "userLoggedIn";

    private Context context;
    private String currentUserId;
    private DatabaseReference messagesRef;
    private ChildEventListener messagesListener;
    private static LocalNotificationService instance;
    private ScheduledExecutorService scheduler;
    private Handler mainHandler;
    private FirebaseAuth mAuth;
    private boolean isInitialized = false;

    // Map to store notification information by conversation ID
    private Map<String, NotificationInfo> activeNotifications = new HashMap<>();

    private class NotificationInfo {
        String senderName;
        int messageCount;
        String lastMessage;
        String senderId;

        NotificationInfo(String senderId, String senderName, String message) {
            this.senderId = senderId;
            this.senderName = senderName;
            this.lastMessage = message;
            this.messageCount = 1;
        }
    }

    private LocalNotificationService(Context context) {
        this.context = context.getApplicationContext();
        this.mAuth = FirebaseAuth.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        checkAuthStatusAndReInitialize();
    }

    public static synchronized LocalNotificationService getInstance(Context context) {
        if (instance == null) {
            instance = new LocalNotificationService(context);
        }
        return instance;
    }

    public void checkAuthStatusAndReInitialize() {
        // First, check if there was a logout
        if (mAuth.getCurrentUser() == null) {
            // Clear any saved login status as this is a logout condition
            saveUserLoggedInStatus(false);
            cleanup();
            isInitialized = false;
            Log.d(TAG, "Notification service stopped - user logged out");
            return;  // Return early to prevent further initialization
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean shouldBeLoggedIn = prefs.getBoolean(USER_LOGGED_IN_KEY, false);

        if (shouldBeLoggedIn && prefs.contains("currentUserId")) {
            // User should be logged in based on "Remember Me"
            this.currentUserId = prefs.getString("currentUserId", null);
            this.messagesRef = FirebaseDatabase.getInstance().getReference().child("messages");

            if (!isInitialized && this.currentUserId != null) {
                createNotificationChannel();
                startListeningForMessages();
                isInitialized = true;
                Log.d(TAG, "Notification service initialized for remembered user: " + currentUserId);
            }
        } else if (mAuth.getCurrentUser() != null) {
            // User is actively logged in
            if (!isInitialized) {
                this.currentUserId = mAuth.getCurrentUser().getUid();
                this.messagesRef = FirebaseDatabase.getInstance().getReference().child("messages");

                // Save logged in state to preferences
                saveUserLoggedInStatus(true);

                createNotificationChannel();
                startListeningForMessages();
                isInitialized = true;
                Log.d(TAG, "Notification service initialized for user: " + currentUserId);
            }
        }

        if (isInitialized) {
            restartNotificationServiceIfNeeded();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Channel for chat message notifications");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 250, 250});
            channel.setShowBadge(true);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void restartNotificationServiceIfNeeded() {
        // Only restart if logged in
        if (isUserLoggedIn()) {
            // Try to restart the service to ensure it stays alive
            Intent serviceIntent = new Intent(context, NotificationForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "Ensuring notification service is running");
        }
    }

    private void saveUserLoggedInStatus(boolean isLoggedIn) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(USER_LOGGED_IN_KEY, isLoggedIn).apply();

        if (isLoggedIn && mAuth.getCurrentUser() != null) {
            prefs.edit().putString("currentUserId", mAuth.getCurrentUser().getUid()).apply();
        }
    }

    public boolean isUserLoggedIn() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(USER_LOGGED_IN_KEY, false);
    }

    public void setActiveConversation(String conversationId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(ACTIVE_CONVERSATION_KEY, conversationId).apply();

        // Cancel notification for this conversation
        if (conversationId != null) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(conversationId.hashCode());
            activeNotifications.remove(conversationId);
        }
    }

    public void clearActiveConversation() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(ACTIVE_CONVERSATION_KEY).apply();
    }

    String getActiveConversationId() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(ACTIVE_CONVERSATION_KEY, null);
    }

    private void startListeningForMessages() {
        // Listen for new messages
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                Message message = snapshot.getValue(Message.class);
                if (message != null && !message.isNotified() &&
                        message.getRecipientId() != null &&
                        message.getRecipientId().equals(currentUserId)) {

                    // Mark the message as notified
                    snapshot.getRef().child("notified").setValue(true);

                    // Only show notification if user is not currently viewing this conversation
                    String activeConversation = getActiveConversationId();
                    if (activeConversation == null || !activeConversation.equals(message.getConversationId())) {
                        // Get sender info and show notification
                        getSenderInfo(message);
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error in message listener", error.toException());
            }
        };

        // Query for all unnotified messages that are intended for the current user
        Query query = messagesRef.orderByChild("recipientId").equalTo(currentUserId);
        query.addChildEventListener(messagesListener);

        //Check if scheduler is active before scheduling
        if (scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()) {
            scheduler = new ScheduledThreadPoolExecutor(1); // Create new instance if needed
        }

        scheduler.scheduleAtFixedRate(this::checkForMissedMessages, 1, 2, TimeUnit.MINUTES);
    }
    
    void checkForMissedMessages() {
        if (mAuth.getCurrentUser() == null) {
            Log.d(TAG, "Skipping missed message check - user not logged in");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Checking for missed messages for user: " + userId);

        Query query = messagesRef
                .orderByChild("recipientId")
                .equalTo(userId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null && !message.isNotified() && !message.isRead()) {
                        // Mark as notified
                        snapshot.getRef().child("notified").setValue(true);

                        // Show notification if not in active conversation
                        String activeConversation = getActiveConversationId();
                        if (activeConversation == null || !activeConversation.equals(message.getConversationId())) {
                            getSenderInfo(message);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking for missed messages", error.toException());
            }
        });
        restartNotificationServiceIfNeeded();
    }

    private void getSenderInfo(Message message) {
        String senderId = message.getSenderId();

        // Check cache first
        SharedPreferences prefs = context.getSharedPreferences("SenderNamesCache", Context.MODE_PRIVATE);
        String cachedName = prefs.getString(senderId, null);

        if (cachedName != null) {
            // Use cached name
            showNotification(message, cachedName);
            return;
        }

        // First check if this is from a doctor
        FirebaseDatabase.getInstance().getReference()
                .child("conversations")
                .child(message.getConversationId()) // Direct access using conversationId as key
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String senderName;
                            String doctorId = dataSnapshot.child("doctorId").getValue(String.class);
                            String patientId = dataSnapshot.child("patientId").getValue(String.class);

                            if (message.getSenderId().equals(doctorId)) {
                                senderName = dataSnapshot.child("doctorName").getValue(String.class);
                            } else if (message.getSenderId().equals(patientId)) {
                                senderName = dataSnapshot.child("patientName").getValue(String.class);
                            } else {
                                senderName = "Unknown";
                            }

                            Log.d(TAG, "Sender name found: " + senderName);
                            if (senderName != null) {
                                showNotification(message, senderName);
                            }
                        } else {
                            Log.d(TAG, "No data found for conversationId: " + message.getConversationId());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error fetching sender name", error.toException());
                    }
                });

    }

    private void showNotification(Message message, String senderName) {
        // Execute on main thread since notification needs to be shown from there
        mainHandler.post(() -> {
            String conversationId = message.getConversationId();
            String messageContent = message.getContent();

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            // Update or create notification info
            NotificationInfo info = activeNotifications.get(conversationId);
            if (info == null) {
                info = new NotificationInfo(message.getSenderId(), senderName, messageContent);
                activeNotifications.put(conversationId, info);
            } else {
                info.lastMessage = messageContent;
                info.messageCount++;
            }

            // Create an intent to open the chat activity
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("conversationId", conversationId);
            intent.putExtra("otherPersonId", message.getSenderId());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    conversationId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Set up direct reply
            RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                    .setLabel("Reply")
                    .build();

            // Create the reply action
            NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                    R.drawable.ic_send, "Reply", getReplyPendingIntent(conversationId, message.getSenderId()))
                    .addRemoteInput(remoteInput)
                    .build();

            // Create a Person object for the sender
            Person sender = new Person.Builder()
                    .setName(senderName)
                    .setImportant(true)
                    .build();

            // Create MessagingStyle notification
            NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(sender);
            if (info.messageCount > 1) {
                messagingStyle.setConversationTitle(senderName + " (" + info.messageCount + " messages)");
            } else {
                messagingStyle.setConversationTitle(senderName);
            }
            messagingStyle.addMessage(messageContent, System.currentTimeMillis(), sender);

            // Get default notification sound
            Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            // Build the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.aarogyanidaanlogo)
                    .setContentTitle(senderName)
                    .setContentText(messageContent)
                    .setStyle(messagingStyle)
                    .setAutoCancel(true)
                    .setColor(context.getResources().getColor(R.color.main_dark))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setContentIntent(pendingIntent)
                    .setSound(notificationSound)
                    .setVibrate(new long[]{0, 250, 250, 250}) // Vibration pattern
                    .addAction(replyAction);

            // Show notification
            notificationManager.notify(conversationId.hashCode(), builder.build());

            // Vibrate the device
            vibrate();
        });
    }

    private void vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vibratorManager != null) {
                    Vibrator vibrator = vibratorManager.getDefaultVibrator();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 250, 250, 250}, -1));
                    } else {
                        vibrator.vibrate(new long[]{0, 250, 250, 250}, -1);
                    }
                }
            } else {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 250, 250, 250}, -1));
                    } else {
                        vibrator.vibrate(new long[]{0, 250, 250, 250}, -1);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error vibrating device", e);
        }
    }

    private PendingIntent getReplyPendingIntent(String conversationId, String recipientId) {
        Intent intent = new Intent(context, DirectReplyReceiver.class);
        intent.putExtra("conversationId", conversationId);
        intent.putExtra("recipientId", recipientId);

        return PendingIntent.getBroadcast(
                context,
                conversationId.hashCode() + 1,  // Different request code from the notification
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
    }



    public void cleanup() {
        if (messagesListener != null && messagesRef != null) {
            Query query = messagesRef.orderByChild("recipientId").equalTo(currentUserId);
            query.removeEventListener(messagesListener);
            messagesListener = null;
        }

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            scheduler = null;  // Add this line
        }

        // Clear user data
        this.currentUserId = null;  // Add this line

        // Clear all active notifications
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        for (String conversationId : activeNotifications.keySet()) {
            notificationManager.cancel(conversationId.hashCode());
        }
        activeNotifications.clear();
    }
}