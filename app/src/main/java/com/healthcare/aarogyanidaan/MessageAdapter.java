package com.healthcare.aarogyanidaan;

import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.*;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private final List<Message> messages;
    private final String currentUserId;

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        if (messages == null || messages.get(position) == null) {
            return VIEW_TYPE_MESSAGE_RECEIVED; // Default to received message type
        }
        return messages.get(position).getSenderId().equals(currentUserId) ?
                VIEW_TYPE_MESSAGE_SENT : VIEW_TYPE_MESSAGE_RECEIVED;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = null;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = inflater.inflate(R.layout.item_message_sent, parent, false);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = inflater.inflate(R.layout.item_message_received, parent, false);
        }

        if (view == null) {
            Log.e("MessageAdapter", "View inflation failed!");
        }

        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        if (messages == null || messages.get(position) == null) {
            Log.e("MessageAdapter", "Message at position " + position + " is null!");
            return;
        }

        Message message = messages.get(position);

        // Apply bold styling to unread received messages
        boolean isReceived = !message.getSenderId().equals(currentUserId);
        boolean isUnread = !message.isRead();

        holder.bind(message, isReceived && isUnread);
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final TextView timeText;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
        }

        void bind(Message message, boolean shouldBeBold) {
            if (messageText != null) {
                messageText.setText(message.getContent() != null ? message.getContent() : ""); // Avoid null content

                // Apply bold styling to unread messages
                messageText.setTypeface(null, shouldBeBold ? Typeface.BOLD : Typeface.NORMAL);
            } else {
                Log.e("MessageAdapter", "messageText is null!");
            }

            if (timeText != null) {
                timeText.setText(formatTime(message.getTimestamp()));
            } else {
                Log.e("MessageAdapter", "timeText is null!");
            }
        }

        private String formatTime(long timestamp) {
            if (timestamp <= 0) return "";
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}