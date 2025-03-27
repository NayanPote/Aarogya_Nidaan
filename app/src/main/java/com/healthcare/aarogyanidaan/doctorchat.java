package com.healthcare.aarogyanidaan;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.healthcare.aarogyanidaan.databinding.ActivityDoctorchatBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class doctorchat extends AppCompatActivity {
    private ActivityDoctorchatBinding binding;
    private ChatRequestAdapter requestAdapter;
    private ConversationAdapter conversationAdapter;
    private List<ChatRequest> chatRequests;
    private List<Conversation> conversations;
    private List<Conversation> filteredConversations;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ValueEventListener requestsListener, conversationsListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDoctorchatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize lists
        chatRequests = new ArrayList<>();
        conversations = new ArrayList<>();
        filteredConversations = new ArrayList<>();

        setupConversationsRecyclerView();
        loadConversations();
        setupRequestsRecyclerView();
        loadChatRequests();
        setupSearch();

        // Floating Action Button click to show requests popup
        binding.fab.setOnClickListener(v -> showRequestsPopup());
        binding.closePopupButton.setOnClickListener(v -> hideRequestsPopup());
        binding.backbutton.setOnClickListener(v -> onBackPressed());
    }

    private void setupConversationsRecyclerView() {
        conversationAdapter = new ConversationAdapter(filteredConversations, conversation -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("conversationId", conversation.getConversationId());
            intent.putExtra("otherPersonName", conversation.getPatientName());
            intent.putExtra("otherPersonId", conversation.getPatientId());
            startActivity(intent);
        }, currentUserId);

        binding.conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.conversationsRecyclerView.setAdapter(conversationAdapter);
    }

    private void setupSearch() {
        binding.docsearchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
                if ((conversation.getPatientName() != null &&
                        conversation.getPatientName().toLowerCase().contains(lowercaseQuery)) ||
                        (conversation.getPatientId() != null &&
                                conversation.getPatientId().toLowerCase().contains(lowercaseQuery))) {

                    filteredConversations.add(conversation);
                }
            }
        }

        updateEmptyStateVisibility();
        conversationAdapter.notifyDataSetChanged();
    }

    private void updateEmptyStateVisibility() {
        if (filteredConversations.isEmpty()) {
            // Show empty state view
            binding.emptyStateView.setVisibility(View.VISIBLE);
        } else {
            // Hide empty state view
            binding.emptyStateView.setVisibility(View.GONE);
        }
    }

    private void loadConversations() {
        DatabaseReference conversationsRef = mDatabase.child("conversations");
        Query conversationsQuery = conversationsRef.orderByChild("doctorId").equalTo(currentUserId);

        conversationsListener = conversationsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                conversations.clear();
                for (DataSnapshot convSnapshot : snapshot.getChildren()) {
                    Conversation conversation = convSnapshot.getValue(Conversation.class);
                    if (conversation != null) {
                        conversations.add(conversation);
                    }
                }

                // Update filtered list and notify adapter
                filteredConversations.clear();
                filteredConversations.addAll(conversations);
                conversationAdapter.notifyDataSetChanged();
                updateEmptyStateVisibility();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(doctorchat.this,
                        "Error loading conversations: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRequestsRecyclerView() {
        requestAdapter = new ChatRequestAdapter(chatRequests, this::handleRequestAction);
        binding.requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.requestsRecyclerView.setAdapter(requestAdapter);
    }

    private void loadChatRequests() {
        DatabaseReference requestsRef = mDatabase.child("chat_requests");
        Query requestsQuery = requestsRef.orderByChild("doctorId").equalTo(currentUserId);

        requestsListener = requestsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatRequests.clear();
                int pendingCount = 0;
                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    ChatRequest request = requestSnapshot.getValue(ChatRequest.class);
                    if (request != null && "pending".equals(request.getStatus())) {
                        chatRequests.add(request);
                        pendingCount++;
                    }
                }
                updateBadge(pendingCount);
                requestAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(doctorchat.this,
                        "Error loading requests: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateBadge(int count) {
        binding.badgeTextView.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        binding.badgeTextView.setText(String.valueOf(count));
    }

    private void handleRequestAction(ChatRequest request, String action) {
        DatabaseReference requestRef = mDatabase.child("chat_requests").child(request.getRequestId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", action);

        requestRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, action.equals("accepted") ? "Request accepted" : "Request rejected", Toast.LENGTH_SHORT).show();
                    if (action.equals("accepted")) {
                        createConversation(request);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update request: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createConversation(ChatRequest request) {
        String conversationId = UUID.randomUUID().toString();

        mDatabase.child("doctor").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot doctorSnapshot) {
                Users doctor = doctorSnapshot.getValue(Users.class);
                if (doctor != null) {
                    mDatabase.child("patient").child(request.getPatientId()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot patientSnapshot) {
                            Users patient = patientSnapshot.getValue(Users.class);
                            if (patient != null) {
                                Conversation conversation = new Conversation(
                                        conversationId,
                                        currentUserId,
                                        request.getPatientId(),
                                        doctor.getDoctor_name(),
                                        doctor.getDoctor_specialization(),
                                        patient.getPatient_name(),
                                        System.currentTimeMillis()
                                );

                                mDatabase.child("conversations").child(conversationId)
                                        .setValue(conversation)
                                        .addOnSuccessListener(aVoid -> Toast.makeText(doctorchat.this, "Conversation created successfully", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(doctorchat.this, "Failed to create conversation: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(doctorchat.this, "Error getting patient details: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(doctorchat.this, "Error getting doctor details: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRequestsPopup() {
        binding.requestsPopup.setVisibility(View.VISIBLE);
        binding.requestsPopup.setAlpha(0f);
        binding.requestsPopup.animate().alpha(1f).setDuration(200).start();
    }

    private void hideRequestsPopup() {
        binding.requestsPopup.animate().alpha(0f).setDuration(200).withEndAction(() -> binding.requestsPopup.setVisibility(View.GONE)).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove database listeners to prevent memory leaks
        if (requestsListener != null) {
            DatabaseReference requestsRef = mDatabase.child("chat_requests");
            Query requestsQuery = requestsRef.orderByChild("doctorId").equalTo(currentUserId);
            requestsQuery.removeEventListener(requestsListener);
        }

        if (conversationsListener != null) {
            DatabaseReference conversationsRef = mDatabase.child("conversations");
            Query conversationsQuery = conversationsRef.orderByChild("doctorId").equalTo(currentUserId);
            conversationsQuery.removeEventListener(conversationsListener);
        }
    }
}