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

public class CompanyMessagesActivity extends AppCompatActivity implements ConversationsAdapter.OnConversationClickListener {

    private RecyclerView rvConversations;
    private LinearLayout tvEmptyState;
    private ConversationsAdapter conversationsAdapter;
    private List<Conversation> conversationsList = new ArrayList<>();

    private String currentUserId;
    private DatabaseReference chatMetadataRef, usersRef, chatsRef, notificationsRef;
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
        setContentView(R.layout.activity_company_messages);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();


        initializeViews();
        FloatingActionButton fabNewMessage = findViewById(R.id.fab_new_message);
        fabNewMessage.setVisibility(View.VISIBLE);
        fabNewMessage.setOnClickListener(v -> {
            Intent intent = new Intent(CompanyMessagesActivity.this, AllStudents.class);
            intent.putExtra("COMPANY_ID", currentUserId); // Pass current company ID to AllStudents
            startActivity(intent);
        });

        setupRecyclerView();
        setupSwipeGestures();
        setupFirebaseReferences();
        loadConversations();
    }

    private void initializeViews() {
        rvConversations = findViewById(R.id.messages_recycler_view);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        etSearch = findViewById(R.id.message_search);
        ImageView menuIcon = findViewById(R.id.menu_icon);
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

        swipeRefreshLayout.setOnRefreshListener(this::loadConversations);
        menuIcon.setOnClickListener(v -> onBackPressed());

        etSearch.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterConversations(s.toString());
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
        });
    }
    private void setupSwipeGestures() {
        setupSwipeGestureForRecyclerView(rvConversations, conversationsList, conversationsAdapter, false);
        setupSwipeGestureForRecyclerView(rvArchivedConversations, archivedConversationsList, archivedConversationsAdapter, true);
    }
    private void setupSwipeGestureForRecyclerView(RecyclerView recyclerView, List<Conversation> list, ConversationsAdapter adapter, boolean isArchived) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Conversation conversation = list.get(position);
                showSwipeActionDialog(conversation, position, isArchived);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                View foregroundView = ((ConversationsAdapter.ConversationViewHolder) viewHolder).itemView.findViewById(R.id.foreground_layout);
                getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                View foregroundView = ((ConversationsAdapter.ConversationViewHolder) viewHolder).itemView.findViewById(R.id.foreground_layout);
                getDefaultUIUtil().clearView(foregroundView);
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void showSwipeActionDialog(Conversation conversation, int position, boolean isFromArchived) {
        String archiveAction = isFromArchived ? "Unarchive" : "Archive";

        new AlertDialog.Builder(this)
                .setTitle("Choose Action")
                .setItems(new CharSequence[]{archiveAction, "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        if (isFromArchived) {
                            unarchiveConversation(conversation, position);
                        } else {
                            archiveConversation(conversation, position);
                        }
                    } else {
                        deleteConversation(conversation, position, isFromArchived);
                    }
                })
                .setOnDismissListener(dialog -> {
                    if (isFromArchived) {
                        archivedConversationsAdapter.notifyItemChanged(position);
                    } else {
                        conversationsAdapter.notifyItemChanged(position);
                    }
                }).show();
    }
    private void archiveConversation(Conversation conversation, int position) {
        chatMetadataRef.child(conversation.getChatId()).child("archivedBy").child(currentUserId).setValue(true)
                .addOnSuccessListener(unused -> {
                    conversation.setArchived(true);
                    conversationsList.remove(position);
                    archivedConversationsList.add(0, conversation);
                    conversationsAdapter.notifyItemRemoved(position);
                    archivedConversationsAdapter.notifyItemInserted(0);
                    updateEmptyStates();
                });
    }

    private void unarchiveConversation(Conversation conversation, int position) {
        chatMetadataRef.child(conversation.getChatId()).child("archivedBy").child(currentUserId).removeValue()
                .addOnSuccessListener(unused -> {
                    conversation.setArchived(false);
                    archivedConversationsList.remove(position);
                    int insertPos = findInsertPosition(conversation);
                    conversationsList.add(insertPos, conversation);
                    archivedConversationsAdapter.notifyItemRemoved(position);
                    conversationsAdapter.notifyItemInserted(insertPos);
                    updateEmptyStates();
                });
    }

    private void deleteConversation(Conversation conversation, int position, boolean isFromArchived) {
        chatMetadataRef.child(conversation.getChatId()).child("deletedBy").child(currentUserId).setValue(true)
                .addOnSuccessListener(unused -> {
                    if (isFromArchived) {
                        archivedConversationsList.remove(position);
                        archivedConversationsAdapter.notifyItemRemoved(position);
                    } else {
                        conversationsList.remove(position);
                        conversationsAdapter.notifyItemRemoved(position);
                    }
                    updateEmptyStates();
                });
    }

    private int findInsertPosition(Conversation convo) {
        for (int i = 0; i < conversationsList.size(); i++) {
            if (convo.getLastMessageTime() > conversationsList.get(i).getLastMessageTime()) {
                return i;
            }
        }
        return conversationsList.size();
    }


    private void showActiveTab() {
        isShowingActiveTab = true;
        rvConversations.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(conversationsList.isEmpty() ? View.VISIBLE : View.GONE);
        rvArchivedConversations.setVisibility(View.GONE);
        tvEmptyStateArchived.setVisibility(View.GONE); // Or handle with archived list logic
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
        tvEmptyStateArchived.setVisibility(View.VISIBLE); // Or handle with archived list logic
        tabArchiveText.setTextColor(getResources().getColor(R.color.blue));
        tabArchiveIndicator.setBackgroundColor(getResources().getColor(R.color.blue));
        tabActiveText.setTextColor(Color.parseColor("#808080"));
        tabActiveIndicator.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }


    private void setupRecyclerView() {
        conversationsAdapter = new ConversationsAdapter(conversationsList, this);
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(conversationsAdapter);

        archivedConversationsAdapter = new ConversationsAdapter(archivedConversationsList, this);
        rvArchivedConversations.setLayoutManager(new LinearLayoutManager(this));
        rvArchivedConversations.setAdapter(archivedConversationsAdapter);
    }


    private void setupFirebaseReferences() {
        chatMetadataRef = FirebaseDatabase.getInstance().getReference("chat_metadata");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
    }

    private void loadConversations() {
        chatMetadataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                conversationsList.clear();
                Set<String> processedChatIds = new HashSet<>();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    Boolean deleted = chatSnapshot.child("deletedBy").child(currentUserId).getValue(Boolean.class);
                    if (Boolean.TRUE.equals(deleted)) {
                        continue; // Skip this conversation
                    }


                    DataSnapshot participantsSnapshot = chatSnapshot.child("participants");

                    if (participantsSnapshot.hasChild(currentUserId)) {
                        String otherUserId = null;
                        for (DataSnapshot participant : participantsSnapshot.getChildren()) {
                            String participantId = participant.getKey();
                            if (!participantId.equals(currentUserId)) {
                                otherUserId = participantId;
                                break;
                            }
                        }

                        if (otherUserId != null) {
                            Conversation conversation = new Conversation();
                            Boolean archived = chatSnapshot.child("archivedBy").child(currentUserId).getValue(Boolean.class);
                            conversation.setArchived(Boolean.TRUE.equals(archived));

                            if (conversation.isArchived()) {
                                archivedConversationsList.add(conversation);
                            } else {
                                conversationsList.add(conversation);
                            }

                            conversation.setChatId(chatId);
                            conversation.setOtherUserId(otherUserId);
                            conversation.setLastMessage(chatSnapshot.child("lastMessage").getValue(String.class));
                            conversation.setLastMessageTime(chatSnapshot.child("lastMessageTime").getValue(Long.class));
                            conversation.setLastSenderId(chatSnapshot.child("lastSenderId").getValue(String.class));
                            calculateUnreadCount(conversation, otherUserId);
                            processedChatIds.add(chatId);
                        }

                    }

                }

                loadDirectChats(processedChatIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadDirectChats(new HashSet<>());
            }
        });
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

    private void loadDirectChats(Set<String> alreadyProcessed) {
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    Boolean deleted = chatSnapshot.child("deletedBy").child(currentUserId).getValue(Boolean.class);
                    if (Boolean.TRUE.equals(deleted)) {
                        continue; // Skip this conversation
                    }

                    if (alreadyProcessed.contains(chatId)) continue;
                    if (chatId != null && chatId.contains(currentUserId)) {
                        String otherUserId = getOtherUserIdFromChatId(chatId);
                        if (otherUserId == null) continue;

                        DataSnapshot messagesSnapshot = chatSnapshot.child("messages");
                        if (!messagesSnapshot.exists()) continue;

                        Conversation conversation = new Conversation();
                        conversation.setChatId(chatId);
                        conversation.setOtherUserId(otherUserId);
                        conversation.setLastMessage("No messages yet");
                        conversation.setLastMessageTime(0);

                        for (DataSnapshot message : messagesSnapshot.getChildren()) {
                            conversation.setLastMessage(message.child("text").getValue(String.class));
                            conversation.setLastMessageTime(message.child("timestamp").getValue(Long.class));
                            conversation.setLastSenderId(message.child("senderId").getValue(String.class));
                        }

                        calculateUnreadCount(conversation, otherUserId);
                        conversationsList.add(conversation);
                    }
                }

                Collections.sort(conversationsList, (c1, c2) -> Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));
                loadUserDetails();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadUserDetails();
            }
        });
    }

    private String getOtherUserIdFromChatId(String chatId) {
        String[] ids = chatId.split("_");
        return ids[0].equals(currentUserId) ? ids[1] : ids[0];
    }

    private void calculateUnreadCount(Conversation conversation, String otherUserId) {
        notificationsRef.child(currentUserId).orderByChild("chatId").equalTo(conversation.getChatId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int unread = 0;
                        for (DataSnapshot notif : snapshot.getChildren()) {
                            Boolean read = notif.child("read").getValue(Boolean.class);
                            String senderId = notif.child("senderId").getValue(String.class);
                            if (!Boolean.TRUE.equals(read) && otherUserId.equals(senderId)) {
                                unread++;
                            }
                        }
                        conversation.setUnreadCount(unread);
                        conversationsAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadUserDetails() {
        List<Conversation> all = new ArrayList<>();
        all.addAll(conversationsList);
        all.addAll(archivedConversationsList);
        for (Conversation conversation : all) {
            usersRef.child(conversation.getOtherUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    conversation.setOtherUserName(snapshot.child("name").getValue(String.class));
                    conversation.setOtherUserRole(snapshot.child("role").getValue(String.class));
                    conversation.setOtherUserLogoUrl(snapshot.child("logoUrl").getValue(String.class));
                    conversationsAdapter.notifyDataSetChanged();

                    // 👇 Add typing listener here
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
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

        }

        if (conversationsList.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
        swipeRefreshLayout.setRefreshing(false);
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

    private void showEmptyState() {
        rvConversations.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        rvConversations.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        // 🔥 Mark notifications as read
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
                        Log.e("CompanyMessagesActivity", "Failed to mark notifications as read: " + error.getMessage());
                    }
                });
        conversation.setUnreadCount(0);
        conversationsAdapter.notifyDataSetChanged();

        // Navigate to ChatActivity
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("CHAT_WITH_ID", conversation.getOtherUserId());
        intent.putExtra("CHAT_WITH_NAME", conversation.getOtherUserName());

        if (conversation.getProjectId() != null)
            intent.putExtra("PROJECT_ID", conversation.getProjectId());

        if (conversation.getApplicationId() != null)
            intent.putExtra("APPLICATION_ID", conversation.getApplicationId());

        startActivity(intent);
    }



    @Override
    protected void onResume() {
        super.onResume();
        loadConversations();
    }
}
