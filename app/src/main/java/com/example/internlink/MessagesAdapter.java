package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DATE_HEADER = 0;
    private static final int TYPE_MESSAGE_SENT = 1;
    private static final int TYPE_MESSAGE_RECEIVED = 2;
    private static final int TYPE_SYSTEM_MESSAGE = 3;

    private List<Message> messages;
    private String currentUserId;

    public MessagesAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);

        if ("system".equals(message.getMessageType())) {
            return TYPE_SYSTEM_MESSAGE;
        }

        // Check if we need to show date header
        if (shouldShowDateHeader(position)) {
            return TYPE_DATE_HEADER;
        }

        if (message.isSentByCurrentUser(currentUserId)) {
            return TYPE_MESSAGE_SENT;
        } else {
            return TYPE_MESSAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case TYPE_DATE_HEADER:
                View dateView = inflater.inflate(R.layout.item_chat_date_header, parent, false);
                return new DateHeaderViewHolder(dateView);

            case TYPE_MESSAGE_SENT:
                View sentView = inflater.inflate(R.layout.item_message_sent, parent, false);
                return new SentMessageViewHolder(sentView);

            case TYPE_MESSAGE_RECEIVED:
                View receivedView = inflater.inflate(R.layout.item_message_received, parent, false);
                return new ReceivedMessageViewHolder(receivedView);

            case TYPE_SYSTEM_MESSAGE:
                View systemView = inflater.inflate(R.layout.item_system_message, parent, false);
                return new SystemMessageViewHolder(systemView);

            default:
                View defaultView = inflater.inflate(R.layout.item_message_received, parent, false);
                return new ReceivedMessageViewHolder(defaultView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder instanceof DateHeaderViewHolder) {
            ((DateHeaderViewHolder) holder).bind(message);
        } else if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        } else if (holder instanceof SystemMessageViewHolder) {
            ((SystemMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private boolean shouldShowDateHeader(int position) {
        if (position == 0) return true;

        Message currentMessage = messages.get(position);
        Message previousMessage = messages.get(position - 1);

        Calendar currentCal = Calendar.getInstance();
        currentCal.setTimeInMillis(currentMessage.getTimestamp());

        Calendar previousCal = Calendar.getInstance();
        previousCal.setTimeInMillis(previousMessage.getTimestamp());

        return currentCal.get(Calendar.DAY_OF_YEAR) != previousCal.get(Calendar.DAY_OF_YEAR) ||
                currentCal.get(Calendar.YEAR) != previousCal.get(Calendar.YEAR);
    }

    // Date Header ViewHolder
    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;

        DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
        }

        void bind(Message message) {
            tvDate.setText(message.getDateHeader());
        }
    }

    // Sent Message ViewHolder
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvStatus;
        LinearLayout messageContainer;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvStatus = itemView.findViewById(R.id.tv_status);
            messageContainer = itemView.findViewById(R.id.message_container);
        }

        void bind(Message message) {
            tvMessage.setText(message.getText());
            tvTime.setText(message.getFormattedTime());

            // Status indicators
            String status = message.getStatus();
            if ("sent".equals(status)) {
                tvStatus.setText("✓");
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
            } else if ("delivered".equals(status)) {
                tvStatus.setText("✓✓");
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
            } else if ("read".equals(status)) {
                tvStatus.setText("✓✓");
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.primary_color));
            }
        }
    }

    // Received Message ViewHolder
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        LinearLayout messageContainer;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            messageContainer = itemView.findViewById(R.id.message_container);
        }

        void bind(Message message) {
            tvMessage.setText(message.getText());
            tvTime.setText(message.getFormattedTime());
        }
    }

    // System Message ViewHolder
    static class SystemMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSystemMessage;

        SystemMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSystemMessage = itemView.findViewById(R.id.tv_system_message);
        }

        void bind(Message message) {
            tvSystemMessage.setText(message.getText());
        }
    }
}