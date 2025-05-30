package com.example.internlink;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ConversationViewHolder> {

    private List<Conversation> conversations;
    private OnConversationClickListener listener;
    private Context context;
    private String currentUserId;

    public void updateList(List<Conversation> newList) {
        this.conversations = newList;
        notifyDataSetChanged();
    }

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public ConversationsAdapter(List<Conversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        holder.bind(conversation);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivCompanyLogo;
        private TextView tvCompanyName;
        private TextView tvProjectTitle;
        private TextView tvLastMessage;
        private TextView tvLastMessageTime;
        private TextView tvUnreadBadge;
        private View unreadIndicator;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);

            ivCompanyLogo = itemView.findViewById(R.id.iv_company_logo);
            tvCompanyName = itemView.findViewById(R.id.tv_company_name);
            tvProjectTitle = itemView.findViewById(R.id.tv_project_title);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvLastMessageTime = itemView.findViewById(R.id.tv_last_message_time);
            tvUnreadBadge = itemView.findViewById(R.id.tv_unread_badge);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onConversationClick(conversations.get(position));
                }
            });
        }

        public void bind(Conversation conversation) {
            // Set company name
            tvCompanyName.setText(conversation.getOtherUserName() != null ?
                    conversation.getOtherUserName() : "Company");

            // Set project title
            if (conversation.getProjectTitle() != null && !conversation.getProjectTitle().trim().isEmpty()) {
                tvProjectTitle.setVisibility(View.VISIBLE);
                tvProjectTitle.setText("📱 " + conversation.getProjectTitle());
            } else {
                tvProjectTitle.setVisibility(View.GONE);
            }

            // Set last message
            String displayMessage = conversation.getDisplayMessage();

            // Add "You: " prefix if the current user sent the last message
            if (conversation.isFromCurrentUser(currentUserId)) {
                displayMessage = "You: " + displayMessage;
            }

            tvLastMessage.setText(displayMessage);

            // Set timestamp
            if (conversation.getLastMessageTime() > 0) {
                String timeText = formatTimestamp(conversation.getLastMessageTime());
                tvLastMessageTime.setText(timeText);
                tvLastMessageTime.setVisibility(View.VISIBLE);
            } else {
                tvLastMessageTime.setVisibility(View.GONE);
            }

            // Set company logo
            if (conversation.getOtherUserLogoUrl() != null && !conversation.getOtherUserLogoUrl().trim().isEmpty()) {
                Glide.with(context)
                        .load(conversation.getOtherUserLogoUrl())
                        .placeholder(R.drawable.ic_company_placeholder)
                        .error(R.drawable.ic_company_placeholder)
                        .circleCrop()
                        .into(ivCompanyLogo);
            } else {
                // Set default company logo
                ivCompanyLogo.setImageResource(R.drawable.ic_company_placeholder);
            }

            // Handle unread count (you can implement this based on your notification system)
            if (conversation.getUnreadCount() > 0) {
                tvUnreadBadge.setVisibility(View.VISIBLE);
                unreadIndicator.setVisibility(View.VISIBLE);
                tvUnreadBadge.setText(String.valueOf(conversation.getUnreadCount()));
            } else {
                tvUnreadBadge.setVisibility(View.GONE);
                unreadIndicator.setVisibility(View.GONE);
            }
        }

        private String formatTimestamp(long timestamp) {
            Date messageDate = new Date(timestamp);
            Date now = new Date();

            // Calculate time difference
            long diff = now.getTime() - timestamp;

            if (diff < 60000) { // Less than 1 minute
                return "now";
            } else if (diff < 3600000) { // Less than 1 hour
                int minutes = (int) (diff / 60000);
                return minutes + "m";
            } else if (diff < 86400000) { // Less than 1 day
                int hours = (int) (diff / 3600000);
                return hours + "h";
            } else if (diff < 604800000) { // Less than 1 week
                int days = (int) (diff / 86400000);
                return days + "d";
            } else {
                // More than a week, show date
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
                return sdf.format(messageDate);
            }
        }

    }
}