package com.example.internlink;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessagesActivity extends AppCompatActivity implements ConversationsAdapter.OnConversationClickListener {

    private static final String TAG = "MessagesActivity";

    private RecyclerView rvConversations;
    private LinearLayout tvEmptyState;
    private ConversationsAdapter conversationsAdapter;
    private List<Conversation> conversationsList = new ArrayList<>();

    private String currentUserId;
    private DatabaseReference chatMetadataRef;
    private DatabaseReference usersRef;
    private DatabaseReference chatsRef;
    private DatabaseReference notificationsRef;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText etSearch;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        // Get current user ID
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Current User ID: " + currentUserId);

        initializeViews();
        setupRecyclerView();
        setupFirebaseReferences();
        loadConversations();
    }

    private void initializeViews() {
        rvConversations = findViewById(R.id.messages_recycler_view);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        ImageView menuIcon = findViewById(R.id.menu_icon);
        ImageView filterIcon = findViewById(R.id.ic_filter);
        FloatingActionButton fabNewMessage = findViewById(R.id.fab_new_message);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadConversations();
        });
        etSearch = findViewById(R.id.message_search);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterConversations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


        menuIcon.setOnClickListener(v -> onBackPressed());
        fabNewMessage.setVisibility(View.VISIBLE);
        fabNewMessage.setOnClickListener(v -> {
            Intent intent = new Intent(MessagesActivity.this, AllCompanies.class);
            intent.putExtra("STUDENT_ID", FirebaseAuth.getInstance().getCurrentUser().getUid());
            startActivity(intent);
        });

    }
    private void filterConversations(String query) {
        List<Conversation> filteredList = new ArrayList<>();
        for (Conversation convo : conversationsList) {
            String name = convo.getOtherUserName() != null ? convo.getOtherUserName().toLowerCase() : "";
            String project = convo.getProjectTitle() != null ? convo.getProjectTitle().toLowerCase() : "";
            if (name.contains(query.toLowerCase()) || project.contains(query.toLowerCase())) {
                filteredList.add(convo);
            }
        }

        conversationsAdapter.updateList(filteredList);
    }


    private void setupRecyclerView() {
        conversationsAdapter = new ConversationsAdapter(conversationsList, this);
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(conversationsAdapter);
    }

    private void setupFirebaseReferences() {
        chatMetadataRef = FirebaseDatabase.getInstance().getReference("chat_metadata");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
    }

    private void loadConversations() {
        Log.d(TAG, "Loading conversations for user: " + currentUserId);

        // Show loading initially
        showEmptyState();

        // Load from chat_metadata first, then fallback to direct chats
        chatMetadataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Chat metadata exists: " + snapshot.exists());
                Log.d(TAG, "Chat metadata children count: " + snapshot.getChildrenCount());

                conversationsList.clear();
                Set<String> processedChatIds = new HashSet<>();

                if (snapshot.exists()) {
                    for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                        String chatId = chatSnapshot.getKey();
                        Log.d(TAG, "Processing chat metadata: " + chatId);

                        // Check if current user is a participant in this chat
                        DataSnapshot participantsSnapshot = chatSnapshot.child("participants");

                        if (participantsSnapshot.hasChild(currentUserId)) {
                            Log.d(TAG, "User is participant in chat: " + chatId);

                            // Get the other participant
                            String otherUserId = null;
                            for (DataSnapshot participantSnapshot : participantsSnapshot.getChildren()) {
                                String participantId = participantSnapshot.getKey();
                                if (!participantId.equals(currentUserId)) {
                                    otherUserId = participantId;
                                    break;
                                }
                            }

                            if (otherUserId != null) {
                                Log.d(TAG, "Found other user: " + otherUserId);

                                // Create conversation object
                                Conversation conversation = new Conversation();
                                conversation.setChatId(chatId);
                                conversation.setOtherUserId(otherUserId);

                                String lastMessage = chatSnapshot.child("lastMessage").getValue(String.class);
                                conversation.setLastMessage(lastMessage != null ? lastMessage : "No messages yet");

                                Long lastMessageTime = chatSnapshot.child("lastMessageTime").getValue(Long.class);
                                conversation.setLastMessageTime(lastMessageTime != null ? lastMessageTime : 0);

                                conversation.setLastSenderId(chatSnapshot.child("lastSenderId").getValue(String.class));

                                // Calculate unread count from notifications
                                calculateUnreadCount(conversation, otherUserId);

                                conversationsList.add(conversation);
                                processedChatIds.add(chatId);

                                Log.d(TAG, "Added conversation with user: " + otherUserId);
                            }
                        }
                    }
                }

                Log.d(TAG, "Found " + conversationsList.size() + " conversations in metadata");

                // Also check direct chats that might not be in metadata
                loadDirectChats(processedChatIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load chat metadata: " + error.getMessage());
                // Try loading direct chats as fallback
                loadDirectChats(new HashSet<>());
                swipeRefreshLayout.setRefreshing(false);

            }
        });
    }

    private void calculateUnreadCount(Conversation conversation, String otherUserId) {
        // Count unread chat message notifications from this user
        notificationsRef.child(currentUserId).orderByChild("type").equalTo("chat_message")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int unreadCount = 0;
                        for (DataSnapshot notifSnapshot : snapshot.getChildren()) {
                            String senderId = notifSnapshot.child("senderId").getValue(String.class);
                            Boolean read = notifSnapshot.child("read").getValue(Boolean.class);
                            String chatId = notifSnapshot.child("chatId").getValue(String.class);

                            if (otherUserId.equals(senderId) &&
                                    (read == null || !read) &&
                                    conversation.getChatId().equals(chatId)) {
                                unreadCount++;
                            }
                        }
                        conversation.setUnreadCount(unreadCount);

                        // Update adapter on main thread
                        runOnUiThread(() -> {
                            if (conversationsAdapter != null) {
                                conversationsAdapter.notifyDataSetChanged();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load notifications for unread count: " + error.getMessage());
                    }
                });
    }

    private void loadDirectChats(Set<String> alreadyProcessed) {
        Log.d(TAG, "Loading direct chats, already processed: " + alreadyProcessed.size());

        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Direct chats exist: " + snapshot.exists());
                Log.d(TAG, "Direct chats children count: " + snapshot.getChildrenCount());

                if (snapshot.exists()) {
                    for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                        String chatId = chatSnapshot.getKey();
                        Log.d(TAG, "Checking direct chat: " + chatId);

                        // Skip if already processed
                        if (alreadyProcessed.contains(chatId)) {
                            Log.d(TAG, "Skipping already processed chat: " + chatId);
                            continue;
                        }

                        // Check if current user is part of this chat ID
                        if (chatId != null && chatId.contains(currentUserId)) {
                            String otherUserId = getOtherUserIdFromChatId(chatId, currentUserId);

                            if (otherUserId != null) {
                                Log.d(TAG, "Found direct chat with: " + otherUserId);

                                // Check if there are actually messages in this chat
                                DataSnapshot messagesSnapshot = chatSnapshot.child("messages");
                                if (!messagesSnapshot.exists()) {
                                    Log.d(TAG, "No messages in chat " + chatId + ", skipping");
                                    continue;
                                }

                                // Get the last message from this chat
                                String lastMessage = "No messages yet";
                                long lastMessageTime = 0;
                                String lastSenderId = "";

                                // Get the last message
                                DataSnapshot lastMessageSnapshot = null;
                                for (DataSnapshot messageSnapshot : messagesSnapshot.getChildren()) {
                                    lastMessageSnapshot = messageSnapshot;
                                }

                                if (lastMessageSnapshot != null) {
                                    String msgText = lastMessageSnapshot.child("text").getValue(String.class);
                                    String messageType = lastMessageSnapshot.child("messageType").getValue(String.class);
                                    String fileName = lastMessageSnapshot.child("fileName").getValue(String.class);
                                    Long timestamp = lastMessageSnapshot.child("timestamp").getValue(Long.class);
                                    lastMessageTime = timestamp != null ? timestamp : 0;
                                    lastSenderId = lastMessageSnapshot.child("senderId").getValue(String.class);

                                    // Format message based on type
                                    if ("image".equals(messageType)) {
                                        lastMessage = "📷 Photo";
                                    } else if ("file".equals(messageType)) {
                                        lastMessage = "📎 " + (fileName != null ? fileName : "File");
                                    } else {
                                        lastMessage = msgText != null ? msgText : "No messages yet";
                                    }
                                }

                                // Create conversation object
                                Conversation conversation = new Conversation();
                                conversation.setChatId(chatId);
                                conversation.setOtherUserId(otherUserId);
                                conversation.setLastMessage(lastMessage);
                                conversation.setLastMessageTime(lastMessageTime);
                                conversation.setLastSenderId(lastSenderId);

                                // Calculate unread count
                                calculateUnreadCount(conversation, otherUserId);

                                conversationsList.add(conversation);

                                Log.d(TAG, "Added direct conversation with user: " + otherUserId);
                            }
                        }
                    }
                }

                Log.d(TAG, "Total conversations found: " + conversationsList.size());

                // Sort and load user details
                finishLoadingConversations();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load direct chats: " + error.getMessage());
                finishLoadingConversations();
                swipeRefreshLayout.setRefreshing(false);

            }
        });
    }

    private void finishLoadingConversations() {
        Log.d(TAG, "Finishing loading conversations, total: " + conversationsList.size());

        // Sort conversations by last message time (most recent first)
        Collections.sort(conversationsList, (c1, c2) ->
                Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));

        // Load user details for each conversation
        loadUserDetailsForConversations();
    }

    private String getOtherUserIdFromChatId(String chatId, String currentUserId) {
        if (chatId == null || currentUserId == null) return null;

        String[] userIds = chatId.split("_");
        if (userIds.length == 2) {
            if (userIds[0].equals(currentUserId)) {
                return userIds[1];
            } else if (userIds[1].equals(currentUserId)) {
                return userIds[0];
            }
        }
        return null;
    }

    private void loadUserDetailsForConversations() {
        Log.d(TAG, "Loading user details for " + conversationsList.size() + " conversations");

        if (conversationsList.isEmpty()) {
            showEmptyState();
            return;
        }

        final int totalConversations = conversationsList.size();
        final int[] loadedCount = {0};

        for (Conversation conversation : conversationsList) {
            Log.d(TAG, "Loading user details for: " + conversation.getOtherUserId());

            usersRef.child(conversation.getOtherUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String role = snapshot.child("role").getValue(String.class);
                        String logoUrl = snapshot.child("logoUrl").getValue(String.class);

                        conversation.setOtherUserName(name != null ? name : "Unknown User");
                        conversation.setOtherUserRole(role != null ? role : "user");
                        conversation.setOtherUserLogoUrl(logoUrl);
                        // 🔽 ADD THIS inside onDataChange after setting user info
                        DatabaseReference typingRef = FirebaseDatabase.getInstance()
                                .getReference("user_status")
                                .child(conversation.getOtherUserId())
                                .child("isTyping");

                        typingRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Boolean typing = snapshot.getValue(Boolean.class);
                                conversation.setTyping(Boolean.TRUE.equals(typing));
                                conversationsAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Failed to listen to typing status: " + error.getMessage());
                            }
                        });


                        Log.d(TAG, "Loaded user details for: " + name + " (role: " + role + ")");

                        // If this is a company, try to get project info from applications
                        if ("company".equals(role)) {
                            loadProjectInfoForConversation(conversation);
                        }
                    } else {
                        // User doesn't exist, set default values
                        conversation.setOtherUserName("Unknown User");
                        conversation.setOtherUserRole("user");
                        Log.w(TAG, "User not found: " + conversation.getOtherUserId());
                    }

                    loadedCount[0]++;
                    Log.d(TAG, "Loaded " + loadedCount[0] + " of " + totalConversations + " user details");

                    if (loadedCount[0] == totalConversations) {
                        // All user details loaded, update UI
                        Log.d(TAG, "All user details loaded, updating UI");
                        runOnUiThread(() -> {
                            if (conversationsList.isEmpty()) {
                                showEmptyState();
                            } else {
                                hideEmptyState();
                                conversationsAdapter.notifyDataSetChanged();
                            }
                            swipeRefreshLayout.setRefreshing(false);
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load user details for " + conversation.getOtherUserId() + ": " + error.getMessage());
                    // Set default values on error
                    conversation.setOtherUserName("Unknown User");
                    conversation.setOtherUserRole("user");

                    loadedCount[0]++;
                    if (loadedCount[0] == totalConversations) {
                        runOnUiThread(() -> {
                            if (conversationsList.isEmpty()) {
                                showEmptyState();
                            } else {
                                hideEmptyState();
                                conversationsAdapter.notifyDataSetChanged();
                            }
                            swipeRefreshLayout.setRefreshing(false);
                        });
                    }
                }
            });
        }

    }

    private void loadProjectInfoForConversation(Conversation conversation) {
        // Look for applications between current user and this company
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        applicationsRef.orderByChild("userId").equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String companyId = appSnapshot.child("companyId").getValue(String.class);

                            if (conversation.getOtherUserId().equals(companyId)) {
                                String projectId = appSnapshot.child("projectId").getValue(String.class);
                                String applicationId = appSnapshot.getKey();

                                conversation.setProjectId(projectId);
                                conversation.setApplicationId(applicationId);

                                Log.d(TAG, "Found project info for conversation: " + projectId);

                                // Load project title
                                if (projectId != null) {
                                    loadProjectTitle(conversation, projectId);
                                }
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load project info: " + error.getMessage());
                    }
                });
    }

    private void loadProjectTitle(Conversation conversation, String projectId) {
        DatabaseReference projectRef = FirebaseDatabase.getInstance()
                .getReference("projects").child(projectId);

        projectRef.child("title").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String projectTitle = snapshot.getValue(String.class);
                conversation.setProjectTitle(projectTitle);

                Log.d(TAG, "Loaded project title: " + projectTitle);

                runOnUiThread(() -> {
                    if (conversationsAdapter != null) {
                        conversationsAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load project title: " + error.getMessage());
            }
        });
    }

    private void showEmptyState() {
        Log.d(TAG, "Showing empty state");
        runOnUiThread(() -> {
            rvConversations.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        });
    }

    private void hideEmptyState() {
        Log.d(TAG, "Hiding empty state");
        runOnUiThread(() -> {
            rvConversations.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        });
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        Log.d(TAG, "Conversation clicked: " + conversation.getOtherUserName());
        // Mark all notifications for this chat as read
        notificationsRef.child(currentUserId)
                .orderByChild("chatId")
                .equalTo(conversation.getChatId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot notifSnapshot : snapshot.getChildren()) {
                            notifSnapshot.getRef().child("read").setValue(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to mark notifications as read: " + error.getMessage());
                    }
                });
        conversation.setUnreadCount(0);
        conversationsAdapter.notifyDataSetChanged();


        Intent intent = new Intent(this, StudentChatActivity.class);
        intent.putExtra("COMPANY_ID", conversation.getOtherUserId());
        intent.putExtra("COMPANY_NAME", conversation.getOtherUserName());

        if (conversation.getProjectId() != null) {
            intent.putExtra("PROJECT_ID", conversation.getProjectId());
        }
        if (conversation.getProjectTitle() != null) {
            intent.putExtra("PROJECT_TITLE", conversation.getProjectTitle());
        }
        if (conversation.getApplicationId() != null) {
            intent.putExtra("APPLICATION_ID", conversation.getApplicationId());
        }

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh conversations when returning to this activity
        if (conversationsAdapter != null) {
            loadConversations();
        }
    }
}