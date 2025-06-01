// MessagesAdapter.java - Updated with long press delete functionality
package com.example.internlink;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DATE_HEADER = 0;
    private static final int TYPE_MESSAGE_SENT = 1;
    private static final int TYPE_MESSAGE_RECEIVED = 2;
    private static final int TYPE_SYSTEM_MESSAGE = 3;
    private static final int TYPE_FILE_SENT = 4;
    private static final int TYPE_FILE_RECEIVED = 5;
    private static final int TYPE_IMAGE_SENT = 6;
    private static final int TYPE_IMAGE_RECEIVED = 7;

    private List<Message> messages;
    private String currentUserId;
    private OnMessageLongClickListener longClickListener;

    // Interface for handling long click events
    public interface OnMessageLongClickListener {
        void onMessageLongClick(Message message, int position);
    }

    public MessagesAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);

        // Check if we need to show date header
        if (position > 0 && shouldShowDateHeader(position)) {
            // This position should show a date header
            // You might need to insert date headers as separate items in your list
        }

        if ("system".equals(message.getMessageType())) {
            return TYPE_SYSTEM_MESSAGE;
        }

        boolean isSent = message.isSentByCurrentUser(currentUserId);

        switch (message.getMessageType()) {
            case "image":
                return isSent ? TYPE_IMAGE_SENT : TYPE_IMAGE_RECEIVED;
            case "file":
                return isSent ? TYPE_FILE_SENT : TYPE_FILE_RECEIVED;
            default:
                return isSent ? TYPE_MESSAGE_SENT : TYPE_MESSAGE_RECEIVED;
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

            case TYPE_FILE_SENT:
                View fileSentView = inflater.inflate(R.layout.item_message_file_sent, parent, false);
                return new FileSentViewHolder(fileSentView);

            case TYPE_FILE_RECEIVED:
                View fileReceivedView = inflater.inflate(R.layout.item_message_file_received, parent, false);
                return new FileReceivedViewHolder(fileReceivedView);

            case TYPE_IMAGE_SENT:
                View imageSentView = inflater.inflate(R.layout.item_message_image_sent, parent, false);
                return new ImageSentViewHolder(imageSentView);

            case TYPE_IMAGE_RECEIVED:
                View imageReceivedView = inflater.inflate(R.layout.item_message_image_received, parent, false);
                return new ImageReceivedViewHolder(imageReceivedView);

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
            ((SentMessageViewHolder) holder).bind(message, position);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message, position);
        } else if (holder instanceof SystemMessageViewHolder) {
            ((SystemMessageViewHolder) holder).bind(message);
        } else if (holder instanceof FileSentViewHolder) {
            ((FileSentViewHolder) holder).bind(message, position);
        } else if (holder instanceof FileReceivedViewHolder) {
            ((FileReceivedViewHolder) holder).bind(message, position);
        } else if (holder instanceof ImageSentViewHolder) {
            ((ImageSentViewHolder) holder).bind(message, position);
        } else if (holder instanceof ImageReceivedViewHolder) {
            ((ImageReceivedViewHolder) holder).bind(message, position);
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

    // Helper method to set up long click listener for message containers
    private void setupLongClickListener(View container, Message message, int position) {
        container.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onMessageLongClick(message, position);
            }
            return true; // Consume the long click event
        });
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
    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvStatus;
        LinearLayout messageContainer;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvStatus = itemView.findViewById(R.id.tv_status);
            messageContainer = itemView.findViewById(R.id.message_container);
        }

        void bind(Message message, int position) {
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

            // Set up long click listener
            setupLongClickListener(messageContainer, message, position);
        }
    }

    // Received Message ViewHolder
    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        LinearLayout messageContainer;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            messageContainer = itemView.findViewById(R.id.message_container);
        }

        void bind(Message message, int position) {
            tvMessage.setText(message.getText());
            tvTime.setText(message.getFormattedTime());

            // Set up long click listener
            setupLongClickListener(messageContainer, message, position);
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
            // System messages typically shouldn't be deletable
        }
    }

    // File Sent ViewHolder
    class FileSentViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvFileSize, tvTime, tvStatus;
        ImageView ivFileIcon;
        LinearLayout messageContainer;

        FileSentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileSize = itemView.findViewById(R.id.tv_file_size);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvStatus = itemView.findViewById(R.id.tv_status);
            ivFileIcon = itemView.findViewById(R.id.iv_file_icon);
            messageContainer = itemView.findViewById(R.id.message_container);
        }

        void bind(Message message, int position) {
            tvFileName.setText(message.getFileName());
            tvFileSize.setText(FileAttachmentHelper.formatFileSize(message.getFileSize()));
            tvTime.setText(message.getFormattedTime());

            // Set file icon based on file type
            setFileIcon(message.getFileName());

            // Status indicators
            updateStatus(message.getStatus());

            // Click to open file
            messageContainer.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(message.getFileUrl()));
                v.getContext().startActivity(intent);
            });

            // Set up long click listener
            setupLongClickListener(messageContainer, message, position);
        }

        private void setFileIcon(String fileName) {
            if (fileName.endsWith(".pdf")) {
                ivFileIcon.setImageResource(R.drawable.ic_pdf);
            } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
                ivFileIcon.setImageResource(R.drawable.ic_doc);
            } else {
                ivFileIcon.setImageResource(R.drawable.ic_file);
            }
        }

        private void updateStatus(String status) {
            if ("sent".equals(status)) {
                tvStatus.setText("✓");
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.white_alpha_70));
            } else if ("delivered".equals(status)) {
                tvStatus.setText("✓✓");
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.white_alpha_70));
            } else if ("read".equals(status)) {
                tvStatus.setText("✓✓");
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.white));
            }
        }
    }

    // File Received ViewHolder
    class FileReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvFileSize, tvTime;
        ImageView ivFileIcon, ivDownload;
        LinearLayout messageContainer;

        FileReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileSize = itemView.findViewById(R.id.tv_file_size);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivFileIcon = itemView.findViewById(R.id.iv_file_icon);
            ivDownload = itemView.findViewById(R.id.iv_download);
            messageContainer = itemView.findViewById(R.id.message_container);
        }

        void bind(Message message, int position) {
            tvFileName.setText(message.getFileName());
            tvFileSize.setText(FileAttachmentHelper.formatFileSize(message.getFileSize()));
            tvTime.setText(message.getFormattedTime());

            // Set file icon
            setFileIcon(message.getFileName());

            // Click to download/open
            View.OnClickListener downloadListener = v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(message.getFileUrl()));
                v.getContext().startActivity(intent);
            };

            messageContainer.setOnClickListener(downloadListener);
            ivDownload.setOnClickListener(downloadListener);

            // Set up long click listener
            setupLongClickListener(messageContainer, message, position);
        }

        private void setFileIcon(String fileName) {
            if (fileName.endsWith(".pdf")) {
                ivFileIcon.setImageResource(R.drawable.ic_pdf);
            } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
                ivFileIcon.setImageResource(R.drawable.ic_doc);
            } else {
                ivFileIcon.setImageResource(R.drawable.ic_file);
            }
        }
    }

    // Image Sent ViewHolder
    class ImageSentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTime, tvStatus;

        ImageSentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }

        void bind(Message message, int position) {
            // Load image with Glide
            Glide.with(itemView.getContext())
                    .load(message.getFileUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(ivImage);

            tvTime.setText(message.getFormattedTime());

            // Status indicators
            updateStatus(message.getStatus());

            // Click to view full image
            ivImage.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ImageViewerActivity.class);
                intent.putExtra("image_url", message.getFileUrl());
                v.getContext().startActivity(intent);
            });

            // Set up long click listener for the image
            setupLongClickListener(ivImage, message, position);
        }

        private void updateStatus(String status) {
            if ("sent".equals(status)) {
                tvStatus.setText("✓");
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.white_alpha_70));
            } else if ("delivered".equals(status)) {
                tvStatus.setText("✓✓");
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.white_alpha_70));
            } else if ("read".equals(status)) {
                tvStatus.setText("✓✓");
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.white));
            }
        }
    }

    // Image Received ViewHolder
    class ImageReceivedViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTime;

        ImageReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        void bind(Message message, int position) {
            // Load image with Glide
            Glide.with(itemView.getContext())
                    .load(message.getFileUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(ivImage);

            tvTime.setText(message.getFormattedTime());

            // Click to view full image
            ivImage.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ImageViewerActivity.class);
                intent.putExtra("image_url", message.getFileUrl());
                v.getContext().startActivity(intent);
            });

            // Set up long click listener for the image
            setupLongClickListener(ivImage, message, position);
        }
    }
}