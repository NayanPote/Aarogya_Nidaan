package com.healthcare.aarogyanidaan;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.healthcare.aarogyanidaan.databinding.ActivityPatientchatBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class patientchat extends AppCompatActivity {
    private ActivityPatientchatBinding binding;
    private DoctorConversationAdapter doctorConversationAdapter;
    private List<Conversation> conversations;
    private List<Conversation> filteredConversations;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ValueEventListener conversationsListener;
    private String currentUserId;
    private int totalConversationsToProcess = 0;
    private int processedConversations = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPatientchatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize lists
        conversations = new ArrayList<>();
        filteredConversations = new ArrayList<>();

        setupViews();
        setupRecyclerView();
        setupSearch();
        loadConversations();
    }

    private void setupViews() {
        binding.backbutton.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });
    }

    private void setupRecyclerView() {
        binding.conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        doctorConversationAdapter = new DoctorConversationAdapter(
                filteredConversations,
                conversation -> {
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("conversationId", conversation.getConversationId());
                    intent.putExtra("otherPersonName", conversation.getDoctorName());
                    intent.putExtra("otherPersonId", conversation.getDoctorId());
                    intent.putExtra("doctorSpecialization", conversation.getDoctorSpecialization());
                    startActivity(intent);
                },
                currentUserId
        );

        binding.conversationsRecyclerView.setAdapter(doctorConversationAdapter);
    }

    private void setupSearch() {
        binding.patsearchBar.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterConversations(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterConversations(newText);
                return true;
            }
        });
    }

    private void filterConversations(String query) {
        filteredConversations.clear();

        if (TextUtils.isEmpty(query)) {
            filteredConversations.addAll(conversations);
        } else {
            String lowercaseQuery = query.toLowerCase();
            for (Conversation conversation : conversations) {
                if (conversation.getDoctorName().toLowerCase().contains(lowercaseQuery) ||
                        conversation.getDoctorSpecialization().toLowerCase().contains(lowercaseQuery) ||
                            conversation.getDoctorId().toLowerCase().contains(lowercaseQuery)) {
                    filteredConversations.add(conversation);
                }
            }
        }

        updateEmptyStateVisibility();
        doctorConversationAdapter.notifyDataSetChanged();
    }

    private void loadConversations() {
        showLoading(true);

        DatabaseReference conversationsRef = mDatabase.child("conversations");
        Query conversationsQuery = conversationsRef.orderByChild("patientId").equalTo(currentUserId);

        conversationsListener = conversationsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                conversations.clear();
                totalConversationsToProcess = (int) snapshot.getChildrenCount();
                processedConversations = 0;

                if (totalConversationsToProcess == 0) {
                    showLoading(false);
                    updateEmptyStateVisibility();
                    return;
                }

                for (DataSnapshot convSnapshot : snapshot.getChildren()) {
                    Conversation conversation = convSnapshot.getValue(Conversation.class);
                    if (conversation != null) {
                        countUnreadMessages(conversation);
                    } else {
                        processedConversations++;
                        checkIfAllProcessed();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(patientchat.this,
                        "Error loading conversations: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void countUnreadMessages(Conversation conversation) {
        DatabaseReference messagesRef = mDatabase.child("messages");
        Query unreadMessagesQuery = messagesRef.orderByChild("conversationId")
                .equalTo(conversation.getConversationId());

        unreadMessagesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int unreadCount = 0;

                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null &&
                            !message.getSenderId().equals(currentUserId) &&
                            !message.isRead()) {
                        unreadCount++;
                    }
                }

                conversation.setUnreadCount(unreadCount);
                conversations.add(conversation);

                processedConversations++;
                checkIfAllProcessed();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                processedConversations++;
                Log.e("patientchat", "Failed to count unread messages", databaseError.toException());
                checkIfAllProcessed();
            }
        });
    }

    private void checkIfAllProcessed() {
        if (processedConversations >= totalConversationsToProcess) {
            sortAndUpdateUI();
        }
    }

    private void sortAndUpdateUI() {
        showLoading(false);

        Collections.sort(conversations, (c1, c2) ->
                Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));

        filteredConversations.clear();
        filteredConversations.addAll(conversations);

        updateEmptyStateVisibility();
        doctorConversationAdapter.notifyDataSetChanged();
    }

    private void updateEmptyStateVisibility() {
        if (filteredConversations.isEmpty()) {
            binding.emptyStateView.setVisibility(View.VISIBLE);
            binding.conversationsRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyStateView.setVisibility(View.GONE);
            binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.conversationsRecyclerView.setVisibility(View.GONE);
            binding.emptyStateView.setVisibility(View.GONE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conversationsListener != null) {
            mDatabase.child("conversations").removeEventListener(conversationsListener);
        }
    }
}