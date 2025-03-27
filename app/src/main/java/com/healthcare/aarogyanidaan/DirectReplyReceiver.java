package com.healthcare.aarogyanidaan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class DirectReplyReceiver extends BroadcastReceiver {
    private static final String TAG = "DirectReplyReceiver";
    private static final String KEY_TEXT_REPLY = "key_text_reply";

    @Override
    public void onReceive(Context context, Intent intent) {
        String conversationId = intent.getStringExtra("conversationId");
        String recipientId = intent.getStringExtra("recipientId");

        if (conversationId == null || recipientId == null) {
            Log.e(TAG, "Missing conversation or recipient ID");
            return;
        }

        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);

        if (remoteInput != null) {
            CharSequence replyText = remoteInput.getCharSequence(KEY_TEXT_REPLY);
            if (replyText != null && replyText.length() > 0) {
                String reply = replyText.toString();

                // Send the reply message
                sendMessage(context, conversationId, recipientId, reply);

                // Cancel the notification
                NotificationManagerCompat.from(context).cancel(conversationId.hashCode());

                // Open the ChatActivity after replying
                Intent chatIntent = new Intent(context, ChatActivity.class);
                chatIntent.putExtra("conversationId", conversationId);
                chatIntent.putExtra("otherPersonId", recipientId);
                chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(chatIntent);
            }
        }
    }

    private void sendMessage(Context context, String conversationId, String recipientId, String content) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "Cannot send message: user not logged in");
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference().child("messages");
        String messageId = messagesRef.push().getKey();

        if (messageId != null) {
            Message message = new Message(
                    messageId,
                    currentUserId,
                    content,
                    System.currentTimeMillis(),
                    conversationId,
                    recipientId,
                    false,  // not read initially
                    false   // not notified initially
            );

            messagesRef.child(messageId).setValue(message)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Reply message sent successfully");
                        // Update conversation's last message
                        updateConversationLastMessage(conversationId, content, recipientId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send reply message", e);
                    });
        }
    }

    private void updateConversationLastMessage(String conversationId, String lastMessage, String recipientId) {
        DatabaseReference conversationRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("conversations")
                .child(conversationId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", lastMessage);
        updates.put("lastMessageTimestamp", ServerValue.TIMESTAMP);
        updates.put("lastMessageSenderId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        updates.put("unreadCount", 1);
        updates.put("unreadCountForUser", recipientId);

        conversationRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Conversation updated with reply");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update conversation", e);
                });
    }
}