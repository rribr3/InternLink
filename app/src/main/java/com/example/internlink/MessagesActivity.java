package com.example.internlink;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
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
    private LinearLayout tabActive, tabArchive;
    private TextView tabActiveText, tabArchiveText;
    private View tabActiveIndicator, tabArchiveIndicator;
    private RecyclerView rvArchivedConversations;
    private LinearLayout tvEmptyStateArchived;
    private ConversationsAdapter archivedConversationsAdapter;
    private List<Conversation> archivedConversationsList = new ArrayList<>();
    private boolean isShowingActiveTab = true;

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
        setupRecyclerViews();
        setupSwipeGestures();
        setupFirebaseReferences();
        loadConversations();
    }

    private void initializeViews() {
        rvConversations = findViewById(R.id.messages_recycler_view);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        tabActive = findViewById(R.id.tab_active);
        tabArchive = findViewById(R.id.tab_archive);
        tabActiveText = findViewById(R.id.tab_active_text);
        tabArchiveText = findViewById(R.id.tab_archive_text);
        tabActiveIndicator = findViewById(R.id.tab_active_indicator);
        tabArchiveIndicator = findViewById(R.id.tab_archive_indicator);
        rvArchivedConversations = findViewById(R.id.archived_messages_recycler_view);
        tvEmptyStateArchived = findViewById(R.id.tv_empty_state_archive);

        tabActive.setOnClickListener(v -> showActiveTab());
        tabArchive.setOnClickListener(v -> showArchiveTab());

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

    private void setupRecyclerViews() {
        // Setup active conversations RecyclerView
        conversationsAdapter = new ConversationsAdapter(conversationsList, this);
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(conversationsAdapter);

        // Setup archived conversations RecyclerView
        archivedConversationsAdapter = new ConversationsAdapter(archivedConversationsList, this);
        rvArchivedConversations.setLayoutManager(new LinearLayoutManager(this));
        rvArchivedConversations.setAdapter(archivedConversationsAdapter);
    }

    private void setupSwipeGestures() {
        // Setup swipe for active conversations
        setupSwipeGestureForRecyclerView(rvConversations, conversationsList, conversationsAdapter, false);

        // Setup swipe for archived conversations
        setupSwipeGestureForRecyclerView(rvArchivedConversations, archivedConversationsList, archivedConversationsAdapter, true);
    }

    private void setupSwipeGestureForRecyclerView(RecyclerView recyclerView, List<Conversation> conversations,
                                                  ConversationsAdapter adapter, boolean isArchived) {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Conversation conversation = conversations.get(position);

                // Show custom action options
                showSwipeActionDialog(conversation, position, isArchived);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                View foregroundView = ((ConversationsAdapter.ConversationViewHolder) viewHolder).itemView.findViewById(R.id.foreground_layout);
                getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, dX, dY,
                        actionState, isCurrentlyActive);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                View foregroundView = ((ConversationsAdapter.ConversationViewHolder) viewHolder).itemView.findViewById(R.id.foreground_layout);
                getDefaultUIUtil().clearView(foregroundView);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void showSwipeActionDialog(Conversation conversation, int position, boolean isFromArchived) {
        String archiveAction = isFromArchived ? "Unarchive" : "Archive";

        new AlertDialog.Builder(this)
                .setTitle("Choose Action")
                .setItems(new CharSequence[]{archiveAction, "Delete"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // Archive/Unarchive
                            if (isFromArchived) {
                                unarchiveConversation(conversation, position);
                            } else {
                                archiveConversation(conversation, position);
                            }
                            break;
                        case 1:
                            // Delete
                            deleteConversation(conversation, position, isFromArchived);
                            break;
                    }
                })
                .setOnDismissListener(dialog -> {
                    // Reset swipe for the appropriate adapter
                    if (isFromArchived) {
                        archivedConversationsAdapter.notifyItemChanged(position);
                    } else {
                        conversationsAdapter.notifyItemChanged(position);
                    }
                })
                .show();
    }

    private void archiveConversation(Conversation conversation, int position) {
        // Update Firebase
        chatMetadataRef.child(conversation.getChatId())
                .child("archivedBy")
                .child(currentUserId)
                .setValue(true)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Conversation archived", Toast.LENGTH_SHORT).show();

                    // Update local state
                    conversation.setArchived(true);

                    // Move from active to archived list
                    conversationsList.remove(position);
                    archivedConversationsList.add(0, conversation); // Add to beginning

                    // Update UI
                    conversationsAdapter.notifyItemRemoved(position);
                    archivedConversationsAdapter.notifyItemInserted(0);

                    updateEmptyStates();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to archive", Toast.LENGTH_SHORT).show();
                    conversationsAdapter.notifyItemChanged(position);
                });
    }

    private void unarchiveConversation(Conversation conversation, int position) {
        // Update Firebase
        chatMetadataRef.child(conversation.getChatId())
                .child("archivedBy")
                .child(currentUserId)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Conversation unarchived", Toast.LENGTH_SHORT).show();

                    // Update local state
                    conversation.setArchived(false);

                    // Move from archived to active list
                    archivedConversationsList.remove(position);

                    // Insert into active list in correct position based on timestamp
                    int insertPosition = findInsertPosition(conversation);
                    conversationsList.add(insertPosition, conversation);

                    // Update UI
                    archivedConversationsAdapter.notifyItemRemoved(position);
                    conversationsAdapter.notifyItemInserted(insertPosition);

                    updateEmptyStates();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to unarchive", Toast.LENGTH_SHORT).show();
                    archivedConversationsAdapter.notifyItemChanged(position);
                });
    }

    private int findInsertPosition(Conversation conversation) {
        // Find the correct position to insert based on timestamp (most recent first)
        for (int i = 0; i < conversationsList.size(); i++) {
            if (conversation.getLastMessageTime() > conversationsList.get(i).getLastMessageTime()) {
                return i;
            }
        }
        return conversationsList.size(); // Insert at end if oldest
    }

    private void deleteConversation(Conversation conversation, int position, boolean isFromArchived) {
        // Soft-delete: mark this chat as deleted for this user in chat_metadata
        chatMetadataRef.child(conversation.getChatId())
                .child("deletedBy")
                .child(currentUserId)
                .setValue(true)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Conversation deleted", Toast.LENGTH_SHORT).show();

                    if (isFromArchived) {
                        archivedConversationsList.remove(position);
                        archivedConversationsAdapter.notifyItemRemoved(position);
                    } else {
                        conversationsList.remove(position);
                        conversationsAdapter.notifyItemRemoved(position);
                    }

                    updateEmptyStates();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show();
                    if (isFromArchived) {
                        archivedConversationsAdapter.notifyItemChanged(position);
                    } else {
                        conversationsAdapter.notifyItemChanged(position);
                    }
                });
    }

    private void showActiveTab() {
        isShowingActiveTab = true;
        rvConversations.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(conversationsList.isEmpty() ? View.VISIBLE : View.GONE);
        rvArchivedConversations.setVisibility(View.GONE);
        tvEmptyStateArchived.setVisibility(View.GONE);

        tabActiveText.setTextColor(getResources().getColor(R.color.blue));
        tabActiveIndicator.setBackgroundColor(getResources().getColor(R.color.blue));
        tabArchiveText.setTextColor(Color.parseColor("#808080"));
        tabArchiveIndicator.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

    private void showArchiveTab() {
        isShowingActiveTab = false;
        rvConversations.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        rvArchivedConversations.setVisibility(View.VISIBLE);
        tvEmptyStateArchived.setVisibility(archivedConversationsList.isEmpty() ? View.VISIBLE : View.GONE);

        tabArchiveText.setTextColor(getResources().getColor(R.color.blue));
        tabArchiveIndicator.setBackgroundColor(getResources().getColor(R.color.blue));
        tabActiveText.setTextColor(Color.parseColor("#808080"));
        tabActiveIndicator.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

    private void updateEmptyStates() {
        runOnUiThread(() -> {
            if (isShowingActiveTab) {
                tvEmptyState.setVisibility(conversationsList.isEmpty() ? View.VISIBLE : View.GONE);
            } else {
                tvEmptyStateArchived.setVisibility(archivedConversationsList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void filterConversations(String query) {
        List<Conversation> filteredList = new ArrayList<>();
        List<Conversation> sourceList = isShowingActiveTab ? conversationsList : archivedConversationsList;

        for (Conversation convo : sourceList) {
            String name = convo.getOtherUserName() != null ? convo.getOtherUserName().toLowerCase() : "";
            String project = convo.getProjectTitle() != null ? convo.getProjectTitle().toLowerCase() : "";
            if (name.contains(query.toLowerCase()) || project.contains(query.toLowerCase())) {
                filteredList.add(convo);
            }
        }

        if (isShowingActiveTab) {
            conversationsAdapter.updateList(filteredList);
        } else {
            archivedConversationsAdapter.updateList(filteredList);
        }
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
                archivedConversationsList.clear();
                Set<String> processedChatIds = new HashSet<>();

                if (snapshot.exists()) {
                    for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                        String chatId = chatSnapshot.getKey();
                        Log.d(TAG, "Processing chat metadata: " + chatId);

                        Boolean deleted = chatSnapshot.child("deletedBy").child(currentUserId).getValue(Boolean.class);
                        if (Boolean.TRUE.equals(deleted)) {
                            Log.d(TAG, "Conversation hidden for this user: " + chatId);
                            continue; // Skip this conversation
                        }

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

                                // Check if conversation is archived
                                Boolean archived = chatSnapshot.child("archivedBy").child(currentUserId).getValue(Boolean.class);
                                conversation.setArchived(Boolean.TRUE.equals(archived));

                                // Calculate unread count from notifications
                                calculateUnreadCount(conversation, otherUserId);

                                // Add to appropriate list
                                if (conversation.isArchived()) {
                                    archivedConversationsList.add(conversation);
                                } else {
                                    conversationsList.add(conversation);
                                }

                                processedChatIds.add(chatId);

                                Log.d(TAG, "Added conversation with user: " + otherUserId + " (archived: " + conversation.isArchived() + ")");
                            }
                        }
                    }
                }

                Log.d(TAG, "Found " + conversationsList.size() + " active and " + archivedConversationsList.size() + " archived conversations in metadata");

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
                            if (archivedConversationsAdapter != null) {
                                archivedConversationsAdapter.notifyDataSetChanged();
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
                        Boolean deleted = chatSnapshot.child("deletedBy").child(currentUserId).getValue(Boolean.class);
                        if (Boolean.TRUE.equals(deleted)) {
                            Log.d(TAG, "Conversation hidden for this user: " + chatId);
                            continue; // Skip this conversation
                        }

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
                                conversation.setArchived(false); // Direct chats default to not archived

                                // Calculate unread count
                                calculateUnreadCount(conversation, otherUserId);

                                conversationsList.add(conversation);

                                Log.d(TAG, "Added direct conversation with user: " + otherUserId);
                            }
                        }
                    }
                }

                Log.d(TAG, "Total conversations found: " + (conversationsList.size() + archivedConversationsList.size()));

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
        Log.d(TAG, "Finishing loading conversations, active: " + conversationsList.size() + ", archived: " + archivedConversationsList.size());

        // Sort conversations by last message time (most recent first)
        Collections.sort(conversationsList, (c1, c2) ->
                Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));

        Collections.sort(archivedConversationsList, (c1, c2) ->
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
        List<Conversation> allConversations = new ArrayList<>();
        allConversations.addAll(conversationsList);
        allConversations.addAll(archivedConversationsList);

        Log.d(TAG, "Loading user details for " + allConversations.size() + " total conversations");

        if (allConversations.isEmpty()) {
            showEmptyState();
            return;
        }

        final int totalConversations = allConversations.size();
        final int[] loadedCount = {0};

        for (Conversation conversation : allConversations) {
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

                        // Add typing status listener
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
                                archivedConversationsAdapter.notifyDataSetChanged();
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
                            updateEmptyStates();
                            conversationsAdapter.notifyDataSetChanged();
                            archivedConversationsAdapter.notifyDataSetChanged();
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
                            updateEmptyStates();
                            conversationsAdapter.notifyDataSetChanged();
                            archivedConversationsAdapter.notifyDataSetChanged();
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
                    if (archivedConversationsAdapter != null) {
                        archivedConversationsAdapter.notifyDataSetChanged();
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
            updateEmptyStates();
        });
    }

    private void hideEmptyState() {
        Log.d(TAG, "Hiding empty state");
        runOnUiThread(() -> {
            updateEmptyStates();
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
        archivedConversationsAdapter.notifyDataSetChanged();

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