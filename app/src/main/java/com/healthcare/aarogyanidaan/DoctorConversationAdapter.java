package com.healthcare.aarogyanidaan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class DoctorConversationAdapter extends RecyclerView.Adapter<DoctorConversationAdapter.ViewHolder> {
    private final List<Conversation> conversations;
    private final Consumer<Conversation> onConversationClick;
    private final String currentUserId;

    public DoctorConversationAdapter(List<Conversation> conversations,
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
                .inflate(R.layout.item_doctor_convertation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);

        // Set doctor details
        holder.nameText.setText(conversation.getDoctorName());
        holder.specializationText.setText(conversation.getDoctorSpecialization());
        holder.idText.setText("ID: " + conversation.getDoctorId());

        // Check if there are messages
        boolean hasMessages = conversation.getLastMessage() != null &&
                !conversation.getLastMessage().isEmpty();

        // Check if current user should see unread count - matching ConversationAdapter logic
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
                holder.unreadBadge.setVisibility(View.VISIBLE);
                holder.unreadBadge.setText(String.valueOf(conversation.getUnreadCount()));
                holder.nameText.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                holder.unreadBadge.setVisibility(View.GONE);
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
            holder.unreadBadge.setVisibility(View.GONE);
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
        TextView specializationText;
        TextView idText;
        TextView lastMessageText;
        TextView timestampText;
        ImageView avatar;
        TextView unreadBadge;

        ViewHolder(View view) {
            super(view);
            nameText = view.findViewById(R.id.nameText);
            specializationText = view.findViewById(R.id.specializationText);
            idText = view.findViewById(R.id.idText);
            lastMessageText = view.findViewById(R.id.lastMessageText);
            timestampText = view.findViewById(R.id.timestampText);
            avatar = view.findViewById(R.id.doctorAvatar);
            unreadBadge = view.findViewById(R.id.docunreadBadge);
        }
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                .format(new java.util.Date(timestamp));
    }

}