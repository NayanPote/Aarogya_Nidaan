package com.healthcare.aarogyanidaan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
    private final List<Conversation> conversations;
    private final Consumer<Conversation> onConversationClick;
    private final String currentUserId;

    public ConversationAdapter(List<Conversation> conversations,
                               Consumer<Conversation> onConversationClick,
                               String currentUserId) {
        this.conversations = conversations;
        this.onConversationClick = onConversationClick;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);

        // Set patient details
        holder.nameText.setText(conversation.getPatientName());
        holder.idText.setText("ID: " + conversation.getPatientId());

        // Check if there are messages
        boolean hasMessages = conversation.getLastMessage() != null &&
                !conversation.getLastMessage().isEmpty();

        // Check if current user should see unread count - updated to match DoctorConversationAdapter
        boolean shouldShowUnread = hasMessages && conversation.getUnreadCount() > 0 &&
                currentUserId.equals(conversation.getUnreadCountForUser());

        if (hasMessages) {
            holder.lastMessageText.setText(conversation.getLastMessage());

            // Update text styling based on unread status
            holder.lastMessageText.setTypeface(null,
                    shouldShowUnread ? android.graphics.Typeface.BOLD :
                            android.graphics.Typeface.NORMAL);

            holder.lastMessageText.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(shouldShowUnread ?
                            android.R.color.black : android.R.color.darker_gray));

            // Show/hide unread badge
            if (shouldShowUnread) {
                holder.patunreadBadge.setVisibility(View.VISIBLE);
                holder.patunreadBadge.setText(String.valueOf(conversation.getUnreadCount()));
                holder.nameText.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                holder.patunreadBadge.setVisibility(View.GONE);
                holder.nameText.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            // Show timestamp if available
            if (conversation.getLastMessageTimestamp() > 0) {
                holder.timestampText.setText(formatTimestamp(conversation.getLastMessageTimestamp()));
                holder.timestampText.setVisibility(View.VISIBLE);
            } else {
                holder.timestampText.setVisibility(View.GONE);
            }
        } else {
            // No messages case
            holder.lastMessageText.setText("No messages yet");
            holder.lastMessageText.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.lastMessageText.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.darker_gray));
            holder.patunreadBadge.setVisibility(View.GONE);
            holder.nameText.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.timestampText.setVisibility(View.GONE);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> onConversationClick.accept(conversation));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView idText;
        TextView lastMessageText;
        TextView timestampText;
        ImageView avatar;
        TextView patunreadBadge;

        ViewHolder(View view) {
            super(view);
            nameText = view.findViewById(R.id.nameText);
            idText = view.findViewById(R.id.idText);
            lastMessageText = view.findViewById(R.id.lastMessageText);
            timestampText = view.findViewById(R.id.timestampText);
            avatar = view.findViewById(R.id.avatar);
            patunreadBadge = view.findViewById(R.id.patunreadBadge);
        }
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                .format(new java.util.Date(timestamp));
    }

}