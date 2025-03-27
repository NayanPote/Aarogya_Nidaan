package com.healthcare.aarogyanidaan;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private TextView name;
    private ImageButton sendButton, backButton, healthDataButton;
    private ProgressBar progressBar;
    private MessageAdapter messageAdapter;
    private List<Message> messages;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private LocalNotificationService notificationService;
    private String conversationId;
    private String otherPersonId;
    private ValueEventListener messagesListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize notification service
        notificationService = LocalNotificationService.getInstance(this);

        initializeViews();
        setupRecyclerView();
        setupConversation();
        setupSendButton();
        setupBackButton();

    }

    private void initializeViews() {
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);
        backButton = findViewById(R.id.backbutton);
        name = findViewById(R.id.name);
        healthDataButton = findViewById(R.id.healthDataButton);
        healthDataButton.setOnClickListener(v -> openHealthData());

        // Get the otherPersonId from intent
        otherPersonId = getIntent().getStringExtra("otherPersonId");
        conversationId = getIntent().getStringExtra("conversationId");

        // If we have otherPersonId, fetch their name
        if (otherPersonId != null) {
            fetchOtherPersonName();
        }

        if (conversationId == null) {
            createNewConversation();
        }
    }



    private void openHealthData() {
        Intent intent = new Intent(this, chathealthdata.class);
        intent.putExtra("patientId", otherPersonId);
        intent.putExtra("conversationId", conversationId);
        startActivity(intent);
    }

    private void fetchOtherPersonName() {
        mDatabase.child("conversations").child(conversationId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentUserId = mAuth.getCurrentUser().getUid();

                    // If current user is the doctor, show patient name, otherwise show doctor name
                    if (currentUserId.equals(snapshot.child("doctorId").getValue(String.class))) {
                        String patientName = snapshot.child("patientName").getValue(String.class);
                        if (patientName != null) {
                            name.setText(patientName);
                        }
                    } else {
                        String doctorName = snapshot.child("doctorName").getValue(String.class);
                        if (doctorName != null) {
                            name.setText(doctorName);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Error fetching user name", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setupBackButton() {
        backButton.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(messages, mAuth.getCurrentUser().getUid());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void createNewConversation() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        DatabaseReference conversationsRef = mDatabase.child("conversations");
        conversationId = conversationsRef.push().getKey();

        if (conversationId != null) {
            Map<String, Object> conversationData = new HashMap<>();
            conversationData.put("participants/" + currentUserId, true);
            conversationData.put("participants/" + otherPersonId, true);
            conversationData.put("createdAt", ServerValue.TIMESTAMP);

            conversationsRef.child(conversationId).setValue(conversationData)
                    .addOnSuccessListener(aVoid -> loadMessages())
                    .addOnFailureListener(e -> Toast.makeText(ChatActivity.this,
                            "Failed to create conversation: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show());
        }
    }

    private void setupConversation() {
        if (conversationId != null) {
            loadMessages();
        }
    }

    private void loadMessages() {
        progressBar.setVisibility(View.VISIBLE);
        DatabaseReference messagesRef = mDatabase.child("messages");
        Query messagesQuery = messagesRef.orderByChild("conversationId").equalTo(conversationId);

        messagesListener = messagesQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messages.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null) {
                        messages.add(message);
                    }
                }
                messageAdapter.notifyDataSetChanged();
                if (!messages.isEmpty()) {
                    messagesRecyclerView.scrollToPosition(messages.size() - 1);
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ChatActivity.this,
                        "Error loading messages: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSendButton() {
        sendButton.setOnClickListener(v -> {
            String content = messageInput.getText().toString().trim();
            if (!TextUtils.isEmpty(content)) {
                sendMessage(content);
            }
        });
    }

    private void sendMessage(String content) {
        if (conversationId == null) {
            createNewConversation(content);
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();
        DatabaseReference messagesRef = mDatabase.child("messages");
        String messageId = messagesRef.push().getKey();

        if (messageId != null) {
            String recipientId = currentUserId.equals(otherPersonId) ?
                    getIntent().getStringExtra("doctorId") : otherPersonId;

            Message message = new Message(
                    messageId,
                    currentUserId,
                    content,
                    System.currentTimeMillis(),
                    conversationId,
                    recipientId,
                    false  // not read initially
            );

            sendButton.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            messagesRef.child(messageId).setValue(message)
                    .addOnSuccessListener(aVoid -> {
                        messageInput.setText("");
                        updateConversationLastMessage(content, recipientId);

                        // No need for explicit notification call
                        // LocalNotificationService will handle notifications through its Firebase listeners

                        sendButton.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ChatActivity.this,
                                "Failed to send message: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        sendButton.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                    });
        }
    }

    private void createNewConversation(String initialMessage) {
        String currentUserId = mAuth.getCurrentUser().getUid();
        DatabaseReference conversationsRef = mDatabase.child("conversations");

        // Check if conversation already exists
        Query query = conversationsRef
                .orderByChild("participants/" + currentUserId)
                .equalTo(true);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean conversationExists = false;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (snapshot.child("participants/" + otherPersonId).exists() &&
                            snapshot.child("participants/" + otherPersonId).getValue(Boolean.class)) {
                        conversationId = snapshot.getKey();
                        conversationExists = true;
                        break;
                    }
                }

                if (!conversationExists) {
                    // Create new conversation
                    conversationId = conversationsRef.push().getKey();
                    if (conversationId != null) {
                        Map<String, Object> conversationData = new HashMap<>();
                        conversationData.put("participants/" + currentUserId, true);
                        conversationData.put("participants/" + otherPersonId, true);
                        conversationData.put("createdAt", ServerValue.TIMESTAMP);

                        conversationsRef.child(conversationId).setValue(conversationData)
                                .addOnSuccessListener(aVoid -> sendMessage(initialMessage))
                                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this,
                                        "Failed to create conversation: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                    }
                } else {
                    // Use existing conversation
                    sendMessage(initialMessage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this,
                        "Error checking existing conversations", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mark current conversation as active to prevent notifications
        if (conversationId != null) {
            notificationService.setActiveConversation(conversationId);
        }
        markMessagesAsRead();
    }

    private void updateConversationLastMessage(String lastMessage, String recipientId) {
        DatabaseReference conversationRef = mDatabase.child("conversations").child(conversationId);

        conversationRef.get().addOnSuccessListener(snapshot -> {
            // Get current unread count or default to 0
            Long existingUnreadCount = snapshot.child("unreadCount").getValue(Long.class);
            int currentUnreadCount = existingUnreadCount != null ? existingUnreadCount.intValue() : 0;

            // Get who currently has unread messages
            String currentUnreadUser = snapshot.child("unreadCountForUser").getValue(String.class);

            // If sending a message to the same person who already has unread messages, increment the count
            // Otherwise, reset the count to 1 for the new recipient
            if (recipientId.equals(currentUnreadUser)) {
                currentUnreadCount++;
            } else {
                currentUnreadCount = 1;
            }

            // Debug logging
            Log.d("ChatActivity", "Updating last message. UnreadCount=" + currentUnreadCount +
                    " ForUser=" + recipientId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("lastMessage", lastMessage);
            updates.put("lastMessageTimestamp", ServerValue.TIMESTAMP);
            updates.put("unreadCount", currentUnreadCount);
            updates.put("unreadCountForUser", recipientId);

            conversationRef.updateChildren(updates);
        });
    }

    private void markMessagesAsRead() {
        if (conversationId == null) return;

        String currentUserId = mAuth.getCurrentUser().getUid();
        DatabaseReference conversationRef = mDatabase.child("conversations").child(conversationId);

        // Check if this user has any unread messages
        conversationRef.get().addOnSuccessListener(snapshot -> {
            String unreadForUser = snapshot.child("unreadCountForUser").getValue(String.class);

            // Only reset unread count if current user is the one with unread messages
            if (currentUserId.equals(unreadForUser)) {
                Log.d("ChatActivity", "Marking messages as read for user: " + currentUserId);

                // Reset the unread count
                Map<String, Object> updates = new HashMap<>();
                updates.put("unreadCount", 0);
                updates.put("unreadCountForUser", "");
                conversationRef.updateChildren(updates);

                // Mark individual messages as read
                DatabaseReference messagesRef = mDatabase.child("messages");
                Query unreadMessagesQuery = messagesRef.orderByChild("conversationId")
                        .equalTo(conversationId);

                unreadMessagesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                            Message message = messageSnapshot.getValue(Message.class);

                            if (message != null &&
                                    !message.getSenderId().equals(currentUserId) &&
                                    !message.isRead()) {

                                Log.d("ChatActivity", "Marking message as read: " + message.getMessageId());
                                messageSnapshot.getRef().child("read").setValue(true);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("ChatActivity", "Error marking messages as read", error.toException());
                    }
                });
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        // Clear active conversation to allow notifications again
        notificationService.clearActiveConversation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            mDatabase.removeEventListener(messagesListener);
        }
    }
}