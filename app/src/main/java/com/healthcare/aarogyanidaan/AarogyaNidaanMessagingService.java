package com.healthcare.aarogyanidaan;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class AarogyaNidaanMessagingService extends FirebaseMessagingService {
    private static final String TAG = "AarogyaNidaanMessage";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "FCM message received: " + remoteMessage.getData());

        // First check if user is actually logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d(TAG, "Ignoring FCM message - user not logged in");
            return;
        }

        // Get data from the message
        String conversationId = remoteMessage.getData().get("conversationId");
        String senderId = remoteMessage.getData().get("senderId");
        String content = remoteMessage.getData().get("content");

        // Process the message if we have valid data
        if (conversationId != null && senderId != null) {
            ensureNotificationServiceRunning();

            // Check if the app is in the foreground by checking if there's an active conversation
            LocalNotificationService notificationService = LocalNotificationService.getInstance(this);
            String activeConversation = notificationService.getActiveConversationId();

            // If we're not in the specific conversation, show a notification
            if (activeConversation == null || !activeConversation.equals(conversationId)) {
                // Force check for missed messages
                notificationService.checkForMissedMessages();
            }
        }
    }

    private void ensureNotificationServiceRunning() {
        // Check if the user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // Start the foreground service if not already running
            Intent serviceIntent = new Intent(this, NotificationForegroundService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);

        // Save the new token to Firebase for this user
        saveTokenToFirebase(token);
    }

    private void saveTokenToFirebase(String token) {
        // Only save if the user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseDatabase.getInstance().getReference("users")
                    .child(userId)
                    .child("fcmToken")
                    .setValue(token)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "FCM token saved to Firebase");
                        } else {
                            Log.e(TAG, "Failed to save FCM token", task.getException());
                        }
                    });
        }
    }
}
