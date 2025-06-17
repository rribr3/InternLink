package com.example.internlink;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Nullable;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private FloatingActionButton fabSend;
    private TextView tvChatName, tvOnlineStatus, tvTypingIndicator, tvInterviewInfo;
    private ImageView ivProfile, ivAttach, ivMoreOptions;
    private LinearLayout llTypingIndicator;
    private CardView cvInterviewBanner;
    private MaterialButton btnInterviewDetails;

    private MessagesAdapter messagesAdapter;
    private List<Message> messagesList = new ArrayList<>();

    private String chatWithId;
    private String chatWithName;
    private String currentUserId;
    private String chatId;

    private DatabaseReference messagesRef;
    private DatabaseReference typingRef;
    private DatabaseReference userStatusRef;
    private ValueEventListener messagesListener;
    private ValueEventListener typingListener;
    private ValueEventListener statusListener;
    private FileAttachmentHelper fileAttachmentHelper;
    private ProgressDialog progressDialog;
    private Applicant applicant;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        if (!PermissionHelper.isNotificationPermissionGranted(this)) {
            if (PermissionHelper.shouldShowNotificationPermissionRationale(this)) {
                // Show explanation dialog
                new AlertDialog.Builder(this)
                        .setTitle("Notification Permission")
                        .setMessage("We need notification permission to alert you about new messages.")
                        .setPositiveButton("Grant", (dialog, which) -> {
                            PermissionHelper.requestNotificationPermission(this);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                PermissionHelper.requestNotificationPermission(this);
            }
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);

        initializeViews();
        // Initialize FileAttachmentHelper
        // Initialize FileAttachmentHelper
        fileAttachmentHelper = new FileAttachmentHelper(this, new FileAttachmentHelper.OnFileUploadListener() {
            @Override
            public void onUploadProgress(int progress) {
                if (!progressDialog.isShowing()) {
                    progressDialog.show();
                }
                progressDialog.setProgress(progress);
            }

            @Override
            public void onUploadSuccess(String fileUrl, String fileName, long fileSize, String fileType) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                if (fileType != null && fileType.startsWith("image/")) {
                    sendImageMessage(fileUrl, fileName, fileSize, fileType);
                } else {
                    sendFileMessage(fileUrl, fileName, fileSize, fileType);
                }
            }

            @Override
            public void onUploadFailure(String error) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(ChatActivity.this, "Upload failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
        getIntentData();
        setupFirebaseReferences();
        setupToolbar();
        setupMessageInput();
        setupRecyclerView();
        loadMessages();
        setupInterviewBanner();
        setupClickListeners();

        // Set user online status
        setUserOnlineStatus(true);
    }
    private void sendImageMessage(String imageUrl, String fileName, long fileSize, String fileType) {
        String messageId = messagesRef.push().getKey();
        if (messageId == null) return;

        Message message = new Message();
        message.setText(fileName);
        message.setSenderId(currentUserId);
        message.setReceiverId(chatWithId);
        message.setTimestamp(System.currentTimeMillis());
        message.setMessageType("image");
        message.setStatus("sent");
        message.setFileUrl(imageUrl);
        message.setFileName(fileName);
        message.setFileSize(fileSize);

        messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    // Update chat metadata
                    updateChatMetadata("📷 Photo");

                    // Send notification
                    sendNotificationToRecipient("Sent a photo");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send image", Toast.LENGTH_SHORT).show();
                });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionHelper.NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Notification permission granted
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                // Notification permission denied
                Toast.makeText(this, "Notifications disabled. You won't receive message alerts.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeViews() {
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        fabSend = findViewById(R.id.fab_send);
        tvChatName = findViewById(R.id.tv_chat_name);
        tvOnlineStatus = findViewById(R.id.tv_online_status);
        tvTypingIndicator = findViewById(R.id.tv_typing_indicator);
        tvInterviewInfo = findViewById(R.id.tv_interview_info);
        ivProfile = findViewById(R.id.iv_profile);
        ivAttach = findViewById(R.id.iv_attach);
        ivMoreOptions = findViewById(R.id.iv_more_options);
        llTypingIndicator = findViewById(R.id.ll_typing_indicator);
        cvInterviewBanner = findViewById(R.id.cv_interview_banner);
        btnInterviewDetails = findViewById(R.id.btn_interview_details);
    }
    private void showAttachmentOptions() {
        View view = getLayoutInflater().inflate(R.layout.layout_attachment_options, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(view);

        // Setup click listeners
        view.findViewById(R.id.image_option).setOnClickListener(v -> {
            fileAttachmentHelper.pickImage();
            bottomSheetDialog.dismiss();
        });

        view.findViewById(R.id.file_option).setOnClickListener(v -> {
            fileAttachmentHelper.pickFile();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (fileAttachmentHelper != null) {
            fileAttachmentHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }

    private void sendFileMessage(String fileUrl, String fileName, long fileSize, String fileType) {
        String messageId = messagesRef.push().getKey();
        if (messageId == null) return;

        Message message = new Message();
        message.setText(fileName);
        message.setSenderId(currentUserId);
        message.setReceiverId(chatWithId);
        message.setTimestamp(System.currentTimeMillis());
        message.setMessageType("file");
        message.setStatus("sent");
        message.setFileUrl(fileUrl);
        message.setFileName(fileName);
        message.setFileSize(fileSize);

        messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    // Update chat metadata
                    updateChatMetadata("📎 " + fileName);

                    // Send notification
                    sendNotificationToRecipient("Sent an attachment: " + fileName);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send file", Toast.LENGTH_SHORT).show();
                });
    }

    private void getIntentData() {
        chatWithId = getIntent().getStringExtra("CHAT_WITH_ID");
        chatWithName = getIntent().getStringExtra("CHAT_WITH_NAME");

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = auth.getCurrentUser().getUid();

        if (chatWithId == null || chatWithName == null || currentUserId == null) {
            Toast.makeText(this, "Error: Missing chat information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Create chat ID (consistent between users)
        chatId = currentUserId.compareTo(chatWithId) < 0 ?
                currentUserId + "_" + chatWithId :
                chatWithId + "_" + currentUserId;

        // Log for debugging
        android.util.Log.d("ChatActivity", "Chat ID: " + chatId);
        android.util.Log.d("ChatActivity", "Current User ID: " + currentUserId);
        android.util.Log.d("ChatActivity", "Chat With ID: " + chatWithId);
    }

    private void setupFirebaseReferences() {
        messagesRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages");
        typingRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("typing");
        userStatusRef = FirebaseDatabase.getInstance().getReference("user_status").child(chatWithId);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvChatName.setText(chatWithName);

        // Load user profile (optional)
        loadUserProfile();
    }

    private void setupMessageInput() {
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                fabSend.setEnabled(hasText);

                // Show typing indicator
                if (hasText && before == 0 && count == 1) {
                    setTypingStatus(true);
                } else if (!hasText && before == 1 && count == 0) {
                    setTypingStatus(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        messagesAdapter = new MessagesAdapter(messagesList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messagesAdapter);
    }

    private void setupClickListeners() {
        fabSend.setOnClickListener(v -> sendMessage());

        ivAttach.setOnClickListener(v -> {
            // Show attachment options
            showAttachmentOptions();
        });

        ivMoreOptions.setOnClickListener(v -> showMoreOptionsMenu());

        btnInterviewDetails.setOnClickListener(v -> {
            // Show interview details dialog or navigate to details
            showInterviewDetails();
        });
        ivProfile.setOnClickListener(v -> {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(chatWithId);

            userRef.child("logoUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String logoUrl = snapshot.getValue(String.class);
                    if (logoUrl != null && !logoUrl.isEmpty()) {
                        // Show full size image in dialog or new activity
                        showFullSizeImage(logoUrl);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ChatActivity.this,
                            "Failed to load profile picture", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    private void showFullSizeImage(String imageUrl) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView imageView = new ImageView(this);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(imageView);

        dialog.setContentView(imageView);
        dialog.show();

        // Close dialog when clicked
        imageView.setOnClickListener(v -> dialog.dismiss());
    }

    private void loadMessages() {
        messagesListener = messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messagesList.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null) {
                        message.setId(messageSnapshot.getKey());
                        messagesList.add(message);
                    }
                }
                messagesAdapter.notifyDataSetChanged();
                if (!messagesList.isEmpty()) {
                    rvMessages.scrollToPosition(messagesList.size() - 1);
                }

                // Mark messages as read
                markMessagesAsRead();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });

        // Listen for typing indicator
        typingListener = typingRef.child(chatWithId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isTyping = snapshot.getValue(Boolean.class);
                if (isTyping != null && isTyping) {
                    llTypingIndicator.setVisibility(View.VISIBLE);
                    tvTypingIndicator.setText(chatWithName + " is typing...");
                } else {
                    llTypingIndicator.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Listen for user online status
        statusListener = userStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOnline = snapshot.child("online").getValue(Boolean.class);
                Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);

                if (isOnline != null && isOnline) {
                    tvOnlineStatus.setText("🟢 Online");
                } else if (lastSeen != null) {
                    String lastSeenText = getLastSeenText(lastSeen);
                    tvOnlineStatus.setText("⚪ " + lastSeenText);
                } else {
                    tvOnlineStatus.setText("⚪ Offline");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) return;

        String messageId = messagesRef.push().getKey();
        if (messageId == null) {
            Toast.makeText(this, "Error: Could not generate message ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Message message = new Message();
        message.setText(messageText);
        message.setSenderId(currentUserId);
        message.setReceiverId(chatWithId);
        message.setTimestamp(System.currentTimeMillis());
        message.setMessageType("text");
        message.setStatus("sent");

        messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    etMessage.setText("");
                    setTypingStatus(false);

                    // Update chat metadata
                    updateChatMetadata(messageText);

                    // Send notification to recipient
                    sendNotificationToRecipient(messageText);
                })
                .addOnFailureListener(e -> {
                    // Log the actual error
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateChatMetadata(String lastMessage) {
        DatabaseReference chatMetaRef = FirebaseDatabase.getInstance()
                .getReference("chat_metadata").child(chatId);

        Map<String, Object> metaData = new HashMap<>();
        metaData.put("lastMessage", lastMessage);
        metaData.put("lastMessageTime", ServerValue.TIMESTAMP);
        metaData.put("lastSenderId", currentUserId);

        // Participant info
        Map<String, Object> participants = new HashMap<>();
        participants.put(currentUserId, true);
        participants.put(chatWithId, true);
        metaData.put("participants", participants);

        chatMetaRef.updateChildren(metaData);
    }

    private void sendNotificationToRecipient(String messageText) {
        // Create notification for the recipient
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications").child(chatWithId);

        String notificationId = notificationsRef.push().getKey();
        if (notificationId == null) return;

        // Get current user's name first
        DatabaseReference currentUserRef = FirebaseDatabase.getInstance()
                .getReference("users").child(currentUserId);

        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String senderName = snapshot.child("name").getValue(String.class);
                if (senderName == null) senderName = "Company";

                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "chat_message");
                notification.put("title", "New message from " + senderName);
                notification.put("message", messageText);
                notification.put("senderId", currentUserId);
                notification.put("senderName", senderName);
                notification.put("chatId", chatId);
                notification.put("timestamp", ServerValue.TIMESTAMP);
                notification.put("read", false);

                notificationsRef.child(notificationId).setValue(notification);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setTypingStatus(boolean isTyping) {
        if (typingRef != null && currentUserId != null) {
            typingRef.child(currentUserId).setValue(isTyping);

            // Auto-remove typing status after 3 seconds
            if (isTyping) {
                // Use a handler to delay the removal
                new android.os.Handler().postDelayed(() -> {
                    if (typingRef != null && currentUserId != null) {
                        typingRef.child(currentUserId).setValue(false);
                    }
                }, 3000);
            }
        }
    }

    private void markMessagesAsRead() {
        for (Message message : messagesList) {
            if (!message.getSenderId().equals(currentUserId) &&
                    !"read".equals(message.getStatus())) {
                messagesRef.child(message.getId()).child("status").setValue("read");
            }
        }
    }

    private void setUserOnlineStatus(boolean isOnline) {
        if (currentUserId != null) {
            DatabaseReference myStatusRef = FirebaseDatabase.getInstance()
                    .getReference("user_status").child(currentUserId);

            Map<String, Object> status = new HashMap<>();
            status.put("online", isOnline);
            status.put("lastSeen", ServerValue.TIMESTAMP);

            myStatusRef.updateChildren(status);

            // Set offline when app is closed
            if (isOnline) {
                myStatusRef.child("online").onDisconnect().setValue(false);
                myStatusRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP);
            }
        }
    }

    private void loadUserProfile() {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(chatWithId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String logoUrl = snapshot.child("logoUrl").getValue(String.class);

                // Set default profile picture
                if (logoUrl == null || logoUrl.isEmpty()) {
                    ivProfile.setImageResource(R.drawable.ic_profile);
                    return;
                }

                // Load profile picture using Glide with error handling
                Glide.with(ChatActivity.this)
                        .load(logoUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        Target<Drawable> target, boolean isFirstResource) {
                                // Handle load failure
                                Toast.makeText(ChatActivity.this,
                                        "Failed to load profile picture", Toast.LENGTH_SHORT).show();
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                           Target<Drawable> target, DataSource dataSource,
                                                           boolean isFirstResource) {
                                // Image loaded successfully
                                return false;
                            }
                        })
                        .into(ivProfile);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Set default profile picture on error
                ivProfile.setImageResource(R.drawable.ic_profile);
                Toast.makeText(ChatActivity.this,
                        "Failed to load profile data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupInterviewBanner() {
        // Check if there's a scheduled interview with this student
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
        String companyId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String userId = appSnapshot.child("userId").getValue(String.class);
                            String status = appSnapshot.child("status").getValue(String.class);
                            String interviewDate = appSnapshot.child("interviewDate").getValue(String.class);
                            String interviewTime = appSnapshot.child("interviewTime").getValue(String.class);

                            if (chatWithId.equals(userId) && "Shortlisted".equals(status) &&
                                    interviewDate != null && interviewTime != null) {

                                cvInterviewBanner.setVisibility(View.VISIBLE);
                                tvInterviewInfo.setText(interviewDate + " at " + interviewTime);
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showMoreOptionsMenu() {
        PopupMenu popup = new PopupMenu(this, ivMoreOptions);
        popup.getMenuInflater().inflate(R.menu.menu_chat_options, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_create_issue) {
                showIssueTypeSelector();
                return true;
            }
            // Handle other menu items
            return false;
        });

        popup.show();
    }

    private void showInterviewDetails() {
        // Implementation to show interview details
        Toast.makeText(this, "Interview details", Toast.LENGTH_SHORT).show();
    }

    private void viewStudentProfile() {
        // Use the existing chatWithId and chatWithName instead of applicant object
        Intent intent = new Intent(this, ApplicantProfileActivity.class);
        intent.putExtra("APPLICANT_ID", chatWithId);  // Use chatWithId instead of applicant.getUserId()
        intent.putExtra("APPLICANT_NAME", chatWithName);  // Use chatWithName instead of applicant.getName()
        startActivity(intent);
    }

    private void scheduleInterview() {
        // Open schedule interview dialog or activity
        Toast.makeText(this, "Schedule interview feature", Toast.LENGTH_SHORT).show();
    }

    private void clearChat() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear Chat")
                .setMessage("Are you sure you want to clear this chat? This action cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    messagesRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to clear chat", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reportIssue() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Report Issue")
                .setMessage("What type of issue would you like to report?")
                .setItems(new String[]{
                        "🚫 Inappropriate behavior",
                        "📧 Spam messages",
                        "🔒 Privacy concerns",
                        "🐛 Technical issue",
                        "❓ Other"
                }, (dialog, which) -> {
                    String issueType = "";
                    switch (which) {
                        case 0: issueType = "Inappropriate behavior"; break;
                        case 1: issueType = "Spam messages"; break;
                        case 2: issueType = "Privacy concerns"; break;
                        case 3: issueType = "Technical issue"; break;
                        case 4: issueType = "Other"; break;
                    }

                    // Create report in Firebase
                    createIssueReport(issueType);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showIssueTypeSelector() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_issue_type_selector, null);
        dialog.setContentView(view);

        RecyclerView rvIssueTypes = view.findViewById(R.id.rv_issue_types);
        rvIssueTypes.setLayoutManager(new LinearLayoutManager(this));

        IssueTypeAdapter adapter = new IssueTypeAdapter(issueType -> {
            // Handle issue type selection
            createIssue(issueType);
            dialog.dismiss();
        });

        rvIssueTypes.setAdapter(adapter);
        dialog.show();
    }

    private void createIssue(IssueType issueType) {
        // Show a dialog to get issue description
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.layout_issue_description, null);
        TextInputEditText etDescription = view.findViewById(R.id.et_description);

        builder.setTitle(issueType.getEmoji() + " New " + issueType.getLabel())
                .setView(view)
                .setPositiveButton("Create", (dialog, which) -> {
                    String description = etDescription.getText().toString().trim();
                    if (description.isEmpty()) {
                        Toast.makeText(this, "Please add a description", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DatabaseReference issuesRef = FirebaseDatabase.getInstance()
                            .getReference("issues").child(chatId);

                    String issueId = issuesRef.push().getKey();
                    if (issueId == null) return;

                    // First get current user's name
                    DatabaseReference currentUserRef = FirebaseDatabase.getInstance()
                            .getReference("users").child(currentUserId);

                    currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String creatorName = snapshot.child("name").getValue(String.class);
                            if (creatorName == null) creatorName = "Unknown User";

                            // Get current UTC timestamp in YYYY-MM-DD HH:MM:SS format
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                            String currentTimestamp = sdf.format(new Date());

                            Map<String, Object> issue = new HashMap<>();
                            issue.put("type", issueType.name());
                            issue.put("description", description);
                            issue.put("createdBy", currentUserId);
                            issue.put("creatorName", creatorName);
                            issue.put("timestamp", currentTimestamp);
                            issue.put("status", "pending");
                            issue.put("chatId", chatId);
                            issue.put("reportedUserId", chatWithId);
                            issue.put("reportedUserName", chatWithName);

                            String finalCreatorName = creatorName;
                            issuesRef.child(issueId).setValue(issue)
                                    .addOnSuccessListener(aVoid -> {
                                        // Create notification for admins about new issue
                                        createIssueNotification(issueId, issueType, finalCreatorName, currentTimestamp, description);

                                        Toast.makeText(ChatActivity.this,
                                                issueType.getEmoji() + " Issue created successfully",
                                                Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(ChatActivity.this,
                                                "Failed to create issue: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                        android.util.Log.e("ChatActivity", "Error creating issue", e);
                                    });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(ChatActivity.this,
                                    "Failed to create issue: Could not get user information",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void createIssueNotification(String issueId, IssueType issueType, String creatorName, String timestamp, String description) {
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("announcements_by_role").child("admin");

        String notificationId = notificationsRef.push().getKey();
        if (notificationId == null) return;

        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "new_issue");
        notification.put("issueId", issueId);
        notification.put("issueType", issueType.name());
        notification.put("chatId", chatId);
        notification.put("createdBy", currentUserId);
        notification.put("creatorName", creatorName);
        notification.put("reportedUserId", chatWithId);
        notification.put("reportedUserName", chatWithName);
        notification.put("date", timestamp);
        notification.put("status", "unread");
        notification.put("title", issueType.getEmoji() + " New " + issueType.getLabel() + " Issue");
        notification.put("message", description); // Using the actual description as the message

        notificationsRef.child(notificationId).setValue(notification)
                .addOnFailureListener(e -> {
                    android.util.Log.e("ChatActivity", "Error creating issue notification", e);
                });
    }

    private void createIssueReport(String issueType) {
        DatabaseReference reportsRef = FirebaseDatabase.getInstance()
                .getReference("reports");

        String reportId = reportsRef.push().getKey();
        if (reportId == null) return;

        Map<String, Object> report = new HashMap<>();
        report.put("reporterId", currentUserId);
        report.put("reportedUserId", chatWithId);
        report.put("chatId", chatId);
        report.put("issueType", issueType);
        report.put("timestamp", ServerValue.TIMESTAMP);
        report.put("status", "pending");

        reportsRef.child(reportId).setValue(report)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Report submitted successfully. We'll review it shortly.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show();
                });
    }

    private String getLastSeenText(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) { // Less than 1 minute
            return "Last seen just now";
        } else if (diff < 3600000) { // Less than 1 hour
            int minutes = (int) (diff / 60000);
            return "Last seen " + minutes + " min ago";
        } else if (diff < 86400000) { // Less than 1 day
            int hours = (int) (diff / 3600000);
            return "Last seen " + hours + " hr ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
            return "Last seen " + sdf.format(new Date(timestamp));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove listeners
        if (messagesListener != null && messagesRef != null) {
            messagesRef.removeEventListener(messagesListener);
        }
        if (typingListener != null && typingRef != null) {
            typingRef.child(chatWithId).removeEventListener(typingListener);
        }
        if (statusListener != null && userStatusRef != null) {
            userStatusRef.removeEventListener(statusListener);
        }

        // Set user offline
        setUserOnlineStatus(false);

        // Clear typing status
        if (typingRef != null && currentUserId != null) {
            typingRef.child(currentUserId).setValue(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        setUserOnlineStatus(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUserOnlineStatus(true);
    }
}