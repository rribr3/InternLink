package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private List<ChatItem> chatItems;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(ChatItem chatItem);
    }

    public ChatListAdapter(List<ChatItem> chatItems, OnChatClickListener listener) {
        this.chatItems = chatItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem chatItem = chatItems.get(position);
        holder.bind(chatItem);
    }

    @Override
    public int getItemCount() {
        return chatItems.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvLastMessage, tvTime, tvUnreadCount;
        private ImageView ivProfile, ivMessageStatus;
        private CardView cvUnreadBadge;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
            ivProfile = itemView.findViewById(R.id.iv_profile);
            ivMessageStatus = itemView.findViewById(R.id.iv_message_status);
            cvUnreadBadge = itemView.findViewById(R.id.cv_unread_badge);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onChatClick(chatItems.get(position));
                }
            });
        }

        void bind(ChatItem chatItem) {
            tvName.setText(chatItem.getOtherUserName());
            tvTime.setText(chatItem.getFormattedTime());

            // Set last message with status
            if (chatItem.isLastMessageFromMe()) {
                tvLastMessage.setText("You: " + chatItem.getLastMessage());
                ivMessageStatus.setVisibility(View.VISIBLE);
            } else {
                tvLastMessage.setText(chatItem.getLastMessage());
                ivMessageStatus.setVisibility(View.GONE);
            }

            // Set unread count
            if (chatItem.getUnreadCount() > 0) {
                cvUnreadBadge.setVisibility(View.VISIBLE);
                tvUnreadCount.setText(String.valueOf(chatItem.getUnreadCount()));
            } else {
                cvUnreadBadge.setVisibility(View.GONE);
            }

            // Set profile icon based on user type
            if ("company".equals(chatItem.getOtherUserType())) {
                ivProfile.setImageResource(R.drawable.ic_business);
            } else {
                ivProfile.setImageResource(R.drawable.ic_person);
            }
        }
    }
}