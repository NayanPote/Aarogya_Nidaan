package com.healthcare.aarogyanidaan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

public class ChatRequestAdapter extends RecyclerView.Adapter<ChatRequestAdapter.ViewHolder> {
    private List<ChatRequest> requests;
    private BiConsumer<ChatRequest, String> onActionClick;

    public ChatRequestAdapter(List<ChatRequest> requests,
                              BiConsumer<ChatRequest, String> onActionClick) {
        this.requests = requests;
        this.onActionClick = onActionClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatRequest request = requests.get(position);
        holder.patientNameText.setText(request.getPatientName());
        holder.patientIdText.setText("ID: " + request.getPatientId()); // Displaying patient ID
        holder.timestampText.setText(formatTimestamp(request.getTimestamp()));

        holder.acceptButton.setOnClickListener(v ->
                onActionClick.accept(request, "accepted"));
        holder.rejectButton.setOnClickListener(v ->
                onActionClick.accept(request, "rejected"));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("MMM dd, yyyy HH:mm",
                Locale.getDefault()).format(new Date(timestamp));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView patientNameText;
        TextView patientIdText;  // Added patient ID TextView
        TextView timestampText;
        Button acceptButton;
        Button rejectButton;

        ViewHolder(View view) {
            super(view);
            patientNameText = view.findViewById(R.id.patientNameText);
            patientIdText = view.findViewById(R.id.patientIdText); // Initialize patient ID TextView
            timestampText = view.findViewById(R.id.timestampText);
            acceptButton = view.findViewById(R.id.acceptButton);
            rejectButton = view.findViewById(R.id.rejectButton);
        }
    }
}
