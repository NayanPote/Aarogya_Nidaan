package com.healthcare.aarogyanidaan;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // View Types
    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_BOT = 1;
    private static final int VIEW_TYPE_USER_MEDIA = 2;
    private static final int VIEW_TYPE_BOT_MEDIA = 3;
    private static final int VIEW_TYPE_DATE = 4;

    private List<chatbot.ChatMessage> chatMessages = new ArrayList<>();
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public ChatAdapter(List<chatbot.ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    @Override
    public int getItemViewType(int position) {
        chatbot.ChatMessage message = chatMessages.get(position);
        if (message.isDateSeparator()) {
            return VIEW_TYPE_DATE;
        } else if (message.getMediaPath() != null) {
            return message.getSender().equals("user") ? VIEW_TYPE_USER_MEDIA : VIEW_TYPE_BOT_MEDIA;
        } else {
            return message.getSender().equals("user") ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VIEW_TYPE_USER:
                return new UserMessageViewHolder(inflater.inflate(R.layout.item_message_user, parent, false));
            case VIEW_TYPE_BOT:
                return new BotMessageViewHolder(inflater.inflate(R.layout.item_message_bot, parent, false));
            case VIEW_TYPE_USER_MEDIA:
                return new UserMediaViewHolder(inflater.inflate(R.layout.item_media_user, parent, false));
            case VIEW_TYPE_BOT_MEDIA:
                return new BotMediaViewHolder(inflater.inflate(R.layout.item_media_bot, parent, false));
            case VIEW_TYPE_DATE:
                return new DateViewHolder(inflater.inflate(R.layout.item_date_separator, parent, false));
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        chatbot.ChatMessage message = chatMessages.get(position);
        String time = timeFormat.format(new Date(message.getTimestamp()));

        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message, time);
        } else if (holder instanceof BotMessageViewHolder) {
            ((BotMessageViewHolder) holder).bind(message, time);
        } else if (holder instanceof UserMediaViewHolder) {
            ((UserMediaViewHolder) holder).bind(message, time);
        } else if (holder instanceof BotMediaViewHolder) {
            ((BotMediaViewHolder) holder).bind(message, time);
        } else if (holder instanceof DateViewHolder) {
            ((DateViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public void addMessage(chatbot.ChatMessage message) {
        chatMessages.add(message);
        notifyItemInserted(chatMessages.size() - 1);
    }

    public void clearMessages() {
        chatMessages.clear();
        notifyDataSetChanged();
    }

    // ✅ User Message ViewHolder
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timestampText;

        UserMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
        }

        void bind(chatbot.ChatMessage message, String time) {
            messageText.setText(message.getMessage());
            timestampText.setText(time);
        }
    }

    // ✅ Bot Message ViewHolder
    static class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timestampText;

        BotMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
        }

        void bind(chatbot.ChatMessage message, String time) {
            messageText.setText(message.getMessage());
            timestampText.setText(time);
        }
    }

    // ✅ User Media ViewHolder
    static class UserMediaViewHolder extends RecyclerView.ViewHolder {
        ImageView mediaView;
        TextView timestampText;

        UserMediaViewHolder(View itemView) {
            super(itemView);
            mediaView = itemView.findViewById(R.id.mediaView);
            timestampText = itemView.findViewById(R.id.timestampText);
        }

        void bind(chatbot.ChatMessage message, String time) {
            if (message.getMediaPath() != null) {
                Glide.with(mediaView.getContext())
                        .load(new File(message.getMediaPath()))
                        .into(mediaView);
            }
            timestampText.setText(time);
        }
    }

    // ✅ Bot Media ViewHolder
    static class BotMediaViewHolder extends RecyclerView.ViewHolder {
        ImageView mediaView;
        TextView timestampText;

        BotMediaViewHolder(View itemView) {
            super(itemView);
            mediaView = itemView.findViewById(R.id.mediaView);
            timestampText = itemView.findViewById(R.id.timestampText);
        }

        void bind(chatbot.ChatMessage message, String time) {
            if (message.getMediaPath() != null) {
                Glide.with(mediaView.getContext())
                        .load(new File(message.getMediaPath()))
                        .into(mediaView);
            }
            timestampText.setText(time);
        }
    }

    //Date ViewHolder
    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView dateSeparator;

        DateViewHolder(View itemView) {
            super(itemView);
            dateSeparator = itemView.findViewById(R.id.dateSeparator);
        }

        void bind(chatbot.ChatMessage message) {
            dateSeparator.setText(message.getMessage());
        }
    }
}
