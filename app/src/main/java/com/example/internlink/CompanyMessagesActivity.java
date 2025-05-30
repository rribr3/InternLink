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

public class CompanyMessagesActivity extends AppCompatActivity implements ConversationsAdapter.OnConversationClickListener {

    private RecyclerView rvConversations;
    private LinearLayout tvEmptyState;
    private ConversationsAdapter conversationsAdapter;
    private List<Conversation> conversationsList = new ArrayList<>();

    private String currentUserId;
    private DatabaseReference chatMetadataRef, usersRef, chatsRef, notificationsRef;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText etSearch;

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
        setupFirebaseReferences();
        loadConversations();
    }

    private void initializeViews() {
        rvConversations = findViewById(R.id.messages_recycler_view);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        etSearch = findViewById(R.id.message_search);
        ImageView menuIcon = findViewById(R.id.menu_icon);

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
        chatMetadataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                conversationsList.clear();
                Set<String> processedChatIds = new HashSet<>();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
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
                            conversation.setChatId(chatId);
                            conversation.setOtherUserId(otherUserId);
                            conversation.setLastMessage(chatSnapshot.child("lastMessage").getValue(String.class));
                            conversation.setLastMessageTime(chatSnapshot.child("lastMessageTime").getValue(Long.class));
                            conversation.setLastSenderId(chatSnapshot.child("lastSenderId").getValue(String.class));
                            calculateUnreadCount(conversation, otherUserId);
                            conversationsList.add(conversation);
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

    private void loadDirectChats(Set<String> alreadyProcessed) {
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
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
        for (Conversation conversation : conversationsList) {
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
